import jenkins.model.*
import hudson.PluginManager
import hudson.model.*
import jenkins.plugins.git.GitSCMSource

def plugins = [
    'git',
    'workflow-aggregator',
    'github',
    'kubernetes',
    'kubernetes-cli',
    'plain-credentials'
]

def jenkins = Jenkins.instance
def pm = jenkins.pluginManager
def uc = jenkins.updateCenter

println "Installing Jenkins Plugins..."
plugins.each { plugin ->
    if (!pm.getPlugin(plugin)) {
        def pluginInstance = uc.getPlugin(plugin)
        if (pluginInstance) {
            pluginInstance.deploy()
            println "Installed: $plugin"
        } else {
            println "Plugin not found: $plugin"
        }
    } else {
        println "Already installed: $plugin"
    }
}
