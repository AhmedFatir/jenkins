import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import java.nio.file.*
import java.util.logging.Logger
import hudson.util.Secret

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// Helper function to load OCI credentials class from the plugin
def loadOCICredentialsClass(logger) {
    // Get Jenkins plugin manager and OCI plugin
    def jenkins = Jenkins.instance
    def pluginManager = jenkins.pluginManager
    def ociPlugin = pluginManager.getPlugin("oracle-cloud-infrastructure-devops")
    
    if (ociPlugin) {
        // Get plugin version and load the credentials implementation class
        logger.info("OCI DevOps plugin version: ${ociPlugin.version}")
        try {
            def cloudCredentialsClass = ociPlugin.classLoader.loadClass("io.jenkins.plugins.oci.credentials.CloudCredentialsImpl")
            return cloudCredentialsClass
        } catch (ClassNotFoundException e) {
            logger.warning("Could not find CloudCredentialsImpl class")
            return null
        }
    }
    return null
}

try {
    // Load the OCI Credentials class from the plugin
    def ociCredentialsClass = loadOCICredentialsClass(logger)
    if (!ociCredentialsClass) {
        throw new ClassNotFoundException("OCI Credentials class not found")
    }

    // Define path to OCI config and key files
    def configFilePath = Paths.get("/var/jenkins_home/.oci/config")
    def keyFilePath = Paths.get("/var/jenkins_home/.oci/oci_api_key.pem")
    
    // Check if the required files exist
    if (!Files.exists(configFilePath)) {
        throw new FileNotFoundException("OCI config file not found: ${configFilePath}")
    }
    if (!Files.exists(keyFilePath)) {
        throw new FileNotFoundException("OCI key file not found: ${keyFilePath}")
    }

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
    def domain = Domain.global()
    def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
    store.addCredentials(domain, credentials)

    logger.info("OCI credentials added to Jenkins.")
} catch (FileNotFoundException e) {
    // Log error if config files are missing
    logger.warning(e.message)
} catch (ClassNotFoundException e) {
    // Log error if plugin classes can't be loaded
    logger.warning("OCI Credentials class not found. Ensure the 'oracle-cloud-infrastructure-devops' plugin is installed.")
} catch (NoSuchMethodException e) {
    // Log error if constructor can't be found
    logger.severe("Could not find the required constructor for CloudCredentialsImpl: ${e.message}")
} catch (Exception e) {
    // Log any other errors
    logger.severe("An error occurred while adding OCI credentials: ${e.message}")
    e.printStackTrace()
}
