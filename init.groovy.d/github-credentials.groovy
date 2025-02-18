import jenkins.model.*
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.impl.*

def instance = Jenkins.getInstance()
def store = instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()

def githubTokenValue = System.getenv('GITHUB_TOKEN')

if (githubTokenValue) {
    def githubToken = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        "github-token",
        "GitHub API Token",
        new Secret(githubTokenValue)
    )

    store.addCredentials(Domain.global(), githubToken)
    instance.save()
    println "GitHub Token Credentials Added"
} else {
    println "GitHub Token not found in environment variables"
}