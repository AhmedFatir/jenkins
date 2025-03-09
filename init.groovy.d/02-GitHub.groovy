import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get Jenkins instance and credentials store
def jenkins = Jenkins.getInstance()
def domain = Domain.global()
def store = jenkins.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getStore()

// Check for GitHub token in environment variables
String token = System.getenv("GITHUB_TOKEN")

if (token) {
    // Check if credentials already exist to avoid duplicates
    def existingCreds = store.getCredentials(domain).find { it.id == 'github-token' }
    
    if (existingCreds) {
        logger.info("GitHub token credentials already exist with ID: github-token")
    }
    else {
        // Create new GitHub token credentials
        def credentials = new StringCredentialsImpl(
            CredentialsScope.GLOBAL,         // Available throughout Jenkins
            "github-token",                  // Credential ID referenced in jobs
            "GitHub Access Token",           // Human-readable description
            Secret.fromString(token)         // Securely store the token value
        )
        
        // Add the credentials to the Jenkins store
        store.addCredentials(domain, credentials)
        logger.info("GitHub token credentials added with ID: github-token")
    }
}
else {
    // Log warning if no token could be found
    logger.warning("GitHub token not found in environment variables")
}
