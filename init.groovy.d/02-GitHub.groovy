import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get the GitHub token from the environment
String token = System.getenv("GITHUB_TOKEN")

// Create new GitHub token credentials
def credentials = new StringCredentialsImpl(
    CredentialsScope.GLOBAL,         // Available throughout Jenkins
    "github-token",                  // Credential ID referenced in jobs
    "GitHub Access Token",           // Human-readable description
    Secret.fromString(token)         // Securely store the token value
)
    
// Add the credentials to the Jenkins store
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
store.addCredentials(Domain.global(), credentials)
logger.info("GitHub token credentials added with ID: github-token")
