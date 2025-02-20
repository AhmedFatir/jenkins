FROM jenkins/jenkins:lts



ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"
# COPY --chown=jenkins:jenkins init.groovy.d/ /var/jenkins_home/init.groovy.d/