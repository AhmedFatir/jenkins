import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*
import hudson.model.FreeStyleProject
import java.util.logging.Logger
import hudson.plugins.git.GitSCM
import hudson.plugins.git.UserRemoteConfig

def logger = Logger.getLogger("")

def jobName = "K8s-Deployment-Pipeline"
def jenkins = Jenkins.instance
def job = jenkins.getItem(jobName)

if (job == null) {
    println "Creating Jenkins Pipeline Job: ${jobName}"
    job = jenkins.createProject(WorkflowJob.class, jobName)
    // Set SCM so pollSCM trigger works
    def userRemote = new UserRemoteConfig("https://github.com/AhmedFatir/k8s-inception.git", "origin", "", "github-token")
    job.scm = new GitSCM([userRemote])
} else {
    println "Updating existing Jenkins Pipeline Job: ${jobName}"
}

def script = '''\
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
                    // Use the job's SCM configuration for checkout
                    checkout scm
                }
            }
            stage('Apply K8s Manifests') {
                steps {
                    sh """
                      cd k8s-inception
                      kubectl apply -Rf k8s/
                    """
                }
            }
            stage('Verify Deployment') {
                steps {
                    sh 'kubectl get pods -A && kubectl get svc -A'
                }
            }
        }
    }
'''.stripIndent()

job.definition = new CpsFlowDefinition(script, true)

job.save()
println "Pipeline Job Created/Updated Successfully"

// Trigger an immediate build after job creation.
job.scheduleBuild2(0)

logger.info("Job configured with GitSCM and pollSCM trigger every 2 minutes; initial build scheduled")
