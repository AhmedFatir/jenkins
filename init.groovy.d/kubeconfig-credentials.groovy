import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl

def instance = Jenkins.getInstance()
def store = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def kubeconfigFile = new File('/var/jenkins_home/.kube/config')
def kubeconfigContent = kubeconfigFile.text

def kubeconfigCreds = new StringCredentialsImpl(
    CredentialsScope.GLOBAL,
    "kubeconfig",
    "Kubernetes Config",
    new Secret(kubeconfigContent)
)

store.addCredentials(Domain.global(), kubeconfigCreds)
instance.save()
println "Kubernetes Config Credentials Added"
