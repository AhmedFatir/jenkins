import jenkins.model.*
import hudson.PluginManager
import hudson.model.*
import java.util.logging.Logger

// Initialize logger for debugging and informational messages
def logger = Logger.getLogger("")

// List of required plugins for the CI/CD pipeline
def plugins = [
    'git',                              // Git integration for Jenkins
    'workflow-aggregator',              // Jenkins Pipeline support
    'github',                           // GitHub integration
    'kubernetes',                       // Kubernetes plugin for Jenkins
    'kubernetes-cli',                   // Provides kubectl CLI in Jenkins
    'plain-credentials',                // Allows storing plain text credentials
    'github-branch-source',             // GitHub Branch Source plugin
    'pipeline-github',                  // Pipeline integration with GitHub
    'oracle-cloud-infrastructure-devops', // OCI DevOps integration
    'bouncycastle-api',                 // Security provider for Jenkins
    'ssh-credentials',                  // SSH credentials management
    'credentials'                       // Core credentials plugin
]

// Get PluginManager and UpdateCenter instances
def pm = Jenkins.instance.pluginManager
def uc = Jenkins.instance.updateCenter

// Refresh update center data to get latest plugin information
uc.updateAllSites()

logger.info("Installing Jenkins Plugins...")
def restartRequired = false

// Loop through each plugin and install if not already present
plugins.each { plugin ->
    if (!pm.getPlugin(plugin)) {
        def pluginInstance = uc.getPlugin(plugin) // Look up the plugin in the update center
        if (pluginInstance) {
            def installFuture = pluginInstance.deploy() // Deploy and install the plugin
            installFuture.get() // Wait for the plugin to be installed
            logger.info("Installed: $plugin")
            restartRequired = true // Set flag to restart Jenkins
        }
        else {
            logger.warning("Plugin not found in update center: $plugin")
        }
    }
    else {
        logger.info("Already installed: $plugin")
    }
}

// If plugins were installed, save Jenkins state and restart to apply changes
if (restartRequired && !Jenkins.instance.isQuietingDown()) {
    logger.info("Saving Jenkins state before restart...")
    Jenkins.instance.save()
    logger.info("Restarting Jenkins to complete plugin installation...")
    Jenkins.instance.safeRestart()
    // Terminate the JVM to prevent further script execution
    System.exit(0)
}
