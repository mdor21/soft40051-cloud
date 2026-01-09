# Setup Guide

This guide provides comprehensive instructions for setting up the SOFT40051 Cloud Storage System.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Setup](#initial-setup)
3. [Environment Configuration](#environment-configuration)
4. [Building the Project](#building-the-project)
5. [Starting Services](#starting-services)
6. [Verification](#verification)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### System Requirements

- **Operating System**: Linux (Ubuntu 20.04+), macOS (12+), or Windows 10/11 with WSL2
- **RAM**: Minimum 8GB, recommended 16GB
- **Disk Space**: Minimum 20GB free space
- **CPU**: 4+ cores recommended

### Required Software

#### 1. Java Development Kit (JDK)

```bash
# Check if Java is installed
java -version

# Should show Java 20 or higher
# If not installed:

# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-20-jdk

# macOS (using Homebrew)
brew install openjdk@20

# Windows
# Download from https://adoptium.net/
```

#### 2. Apache Maven

```bash
# Check if Maven is installed
mvn -version

# Should show Maven 3.8 or higher
# If not installed:

# Ubuntu/Debian
sudo apt install maven

# macOS
brew install maven

# Windows
# Download from https://maven.apache.org/download.cgi
```

#### 3. Docker Engine

```bash
# Check if Docker is installed
docker --version

# Should show Docker 20.10 or higher
# If not installed:

# Ubuntu
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# macOS
# Download Docker Desktop from https://www.docker.com/products/docker-desktop

# Windows
# Download Docker Desktop from https://www.docker.com/products/docker-desktop
```

#### 4. Docker Compose

```bash
# Check if Docker Compose is installed
docker compose version

# Should show v2.0 or higher
# Usually comes with Docker Desktop
# For Linux:
sudo apt install docker-compose-plugin
```

#### 5. Git

```bash
# Check if Git is installed
git --version

# If not installed:
# Ubuntu/Debian
sudo apt install git

# macOS
brew install git

# Windows
# Download from https://git-scm.com/download/win
```

## Initial Setup

### 1. Clone the Repository

```bash
# Clone the repository
git clone https://github.com/mdor21/soft40051-cloud.git
cd soft40051-cloud
```

### 2. Verify Project Structure

```bash
# List the project structure
ls -la

# You should see:
# - AggService/
# - cloud-gui/
# - cloudlb/
# - hostmanager/
# - config/
# - db/
# - docs/
# - docker-compose.yml
# - pom.xml
# - README.md
```

## Environment Configuration

### 1. Create Environment File

```bash
# Copy the example environment file
cp .env.example .env
```

### 2. Edit Environment Variables

Open `.env` in your preferred text editor and configure:

```bash
# Database Configuration
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_DATABASE=cloud_storage
MYSQL_USER=clouduser
MYSQL_PASSWORD=your_secure_password

# Load Balancer Configuration
LB_PORT=6869
LB_ALGORITHM=ROUND_ROBIN  # Options: FCFS, SJN, ROUND_ROBIN

# Aggregator Configuration
AGG_PORT=9000

# Storage Node Configuration
STORAGE_NODE_PASSWORD=your_storage_password

# MQTT Configuration
MQTT_BROKER_HOST=mqtt-broker
MQTT_BROKER_PORT=1883

# Jenkins Configuration
JENKINS_ADMIN_PASSWORD=your_jenkins_password

# Gitea Configuration
GITEA_ADMIN_PASSWORD=your_gitea_password
```

**Security Note**: Use strong, unique passwords for production environments!

### 3. Configure Docker Compose Override (Optional)

For local development customizations:

```bash
# Copy the override example
cp docker-compose.override.yml.example docker-compose.override.yml

# Edit as needed
nano docker-compose.override.yml
```

## Building the Project

### 1. Build Parent Project

```bash
# Build the parent project
mvn clean install
```

### 2. Build Individual Services

```bash
# Build AggService
cd AggService
mvn clean package
cd ..

# Build Load Balancer
cd cloudlb
mvn clean package
cd ..

# Build Cloud GUI
cd cloud-gui
mvn clean package
cd ..

# Build Host Manager
cd hostmanager
mvn clean package
cd ..
```

### 3. Build Docker Images

```bash
# Build all Docker images
docker compose build

# Or build specific services:
docker compose build aggservice
docker compose build cloudlb
docker compose build cloud-gui
```

## Starting Services

### 1. Start Core Infrastructure

```bash
# Start database and MQTT broker first
docker compose up -d lamp-server mqtt-broker

# Wait for services to be healthy (about 30 seconds)
docker compose ps

# Check logs
docker compose logs -f lamp-server mqtt-broker
```

### 2. Initialize Database

```bash
# The database schema will be automatically created
# You can verify by checking the logs:
docker compose logs lamp-server | grep "ready for connections"
```

### 3. Start Storage Nodes

```bash
# Start all storage nodes
docker compose up -d soft40051-files-container1 \
                     soft40051-files-container2 \
                     soft40051-files-container3 \
                     soft40051-files-container4

# Verify storage nodes are running
docker compose ps | grep soft40051-files
```

### 4. Start Processing Services

```bash
# Start aggregator service
docker compose up -d aggregator

# Start load balancer
docker compose up -d load-balancer

# Start host manager
docker compose up -d hostmanager
```

### 5. Start User Interface

```bash
# Start the JavaFX GUI container
docker compose up -d ntu-vm-soft40051
```

### 6. Start DevOps Tools (Optional)

```bash
# Start Jenkins
docker compose up -d jenkins-soft40051

# Start Gitea
docker compose up -d gitea
```

### 7. Start All Services at Once

```bash
# Start all services in detached mode
docker compose up -d

# Follow logs
docker compose logs -f
```

## Verification

### 1. Check Service Status

```bash
# Check all running containers
docker compose ps

# All services should show "Up" status
```

### 2. Verify Network Connectivity

```bash
# Check internal network
docker network ls | grep soft40051

# Inspect network
docker network inspect soft40051-cloud_soft40051_network
```

### 3. Test Database Connection

```bash
# Connect to MySQL
docker compose exec lamp-server mysql -u clouduser -p

# Enter password when prompted
# Run test query:
# SHOW DATABASES;
# USE cloud_storage;
# SHOW TABLES;
```

### 4. Test MQTT Broker

```bash
# Subscribe to test topic (in one terminal)
docker compose exec mqtt-broker mosquitto_sub -t "test/topic" -v

# Publish to test topic (in another terminal)
docker compose exec mqtt-broker mosquitto_pub -t "test/topic" -m "Hello MQTT"
```

### 5. Access Web Interfaces

Open your web browser and verify:

- **phpMyAdmin**: http://localhost:8080
  - Username: `clouduser`
  - Password: (from your .env file)

- **Jenkins**: http://localhost:8081
  - Initial admin password: Check logs or use configured password

- **Gitea**: http://localhost:3000
  - Set up admin account on first access

### 6. Test Load Balancer

```bash
# Check load balancer logs
docker compose logs -f load-balancer

# You should see health check messages
```

### 7. Test File Upload (Manual)

```bash
# Access the GUI via RDP
# On Linux/Mac:
# rdesktop localhost:3390

# On Windows:
# Remote Desktop Connection to localhost:3390
```

## Troubleshooting

### Common Issues

#### 1. Port Already in Use

```bash
# Find process using port (e.g., 3306)
sudo lsof -i :3306

# Or on Linux
sudo netstat -tulpn | grep 3306

# Kill the process or change port in docker-compose.yml
```

#### 2. Docker Daemon Not Running

```bash
# Start Docker service
sudo systemctl start docker

# Or on macOS/Windows, start Docker Desktop
```

#### 3. Permission Denied Errors

```bash
# Add user to docker group (Linux)
sudo usermod -aG docker $USER
newgrp docker

# Restart Docker service
sudo systemctl restart docker
```

#### 4. Out of Memory Errors

```bash
# Increase Docker memory limit
# Docker Desktop: Settings -> Resources -> Memory

# Check Docker resource usage
docker stats
```

#### 5. Container Fails to Start

```bash
# Check container logs
docker compose logs [service-name]

# Check detailed container info
docker inspect [container-name]

# Remove and recreate container
docker compose rm -f [service-name]
docker compose up -d [service-name]
```

#### 6. Database Connection Failed

```bash
# Ensure database is healthy
docker compose exec lamp-server mysqladmin -u root -p status

# Restart database
docker compose restart lamp-server

# Check database logs
docker compose logs lamp-server
```

#### 7. Maven Build Failures

```bash
# Clean Maven cache
mvn clean

# Update dependencies
mvn dependency:resolve

# Skip tests if needed
mvn clean package -DskipTests
```

#### 8. Network Issues

```bash
# Recreate Docker network
docker compose down
docker network prune
docker compose up -d
```

### Getting More Help

- **View all logs**: `docker compose logs`
- **View specific service logs**: `docker compose logs [service-name]`
- **Follow logs in real-time**: `docker compose logs -f`
- **Check Docker system**: `docker system df`
- **Clean up Docker**: `docker system prune -a` (WARNING: removes unused images)

### Useful Commands

```bash
# Stop all services
docker compose down

# Stop and remove volumes (WARNING: deletes data)
docker compose down -v

# Restart specific service
docker compose restart [service-name]

# Rebuild and restart service
docker compose up -d --build [service-name]

# Execute command in container
docker compose exec [service-name] [command]

# View resource usage
docker compose stats
```

## Next Steps

After successful setup:

1. Read the [Architecture Documentation](../architecture/ARCHITECTURE.md)
2. Review the [API Documentation](../api/)
3. Check the [Testing Guide](../testing/TESTING.md)
4. Explore [Contributing Guidelines](../../CONTRIBUTING.md)

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Maven Documentation](https://maven.apache.org/guides/)
- [JavaFX Documentation](https://openjfx.io/)
- [MySQL Documentation](https://dev.mysql.com/doc/)
- [MQTT Protocol](https://mqtt.org/)
