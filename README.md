# qubership-colly

This project uses Quarkus, the Supersonic Subatomic Java Framework and React.

## Goal

- The tool is designed to track the usage of clusters and environments within clusters.
- Support several clusters
- ability to group several namespaces into one environment
- show additional custom UI parameters for the environment (owner, description, status)
- (todo) ability to show information (name, version) about deployed helm packages
    - (optional) support argo packages
- (todo) collect resources and metrics from kubernetes and monitoring


## Run latest version in Docker
```shell script
docker run -d --rm --name colly-db -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:17
docker run -v ~/.kube:/kubeconfigs -e ENV_INSTANCES_REPO=https://github.com/ormig/cloud-passport-samples.git -i --rm -p 8080:8080 ghcr.io/netcracker/qubership-colly:latest
```

## Clusters configuration
There are two ways to specify clusters:
1. Specify folder with kubeconfig files in `/kubeconfigs` and run the application. The application will read all kubeconfig files and connect to clusters. Example:
   ```shell
   docker run -v ~/.kube:/kubeconfigs -i --rm -p 8080:8080 ghcr.io/netcracker/qubership-colly:latest
   ```
   The application will read all kubeconfig files in `~/.kube` folder and connect to clusters.
2. Specify `ENV_INSTANCES_REPO` environment variable with URL to git repository with Cloud Passports files. The application will clone the repository and read all cloud passports for each cluster. Example:
    ```shell
   docker run -e ENV_INSTANCES_REPO=https://github.com/ormig/cloud-passport-samples.git -i --rm -p 8080:8080 ghcr.io/netcracker/qubership-colly:latest
    ```
    The application will clone the repository `https://github.com/ormig/cloud-passport-samples.git` and read all cloud passports for each cluster. If authentication is required to clone repository you can specify it in URL:
    ```shell
      docker run -e ENV_INSTANCES_REPO=https://myusername:mypassword@github.com/ormig/cloud-passport-samples.git -i --rm -p 8080:8080 ghcr.io/netcracker/qubership-colly:latest
    ```
