FROM jenkins/jenkins:lts

USER root

# Ensure the directories exist with correct permissions
RUN mkdir -p /var/jenkins_home/.kube && \
    mkdir -p /var/jenkins_home/.oci && \
    chown -R jenkins:jenkins /var/jenkins_home/.kube && \
    chown -R jenkins:jenkins /var/jenkins_home/.oci && \
    chmod 700 /var/jenkins_home/.oci

# Copy configuration files
COPY --chown=jenkins:jenkins ./conf/k8s_config /var/jenkins_home/.kube/config
COPY --chown=jenkins:jenkins ./conf/oci_config /var/jenkins_home/.oci/config
COPY --chown=jenkins:jenkins ./conf/oci_api_key.pem /var/jenkins_home/.oci/oci_api_key.pem

# Set correct permissions for sensitive files
RUN chmod 600 /var/jenkins_home/.oci/config && \
    chmod 600 /var/jenkins_home/.oci/oci_api_key.pem

# Install kubectl
RUN apt-get update && \
    apt-get install -y curl wget && \
    curl -L -s "https://dl.k8s.io/release/stable.txt" |  xargs curl -LO  "https://dl.k8s.io/release/$(cat)/bin/linux/amd64/kubectl" && \
    curl -L -s "https://dl.k8s.io/release/stable.txt" | xargs curl -LO "https://dl.k8s.io/release/$(cat)/bin/linux/amd64/kubectl.sha256" && \
    echo "$(cat kubectl.sha256)  kubectl" | sha256sum --check && \
    install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Install OCI CLI in a virtual environment
RUN apt-get install -y python3-venv
RUN python3 -m venv /opt/oci-cli
RUN /opt/oci-cli/bin/pip install --upgrade pip
RUN /opt/oci-cli/bin/pip install oci-cli

# Add OCI CLI to PATH
ENV PATH="/opt/oci-cli/bin:${PATH}"

# Switch back to the jenkins user
USER jenkins

# Disable the Jenkins setup wizard
ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false"

COPY --chown=jenkins:jenkins init.groovy.d/ /var/jenkins_home/init.groovy.d/