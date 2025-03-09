import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import java.util.logging.Logger
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig
import hudson.plugins.git.BranchSpec
import hudson.triggers.SCMTrigger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Create a new Pipeline job.
def jobName = "K8s-Deployment-Pipeline"
job = Jenkins.instance.createProject(WorkflowJob.class, jobName)
job.setDefinition(new CpsFlowDefinition(createPipelineScript(), true)) // Set the pipeline script
job.addTrigger(new SCMTrigger("H/2 * * * *")) // Add SCM polling trigger to check for changes every 2 minutes
job.save() // Save the job configuration
logger.info("Pipeline Job Created Successfully with name: ${jobName}")

// Trigger the initial build of the pipeline job
job.scheduleBuild2(0)
logger.info("Initial build triggered for the newly created job.")

// This function defines the actual Jenkins pipeline script
def createPipelineScript() {
    return '''
    pipeline {
        agent any
        environment {
            // Set the location of the Kubernetes config file
            KUBECONFIG = '/var/jenkins_home/.kube/config'
        }
        stages {
            stage('Clean Workspace') {
                steps {
                    // Clean the workspace to avoid conflicts with previous builds
                    deleteDir()
                }
            }
            stage('Checkout') {
                steps {
                    // Clone the Git repository with Kubernetes manifests
                    git url: 'https://github.com/AhmedFatir/k8s-inception.git', 
                        branch: 'main', 
                        credentialsId: 'github-token'
                }
            }
            stage('Apply K8s Manifests') {
                steps {
                    // Apply all Kubernetes manifests in the k8s directory
                    sh "chmod +x k8s/deploy.sh && ./k8s/deploy.sh"
                }
            }
            stage('Verify Deployment') {
                steps {
                    // List all non-system pods to verify deployments
                    sh 'kubectl get pods -A | grep -v kube-system'
                    
                    // List all non-system services to verify network exposure
                    sh 'kubectl get svc -A | grep -v kube-system'
                    
                    // Wait for all deployments to complete successfully
                    sh 'kubectl rollout status deployment/nginx'
                    sh 'kubectl rollout status deployment/wordpress'
                    sh 'kubectl rollout status statefulset/mariadb'
                }
            }
            stage('Smoke Tests') {
                steps {
                    script {
                        // Wait for and retrieve the external IP address of the nginx service
                        def externalIp = ""
                        timeout(time: 5, unit: 'MINUTES') {
                            while (externalIp == "") {
                                echo "Waiting for external IP..."
                                externalIp = sh(
                                    script: 'kubectl get svc nginx --template="{{range .status.loadBalancer.ingress}}{{.ip}}{{end}}"', 
                                    returnStdout: true
                                ).trim()
                                
                                // If no IP is available yet, wait 5 seconds before checking again
                                if (externalIp == "") {
                                    sleep 5
                                }
                            }
                        }
                        echo "External IP: ${externalIp}"
                        
                        // Perform a basic HTTP request to verify the service is responding
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
            // Actions to take after the pipeline completes
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
