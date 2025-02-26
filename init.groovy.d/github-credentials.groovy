import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import java.nio.file.Paths
import java.nio.file.Files
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get Jenkins instance and credentials store
def jenkins = Jenkins.getInstance()
def domain = Domain.global()
def store = jenkins.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getStore()

// First check for GitHub token in environment variables (Docker containers often pass credentials this way)
String token = System.getenv("GITHUB_TOKEN")
logger.info("Checking for GitHub token in environment variables")

// If token is not found in environment variables, try reading from files
if (!token) {
    logger.info("Token not found in environment variables, checking .env files")
    
    // Try reading from primary .env file inside container
    def envFile = new File('/var/jenkins_home/.env')
    if (envFile.exists()) {
        // Parse each line looking for GITHUB_TOKEN
        envFile.eachLine { line ->
            if (line.startsWith('GITHUB_TOKEN=')) {
                token = line.substring('GITHUB_TOKEN='.length())
                logger.info("Found GitHub token in /var/jenkins_home/.env")
            }
        }
    } else {
        logger.info("/var/jenkins_home/.env file not found")
    }

    // If still no token, check alternative location
    if (!token) {
        def altEnvFile = new File('/home/ubuntu/jenkins/.env')
        if (altEnvFile.exists()) {
            // Parse each line looking for GITHUB_TOKEN
            altEnvFile.eachLine { line ->
                if (line.startsWith('GITHUB_TOKEN=')) {
                    token = line.substring('GITHUB_TOKEN='.length())
                    logger.info("Found GitHub token in /home/ubuntu/jenkins/.env")
                }
            }
        } else {
            logger.info("/home/ubuntu/jenkins/.env file not found")
        }
    }
}

if (token) {
    // Check if credentials already exist to avoid duplicates
    def existingCreds = store.getCredentials(domain).find { it.id == 'github-token' }
    
    if (existingCreds) {
        logger.info("GitHub token credentials already exist with ID: github-token")
    } else {
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
} else {
    // Log warning if no token could be found
    logger.warning("GitHub token not found in environment variables or .env files")
}
