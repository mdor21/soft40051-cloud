# SOFT40051 Cloud-Based Distributed File Storage System

[![Java CI](https://github.com/mdor21/soft40051-cloud/actions/workflows/java-ci.yml/badge.svg)](https://github.com/mdor21/soft40051-cloud/actions/workflows/java-ci.yml)
[![Docker Build](https://github.com/mdor21/soft40051-cloud/actions/workflows/docker-build.yml/badge.svg)](https://github.com/mdor21/soft40051-cloud/actions/workflows/docker-build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-20%2B-blue.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-red.svg)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-20.10%2B-blue.svg)](https://www.docker.com/)

> A high-security distributed file storage system simulating a global logistics network with microservices architecture, load balancing, encryption, and real-time monitoring.

## 📋 Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Features](#features)
- [Quick Start](#quick-start)
- [Documentation](#documentation)
- [Technology Stack](#technology-stack)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)

## Overview

This is a high-security distributed file storage system simulating a global logistics network with the following components:

The SOFT40051 Cloud Storage System is an academic project demonstrating distributed systems principles, cloud computing concepts, and microservices architecture. Built with Java and Docker, it provides:

- **Distributed Storage**: Files encrypted, chunked, and distributed across multiple nodes
- **Load Balancing**: Multiple scheduling algorithms (FCFS, SJN, Round Robin)
- **High Security**: AES encryption, CRC32 checksums, SSH communication
- **Real-time Monitoring**: MQTT-based system monitoring and alerting
- **Scalability**: Containerized architecture for easy horizontal scaling
- **DevOps Integration**: CI/CD with Jenkins, version control with Gitea

## Features

### 🔐 Security Features

- **AES Encryption**: All file chunks encrypted before storage
- **CRC32 Checksums**: Data integrity verification
- **SSH Communication**: Secure inter-node file transfer
- **Access Control**: File permission system with ACLs
- **Audit Logging**: Complete audit trail of all operations

### ⚖️ Load Balancing

- **FCFS**: First-Come-First-Served scheduling
- **SJN**: Shortest-Job-Next prioritization
- **Round Robin**: Even distribution across nodes
- **Starvation Prevention**: Aging mechanism for waiting requests
- **Health Checking**: Automatic node health monitoring

### 📊 Monitoring & Management

- **MQTT Integration**: Real-time event streaming
- **Health Dashboards**: System status visualization
- **Performance Metrics**: Load balancer statistics
- **Storage Node Monitoring**: Individual node health tracking

### 🎯 Architecture Highlights

- **Microservices**: Independent, scalable services
- **Containerization**: Docker-based deployment
- **Database Layer**: MySQL for metadata persistence
- **Message Queue**: MQTT for event-driven architecture

## System Architecture

### Service Components

| Service | Container Name | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------------|---------|
| Main App/GUI | ntu-vm-soft40051 | 3389, 22 | 3390, 2022 | JavaFX portal, RDP/SSH |
| Load Balancer | load-balancer | 6869 | N/A | Multi-threaded task scheduling |
| Aggregator | aggregator | 9000 | N/A | Security and file processing |
| MySQL | lamp-server | 3306 | 3306 | Metadata and logs |
| MQTT Broker | mqtt-broker | 1883, 9001 | 1883, 9001 | Scaling and monitoring |
| Storage Node 1-4 | soft40051-files-container[1-4] | 22 | 4848-4851 | Encrypted chunk storage |
| Jenkins | jenkins-soft40051 | 8080, 50000 | 8081, 50001 | CI/CD automation |
| Gitea | gitea | 3000, 22 | 3000, 2222 | Version control |

For detailed architecture diagrams and component descriptions, see [Architecture Documentation](docs/architecture/ARCHITECTURE.md).

## Quick Start

### Prerequisites

- **Docker Engine** 20.10+
- **Docker Compose** 2.0+
- **RAM**: 8GB minimum, 16GB recommended
- **Disk Space**: 20GB free minimum

For detailed setup instructions, see [Setup Guide](docs/setup/SETUP.md).

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/mdor21/soft40051-cloud.git
   cd soft40051-cloud
   ```

2. **Configure environment:**
   ```bash
   cp .env.example .env
   # Edit .env with your secure credentials
   ```

3. **Start all services:**
   ```bash
   docker compose up -d
   ```

4. **Verify services:**
   ```bash
   docker compose ps
   ```

5. **View logs:**
   ```bash
   docker compose logs -f
   ```

### Access Points

- **JavaFX Application**: RDP to `localhost:3390`
- **phpMyAdmin**: http://localhost:8080
- **Jenkins**: http://localhost:8081
- **Gitea**: http://localhost:3000
- **MQTT Broker**: `localhost:1883`

## Documentation

### 📚 Available Documentation

- **[Architecture Overview](docs/architecture/ARCHITECTURE.md)** - System design and component details
- **[Setup Guide](docs/setup/SETUP.md)** - Complete installation and configuration
- **[Testing Guide](docs/testing/TESTING.md)** - Testing strategies and best practices
- **[Contributing Guidelines](CONTRIBUTING.md)** - How to contribute to the project
- **[Security Policy](SECURITY.md)** - Security considerations and reporting
- **[Code of Conduct](CODE_OF_CONDUCT.md)** - Community standards
- **[Changelog](CHANGELOG.md)** - Version history and updates

### 🔄 Application Flow

1. **Request Initiation**: JavaFX GUI → Load Balancer (Port 6869)
2. **Scheduling**: FCFS/SJN/Round Robin + 1-5s simulated latency
3. **Processing**: Aggregator (Port 9000) handles encryption, chunking, CRC32
4. **Storage**: Chunks distributed via SSH (Port 22) to 4 storage nodes
5. **Metadata**: MySQL (Port 3306) stores chunk locations and checksums
6. **Scaling**: MQTT messages trigger dynamic container deployment

### 🗄️ Database Schema

#### Core Tables

- `users`: User profiles with encrypted passwords
- `file_metadata`: File information and status
- `file_chunks`: Individual chunk details with CRC32
- `file_permissions`: ACL for file sharing
- `system_logs`: Audit trail for all operations
- `load_balancer_stats`: Performance metrics
- `storage_node_health`: Node monitoring

## Technology Stack

### Backend

- **Language**: Java 20
- **Build Tool**: Maven 3.8+
- **Database**: MySQL 8.0
- **Message Broker**: Eclipse Mosquitto (MQTT)
- **SSH/SFTP**: JSch library

### Frontend

- **Framework**: JavaFX 20
- **UI Design**: Scene Builder
- **Access**: RDP/SSH

### Infrastructure

- **Containerization**: Docker & Docker Compose
- **CI/CD**: Jenkins, GitHub Actions
- **Version Control**: Git, Gitea
- **Monitoring**: MQTT, phpMyAdmin

### Development

- **IDE**: NetBeans (recommended), IntelliJ IDEA
- **Testing**: JUnit 5, Mockito
- **Code Quality**: Checkstyle, SpotBugs
- **Documentation**: JavaDoc, Markdown

## Development

### Building from Source

```bash
# Build parent project
mvn clean install

# Build individual services
cd AggService && mvn clean package
cd cloudlb && mvn clean package
cd cloud-gui && mvn clean package
cd hostmanager && mvn clean package
```

### Running Tests

```bash
# Run all tests
mvn test

# Run service-specific tests
cd AggService && mvn test      # 4 tests
cd cloudlb && mvn test          # 20 tests
cd hostmanager && mvn test      # 9 tests

# Run with coverage
mvn clean test jacoco:report

# Run integration tests
mvn verify
```

### Post-Merge Verification

After any pull or merge operation, verify that all services and components are functional:

```bash
# Build all services
bash scripts/build-all.sh

# Run service health checks
bash scripts/ci/service-health-check.sh

# Run component connectivity tests
bash scripts/ci/component-connectivity-test.sh

# Run all tests
bash scripts/run-tests.sh
```

See [Post-Merge Verification Guide](docs/POST_MERGE_VERIFICATION.md) for detailed testing information.

### Code Quality

```bash
# Run Checkstyle
mvn checkstyle:check

# Run SpotBugs
mvn spotbugs:check

# Check for dependency vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details on:

- Code of Conduct
- Development workflow
- Coding standards
- Testing requirements
- Pull request process

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests (`mvn test`)
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Course**: SOFT40051 - Distributed Systems
- **Institution**: Nottingham Trent University (NTU)
- **Contributors**: See [CONTRIBUTORS.md](CONTRIBUTORS.md)
- **Special Thanks**: 
  - Course instructors and teaching assistants
  - Open source community for libraries and tools
  - Docker and container ecosystem contributors

## Project Status

🚀 **Active Development** - This project is actively maintained as part of the SOFT40051 coursework.

### Roadmap

- [x] Core microservices architecture
- [x] Load balancing with multiple algorithms
- [x] File encryption and chunking
- [x] Health monitoring and MQTT integration
- [x] CI/CD pipeline setup
- [ ] Enhanced security features (key rotation, HSM)
- [ ] Performance optimization
- [ ] Extended documentation and tutorials
- [ ] Additional storage backends

## Support

- **Issues**: [GitHub Issues](https://github.com/mdor21/soft40051-cloud/issues)
- **Discussions**: [GitHub Discussions](https://github.com/mdor21/soft40051-cloud/discussions)
- **Documentation**: [docs/](docs/)

---

**Made with ❤️ for SOFT40051 at NTU**
