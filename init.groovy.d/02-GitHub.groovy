import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import hudson.util.Secret
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get the GitHub token from the environment
String token = System.getenv("GITHUB_TOKEN")

if (!token || token.trim().isEmpty()) {
    logger.severe("GITHUB_TOKEN environment variable is not set or empty!")
    return
}

logger.info("GitHub token found, proceeding with credential creation...")

// Check if credentials already exist
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
def existingCreds = store.getCredentials(Domain.global()).find { it.id == "github-token" }

if (existingCreds) {
    logger.info("GitHub token credentials already exist with ID: github-token")
    return
}

// Create new GitHub token credentials as Username with password
// GitHub expects either username/token or token/x-oauth-basic
def credentials = new UsernamePasswordCredentialsImpl(
    CredentialsScope.GLOBAL,         // Available throughout Jenkins
    "github-token",                  // Credential ID referenced in jobs
    "GitHub Access Token",           // Human-readable description
    "token",                         // Username (can be anything for tokens)
    token                            // Password (the actual GitHub token)
)
    
// Add the credentials to the Jenkins store
store.addCredentials(Domain.global(), credentials)

// Save the configuration
Jenkins.instance.save()

logger.info("GitHub token credentials added with ID: github-token")

// Verify the credentials were actually added
def verifyCredentials = store.getCredentials(Domain.global()).find { it.id == "github-token" }
if (verifyCredentials) {
    logger.info("Verification successful: GitHub token credentials are available")
} else {
    logger.severe("Verification failed: GitHub token credentials were not properly saved")
}
