import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get Jenkins instance and credentials store
def instance = Jenkins.getInstance()
def store = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

// Read the Kubernetes configuration file
def kubeconfigFile = new File('/var/jenkins_home/.kube/config')
def kubeconfigContent = kubeconfigFile.text

// Create credentials object to store Kubernetes config
def kubeconfigCreds = new StringCredentialsImpl(
    CredentialsScope.GLOBAL,           // Available throughout Jenkins
    "kubeconfig",                      // Credential ID referenced in jobs
    "Kubernetes Config",               // Human-readable description
    Secret.fromString(kubeconfigContent) // Securely store the kubeconfig content
)

// Add the credentials to the Jenkins store
store.addCredentials(Domain.global(), kubeconfigCreds)

// Save Jenkins configuration
instance.save()
logger.info("Kubernetes Config Credentials Added")
