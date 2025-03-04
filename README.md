# Jenkins CI/CD Environment
- This repository is part of the Project [**OCI-IAC**](https://github.com/AhmedFatir/OCI-IAC) to run a Jenkins server into an OCI Instance
- This repository contains the configuration and setup files for a Jenkins CI/CD environment.
- The setup includes Docker, Docker Compose, and various Groovy scripts to automate the initialization and configuration of Jenkins.


## Architecture Diagram

```
      Build                     Initialize            Pull Code      Deploy to OKE
  +--------------+  +--------------+  +----------+  +----------+  +-----------------+
  |              |  |              |  |          |  |          |  |                 |
  |  Dockerfile  +->| Jenkins with |  | Pipeline |  |  GitHub  |  | OCI Kubernetes  |
  |  + Groovy    |  | Installed    +->|  Job     +->|  Repo    +->| Engine (OKE)    |
  |  Scripts     |  | Plugins      |  |          |  |          |  | Cluster         |
  |              |  |              |  |          |  |          |  |                 |
  +--------------+  +--------------+  +----------+  +----------+  +-----------------+
                        ^    ^             ^
                        |    |             |
                        |    |             |
  +--------------+      |    |       +-----+-------+
  |              |      |    |       |             |
  | Kubernetes   +------+    +-------+ OCI         |
  | Credentials  |                   | Credentials |
  |              |                   |             |
  +--------------+                   +-------------+
```

The repository contains components for a complete CI/CD pipeline:

1. **Jenkins Initialization**:
   - **Dockerfile**: Creates a Jenkins container with necessary tools (kubectl, OCI CLI)
   - **Groovy Scripts**: Automate the setup process:
     - `installPlugins.groovy`: Installs required Jenkins plugins
     - `github-credentials.groovy`: Sets up GitHub authentication
     - `kubeconfig-credentials.groovy`: Configures Kubernetes credentials
     - `setupOCICredentials.groovy`: Sets up OCI authentication
     - `create-pipeline.groovy`: Creates the deployment pipeline job

2. **Pipeline Workflow**:
   - The pipeline job pulls Kubernetes manifests from a GitHub repository
   - Authentication to GitHub using configured credentials
   - Authenticates to OKE cluster using Kubernetes configuration
   - Deploys the Kubernetes manifests to the OCI OKE cluster

3. **Configuration**:
   - Kubernetes config file for OKE cluster access
   - OCI config and API key for authentication with Oracle Cloud

This setup provides an automated way to initialize Jenkins with proper credentials and a deployment pipeline that connects to both GitHub and OCI Kubernetes Engine.

## Directory Structure

```
/workspaces/jenkins
├── conf/                             # Configuration files for Kubernetes and OCI
│   ├── k8s_config                    # Kubernetes config file
│   ├── oci_config                    # OCI config file
│   └── oci_api_key.pem               # OCI API key file
├── init.groovy.d/                    # Groovy scripts for Jenkins initialization
│   ├── create-pipeline.groovy        # Script to create a Jenkins pipeline job
│   ├── github-credentials.groovy     # Script to add GitHub credentials
│   ├── installPlugins.groovy         # Script to install required Jenkins plugins
│   ├── kubeconfig-credentials.groovy # Script to add Kubernetes config credentials
│   └── setupOCICredentials.groovy    # Script to add OCI credentials
├── .gitignore                        # Git ignore file
├── Dockerfile                        # Dockerfile to build the Jenkins image
├── docker-compose.yml                # Docker Compose file to run Jenkins
├── Makefile                          # Makefile with common Docker commands
└── README.md                         # This README file
```
## Prerequisites

- Docker
- Docker Compose

## Setup Instructions

### 1. Clone the Repository

```sh
git clone <repository-url>
cd jenkins
```

### 2. Configure Environment Variables

Create a `.env` file in the root directory with the following content:

```
GITHUB_TOKEN=<your-github-token>
```

### 3. Build and Run Jenkins

Use the Makefile to build and run the Jenkins environment:

```sh
make
```

This will clear the terminal and start the Jenkins server using Docker Compose.

### 4. Access Jenkins

Open your web browser and navigate to `http://localhost:8080`. You should see the Jenkins login page.

### 5. Initial Setup

Jenkins will automatically install the required plugins and configure credentials using the Groovy scripts in the `init.groovy.d` directory.

## Makefile Commands

- `make all`: Build and start the Jenkins server.
- `make down`: Stop and remove the Jenkins containers.
- `make stop`: Stop the Jenkins containers.
- `make start`: Start the Jenkins containers.
- `make clean`: Clean up Docker containers, images, volumes, and networks.
- `make prune`: Clean up Docker system.
- `make re`: Rebuild and start the Jenkins server.
- `make jk`: Access the Jenkins container shell.

## Groovy Scripts

### `create-pipeline.groovy`

Creates a Jenkins pipeline job named "K8s-Deployment-Pipeline" that deploys Kubernetes manifests from a GitHub repository.

### `github-credentials.groovy`

Adds GitHub token credentials to Jenkins, either from environment variables or `.env` files.

### `installPlugins.groovy`

Installs a list of required Jenkins plugins for the CI/CD pipeline.

### `kubeconfig-credentials.groovy`

Adds Kubernetes config credentials to Jenkins from the specified file.

### `setupOCICredentials.groovy`

Adds OCI credentials to Jenkins from the specified config and key files.

## Dockerfile

The Dockerfile sets up the Jenkins environment with the necessary tools and configurations, including:

- Installing `kubectl`
- Installing OCI CLI
- Copying Kubernetes and OCI configuration files
- Setting up Jenkins initialization scripts

## Docker Compose

The `docker-compose.yml` file defines the Jenkins service, including:

- Building the Jenkins image from the Dockerfile
- Mapping ports
- Mounting volumes
- Using environment variables from the `.env` file

## .gitignore

The `.gitignore` file specifies files and directories to be ignored by Git, including:

- `.env`
- `conf/`
- `*.log`