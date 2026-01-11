# Cloud Simulation Platform

This repository contains the source code for a multi-service cloud simulation platform.

## Services Overview

The platform is orchestrated using Docker Compose and consists of the following services:

- **`ntu-vm-soft40051`**: The main JavaFX GUI for user interaction.
- **`load-balancer`**: Distributes incoming requests to the aggregator.
- **`aggregator`**: Handles file processing, encryption, and chunking.
- **`lamp-server`**: MySQL database for metadata storage.
- **`mqtt-broker`**: Message broker for inter-service communication.
- **`soft40051-files-container*`**: SFTP storage nodes.
- **`jenkins-soft40051`**: CI/CD server.
- **`gitea`**: Git version control server.

## Running the Platform

### Prerequisites

- Docker and Docker Compose
- Java 21+
- Maven 3.9+

### Quick Start

1.  **Environment Configuration**: If a `.env` file does not exist, create one. It should contain the necessary environment variables for the services (e.g., database passwords, ports).
2.  **Start Services**: Run the following command from the repository root to build and start all containerized services.

    ```bash
    sudo docker compose up -d --build
    ```

### Host Manager (`hostmanager`)

The `hostmanager` service is a special component that runs on the **host machine**, not in a Docker container. Its purpose is to interact with the Docker daemon to dynamically scale the number of storage nodes.

#### Building the Host Manager

Navigate to the repository root and run the following Maven command:

```bash
mvn -f hostmanager/pom.xml clean package
```

This will compile the service and create an executable JAR in the `hostmanager/target/` directory.

#### Running the Host Manager

Once built, you can run the `hostmanager` with the following command:

```bash
java -jar hostmanager/target/hostmanager-1.0-SNAPSHOT.jar
```

The Host Manager will connect to the MQTT broker and listen for scaling requests published by the `load-balancer`.
