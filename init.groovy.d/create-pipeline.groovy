import jenkins.model.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.*

def jobName = "K8s-Deployment-Pipeline"
def jenkins = Jenkins.instance
def job = jenkins.getItem(jobName)

if (job == null) {
    println "Creating Jenkins Pipeline Job: ${jobName}"

    def pipeline = new WorkflowJob(jenkins, jobName)
    def script = '''\
        pipeline {
            agent any
            environment {
                KUBECONFIG = credentials('kubeconfig')
            }
            stages {
                stage('Prepare Kubeconfig') {
                    steps {
                        sh 'mkdir -p $HOME/.kube'
                        sh 'echo "$KUBECONFIG" > $HOME/.kube/config'
                    }
                }
                stage('Checkout') {
                    steps {
                        git branch: 'main', credentialsId: 'github-token', url: 'https://github.com/AhmedFatir/k8s-inception'
                    }
                }
                stage('Apply K8s Manifests') {
                    steps {
                        sh 'kubectl apply -Rf k8s/'
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

    def flowDefinition = new CpsFlowDefinition(script, true)
    pipeline.definition = flowDefinition
    jenkins.add(pipeline)
    jenkins.save()
    println "Pipeline Job Created Successfully"
} else {
    println "Pipeline Job Already Exists"
}
