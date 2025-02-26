import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl
import hudson.util.Secret
import java.nio.file.Paths
import java.nio.file.Files
import java.util.logging.Logger

def logger = Logger.getLogger("")
def jenkins = Jenkins.getInstance()
def domain = Domain.global()
def store = jenkins.getExtensionList("com.cloudbees.plugins.credentials.SystemCredentialsProvider")[0].getStore()

// First check for token in environment variables (passed from Docker's .env file)
String token = System.getenv("GITHUB_TOKEN")
logger.info("Checking for GitHub token in environment variables")

// If not found in environment, try reading from files
if (!token) {
    logger.info("Token not found in environment variables, checking .env files")
    
    // Read GitHub token from .env file inside container
    def envFile = new File('/var/jenkins_home/.env')
    if (envFile.exists()) {
        envFile.eachLine { line ->
            if (line.startsWith('GITHUB_TOKEN=')) {
                token = line.substring('GITHUB_TOKEN='.length())
                logger.info("Found GitHub token in /var/jenkins_home/.env")
            }
        }
    } else {
        logger.info("/var/jenkins_home/.env file not found")
    }

    // If token wasn't found, check alternative location
    if (!token) {
        def altEnvFile = new File('/home/ubuntu/jenkins/.env')
        if (altEnvFile.exists()) {
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
    // Check if credentials already exist
    def existingCreds = store.getCredentials(domain).find { it.id == 'github-token' }
    
    if (existingCreds) {
        logger.info("GitHub token credentials already exist with ID: github-token")
    } else {
        // Create GitHub token credentials
        def credentials = new StringCredentialsImpl(
            CredentialsScope.GLOBAL,
            "github-token",
            "GitHub Access Token",
            Secret.fromString(token)
        )
        
        store.addCredentials(domain, credentials)
        logger.info("GitHub token credentials added with ID: github-token")
    }
} else {
    logger.warning("GitHub token not found in environment variables or .env files")
}
