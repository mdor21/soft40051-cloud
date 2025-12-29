# SOFT40051 Cloud Computing Microservices Project

## 1. Project Overview

This repository contains a multi-container microservices simulation designed for the SOFT40051 Cloud Computing module. The system simulates a secure, distributed file storage and processing platform, demonstrating key cloud architecture concepts such as loose coupling, service discovery, elastic scaling, and concurrency control.

---

## 2. Architecture

The system is composed of several Java-based microservices that communicate over a shared Docker bridge network.

![Architecture Diagram](https://i.imgur.com/your-diagram-url.png) <!-- Placeholder for a future diagram -->

*   **`cloud-gui` (User Portal)**: A JavaFX application that serves as the user's entry point. It handles user authentication and initiates file upload/download operations. It also includes a local SQLite cache for offline resilience.
*   **`load-balancer`**: The single point of entry for file operations. It receives requests from the GUI, applies a scheduling algorithm (e.g., Shortest-Job-Next), and forwards the request to the Aggregator. It also monitors the request queue and triggers elastic scaling.
*   **`aggregator`**: The core file processing service. It encrypts files (AES-256), splits them into chunks, calculates CRC32 checksums for integrity, and distributes them to the file servers.
*   **`host-manager`**: The orchestrator. It listens for scaling commands from the Load Balancer via an MQTT broker and executes `docker run` or `docker stop` commands to dynamically add or remove file server containers.
*   **`file-servers`**: A set of simple SSH servers that provide the distributed storage backend.
*   **`lamp-server` (MySQL)**: The central database for all metadata, including user profiles, file locations, and chunk checksums.
*   **`mqtt-broker`**: A lightweight message broker that facilitates asynchronous communication between the Load Balancer and the Host Manager for elastic scaling.

---

## 3. Prerequisites

Before running the project, you must have the following software installed on your host machine:

*   **Docker Desktop** (or Docker Engine on Linux)
*   **Docker Compose** (usually included with Docker Desktop)
*   **Java Development Kit (JDK) 21** or higher
*   **Apache Maven**

---

## 4. How to Run the System

1.  **Clone the Repository**
    ```bash
    git clone <your-repository-url>
    cd <your-repository-name>
    ```

2.  **Build and Start All Services**
    Use the provided helper script to build and start the entire microservices stack in the background.
    ```bash
    # Make the script executable
    chmod +x scripts/start-dev.sh

    # Run the script
    ./scripts/start-dev.sh
    ```
    Alternatively, you can run the Docker Compose command directly:
    ```bash
    sudo docker compose up --build -d
    ```

3.  **Access the GUI**
    *   Once all containers are running, you can connect to the main application's desktop environment using any RDP (Remote Desktop Protocol) client.
    *   **Address**: `localhost:3390`

4.  **Shut Down the System**
    To stop all running containers, run the following command from the project's root directory:
    ```bash
    sudo docker compose down
    ```

---

## 5. Key Service Endpoints

| Service | Port (Host) | Internal Port | Description |
| :--- | :--- | :--- | :--- |
| **GUI (RDP)** | `3390` | `3389` | Remote Desktop for the main JavaFX application. |
| **GUI (SSH)** | `2022` | `22` | SSH access to the main GUI container. |
| **MySQL Database** | `3306` | `3306` | Direct access to the `lamp-server` database. |
| **Web Server (LAMP)**| `8080` | `80` | Web server running on the `lamp-server`. |
| **MQTT Broker** | `1883` | `1883` | MQTT message broker. |
| **File Server 1** | `4848` | `22` | SSH access to the first file storage container. |
| **Jenkins** | `8081` | `8080` | Jenkins CI/CD server web interface. |
| **Gitea** | `3000` | `3000` | Gitea Git server web interface. |
