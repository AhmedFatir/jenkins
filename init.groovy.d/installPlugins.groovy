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

// Get Jenkins instance and plugin manager
def jenkins = Jenkins.instance
def pm = jenkins.pluginManager
def uc = jenkins.updateCenter

// Refresh update center data to get latest plugin information
uc.updateAllSites()

println "Installing Jenkins Plugins..."
def restartRequired = false

// Loop through each plugin and install if not already present
plugins.each { plugin ->
    if (!pm.getPlugin(plugin)) {
        // Look up the plugin in the update center
        def pluginInstance = uc.getPlugin(plugin)
        if (pluginInstance) {
            // Deploy and install the plugin
            def installFuture = pluginInstance.deploy()
            installFuture.get() // Wait for the plugin to be installed
            println "Installed: $plugin"
            restartRequired = true
        } else {
            println "Plugin not found: $plugin"
        }
    } else {
        println "Already installed: $plugin"
    }
}

// If plugins were installed, save Jenkins state and restart to apply changes
if (restartRequired && !jenkins.isQuietingDown()) {
    println "Saving Jenkins state before restart..."
    jenkins.save()
    println "Restarting Jenkins to complete plugin installation..."
    jenkins.safeRestart()
}
