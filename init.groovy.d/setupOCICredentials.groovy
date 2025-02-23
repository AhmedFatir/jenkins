import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import java.nio.file.*
import java.util.logging.Logger
import hudson.util.Secret

def logger = Logger.getLogger("")

def loadOCICredentialsClass(logger) {
    def jenkins = Jenkins.instance
    def pluginManager = jenkins.pluginManager
    def ociPlugin = pluginManager.getPlugin("oracle-cloud-infrastructure-devops")
    
    if (ociPlugin) {
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
    def ociCredentialsClass = loadOCICredentialsClass(logger)
    if (!ociCredentialsClass) {
        throw new ClassNotFoundException("OCI Credentials class not found")
    }

    def configFilePath = Paths.get("/var/jenkins_home/.oci/config")
    def keyFilePath = Paths.get("/var/jenkins_home/.oci/oci_api_key.pem")
    
    if (!Files.exists(configFilePath)) {
        throw new FileNotFoundException("OCI config file not found: ${configFilePath}")
    }
    if (!Files.exists(keyFilePath)) {
        throw new FileNotFoundException("OCI key file not found: ${keyFilePath}")
    }

    def config = new Properties()
    config.load(new FileInputStream(configFilePath.toFile()))

    def fingerprint = config.getProperty("fingerprint")
    def apiKey = new String(Files.readAllBytes(keyFilePath))
    def tenantId = config.getProperty("tenancy")
    def userId = config.getProperty("user")
    def region = config.getProperty("region")
    def scope = CredentialsScope.GLOBAL

    logger.info("Found OCI config with tenancy: ${tenantId}")
    logger.info("Using key file: ${keyFilePath}")

    // Get the correct constructor
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

    // Create the credentials object
    def credentials = constructor.newInstance(
        scope,                    // scope
        "oci-credentials",          // id
        "OCI Credentials",          // description
        fingerprint,                // fingerprint
        Secret.fromString(apiKey),  // privateKey
        Secret.fromString(""),      // passphrase
        tenantId,                   // tenancyId
        userId,                     // userId
        region                      // region
    )

    def domain = Domain.global()
    def store = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
    store.addCredentials(domain, credentials)

    logger.info("OCI credentials added to Jenkins.")
} catch (FileNotFoundException e) {
    logger.warning(e.message)
} catch (ClassNotFoundException e) {
    logger.warning("OCI Credentials class not found. Ensure the 'oracle-cloud-infrastructure-devops' plugin is installed.")
} catch (NoSuchMethodException e) {
    logger.severe("Could not find the required constructor for CloudCredentialsImpl: ${e.message}")
} catch (Exception e) {
    logger.severe("An error occurred while adding OCI credentials: ${e.message}")
    e.printStackTrace()
}
