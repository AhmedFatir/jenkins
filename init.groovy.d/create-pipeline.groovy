import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import java.util.logging.Logger
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.BranchSpec
import hudson.triggers.SCMTrigger

def logger = Logger.getLogger("")

def jobName = "K8s-Deployment-Pipeline"
def jenkins = Jenkins.instance
def job = jenkins.getItem(jobName)

def repoUrl = "https://github.com/AhmedFatir/k8s-inception.git"
def branchName = "main"
def credentialsId = "github-token"

if (job == null) {
    logger.info("Creating Jenkins Pipeline Job: ${jobName}")
    job = jenkins.createProject(WorkflowJob.class, jobName)
    
    // Configure Git SCM
    def userRemote = new UserRemoteConfig(repoUrl, "origin", "", credentialsId)
    def branchSpec = new BranchSpec("*/${branchName}")
    def scm = new GitSCM([userRemote], [branchSpec], false, [], null, null, [])
    job.setDefinition(new CpsFlowDefinition(createPipelineScript(), true))
    
    // Add SCM polling trigger
    job.addTrigger(new SCMTrigger("H/2 * * * *"))
    
    job.save()
    logger.info("Pipeline Job Created Successfully")
    
    // Trigger an initial build
    job.scheduleBuild2(0)
    logger.info("Initial build triggered for the newly created job.")
} else {
    logger.info("Updating existing Jenkins Pipeline Job: ${jobName}")
    
    // Update existing job
    job.setDefinition(new CpsFlowDefinition(createPipelineScript(), true))
    
    // Make sure SCM polling is set
    if (job.getTriggers().get(SCMTrigger.class) == null) {
        job.addTrigger(new SCMTrigger("H/2 * * * *"))
    }
    
    job.save()
    logger.info("Pipeline Job Updated Successfully")
}

def createPipelineScript() {
    return '''
    pipeline {
        agent any
        triggers {
            pollSCM('H/2 * * * *')
        }
        environment {
            KUBECONFIG = '/var/jenkins_home/.kube/config'
        }
        stages {
            stage('Clean Workspace') {
                steps {
                    deleteDir()
                }
            }
            stage('Checkout') {
                steps {
                    git url: 'https://github.com/AhmedFatir/k8s-inception.git', 
                        branch: 'main', 
                        credentialsId: 'github-token'
                }
            }
            stage('Apply K8s Manifests') {
                steps {
                    sh "kubectl apply -Rf k8s/"
                }
            }
            stage('Verify Deployment') {
                steps {
                    sh 'kubectl get pods -A | grep -v kube-system'
                    sh 'kubectl get svc -A | grep -v kube-system'
                    sh 'kubectl rollout status deployment/nginx'
                    sh 'kubectl rollout status deployment/wordpress'
                    sh 'kubectl rollout status statefulset/mariadb'
                }
            }
            stage('Smoke Tests') {
                steps {
                    script {
                        def externalIp = ""
                        timeout(time: 5, unit: 'MINUTES') {
                            while (externalIp == "") {
                                echo "Waiting for external IP..."
                                externalIp = sh(
                                    script: 'kubectl get svc nginx --template="{{range .status.loadBalancer.ingress}}{{.ip}}{{end}}"', 
                                    returnStdout: true
                                ).trim()
                                
                                if (externalIp == "") {
                                    sleep 10
                                }
                            }
                        }
                        echo "External IP: ${externalIp}"
                        
                        def response = sh(
                            script: "curl -s -o /dev/null -w '%{http_code}' http://${externalIp}",
                            returnStdout: true
                        ).trim()
                        
                        echo "HTTP Status Code: ${response}"
                    }
                }
            }
        }
        post {
            success {
                echo "Pipeline succeeded! The K8s cluster has been successfully updated."
            }
            failure {
                echo "Pipeline failed. Please check the logs for details."
            }
        }
    }
    '''.stripIndent()
}

logger.info("Job configured with GitSCM and pollSCM trigger every 2 minutes")
