import jenkins.model.*
import hudson.PluginManager
import hudson.model.*
import java.util.logging.Logger

def logger = Logger.getLogger("")

def plugins = [
    'git',
    'workflow-aggregator',
    'github',
    'kubernetes',
    'kubernetes-cli',
    'plain-credentials',
    'github-branch-source',
    'pipeline-github',
    'oracle-cloud-infrastructure-devops',
    'bouncycastle-api',
    'ssh-credentials',
    'credentials'
]

def jenkins = Jenkins.instance
def pm = jenkins.pluginManager
def uc = jenkins.updateCenter

// Refresh update center data
uc.updateAllSites()

println "Installing Jenkins Plugins..."
def restartRequired = false
plugins.each { plugin ->
    if (!pm.getPlugin(plugin)) {
        def pluginInstance = uc.getPlugin(plugin)
        if (pluginInstance) {
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

if (restartRequired && !jenkins.isQuietingDown()) {
    println "Saving Jenkins state before restart..."
    jenkins.save()
    println "Restarting Jenkins to complete plugin installation..."
    jenkins.safeRestart()
}
