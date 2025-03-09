import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import java.nio.file.*
import java.util.logging.Logger
import hudson.util.Secret

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Get the OCI plugin and credentials class
def ociPlugin = Jenkins.instance.pluginManager.getPlugin("oracle-cloud-infrastructure-devops")
def ociCredentialsClass = ociPlugin.classLoader.loadClass("io.jenkins.plugins.oci.credentials.CloudCredentialsImpl")
if (!ociCredentialsClass) {
    logger.warning("OCI Credentials class not found. Ensure the 'oracle-cloud-infrastructure-devops' plugin is installed.")
    return
}

// Define path to OCI config and key files
def configFilePath = Paths.get("/var/jenkins_home/.oci/config")
def keyFilePath = Paths.get("/var/jenkins_home/.oci/oci_api_key.pem")

// Load OCI config properties
def config = new Properties()
config.load(new FileInputStream(configFilePath.toFile()))

// Extract required properties from the config file
def fingerprint = config.getProperty("fingerprint")
def apiKey = new String(Files.readAllBytes(keyFilePath))
def tenantId = config.getProperty("tenancy")
def userId = config.getProperty("user")
def region = config.getProperty("region")
def scope = CredentialsScope.GLOBAL

logger.info("Found OCI config with tenancy: ${tenantId}")
logger.info("Using key file: ${keyFilePath}")

// Get the correct constructor for the OCI credentials class
def constructor = ociCredentialsClass.getConstructor(
    com.cloudbees.plugins.credentials.CredentialsScope.class,
    String.class,
    String.class,
    String.class,
    hudson.util.Secret.class,
    hudson.util.Secret.class,
    String.class,
    String.class,
    String.class
)

// Create the OCI credentials object with values from the config
def credentials = constructor.newInstance(
    scope,                    // credentials scope
    "oci-credentials",        // credential ID
    "OCI Credentials",        // description
    fingerprint,              // API key fingerprint
    Secret.fromString(apiKey), // private key content
    Secret.fromString(""),    // passphrase (empty)
    tenantId,                 // OCI tenancy ID
    userId,                   // OCI user ID
    region                    // OCI region
)

// Add the credentials to the Jenkins store
def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
store.addCredentials(Domain.global(), credentials)
logger.info("OCI credentials added with ID: oci-credentials")
