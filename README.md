# SOFT40051 Cloud-Based Distributed File Storage System

## System Architecture

This is a high-security distributed file storage system simulating a global logistics network with the following components:

### Service Components

| Service | Container Name | Internal Port | External Port | Purpose |
|---------|---------------|---------------|---------------|---------|
| Main App/GUI | ntu-vm-soft40051 | 3389, 22 | 3390, 2022 | JavaFX portal, RDP/SSH |
| Load Balancer | load-balancer | 6869 | N/A | Multi-threaded task scheduling |
| Aggregator | aggregator | 9000 | N/A | Security and file processing |
| MySQL | lamp-server | 3306 | 3306 | Metadata and logs |
| MQTT Broker | mqtt-broker | 1883 | 1883 | Scaling and monitoring |
| Storage Node 1-4 | soft40051-files-container[1-4] | 22 | 4848-4851 | Encrypted chunk storage |
| Jenkins | jenkins-soft40051 | 8080, 50000 | 8081, 50001 | CI/CD automation |
| Gitea | gitea | 3000, 22 | 3000, 2222 | Version control |

## Quick Start

### Prerequisites
- Docker Engine 20.10+
- Docker Compose 2.0+
- 8GB RAM minimum
- 20GB free disk space

### Deployment

1. **Clone and navigate to the project:**
```bash
cd /Users/mado/soft40051-cloud
```

2. **Configure environment:**
```bash
cp .env.example .env
# Edit .env with your secure credentials
```

3. **Start all services:**
```bash
docker-compose up -d
```

4. **Verify services are running:**
```bash
docker-compose ps
```

5. **Check logs:**
```bash
docker-compose logs -f
```

### Access Points

- **JavaFX Application**: RDP to `localhost:3390`
- **phpMyAdmin**: http://localhost:8080
- **Jenkins**: http://localhost:8081
- **Gitea**: http://localhost:3000
- **MQTT Broker**: `localhost:1883`

## Application Flow

1. **Request Initiation**: JavaFX GUI → Load Balancer (Port 6869)
2. **Scheduling**: FCFS/SJN/Round Robin + 1-5s simulated latency
3. **Processing**: Aggregator (Port 9000) handles encryption, chunking, CRC32
4. **Storage**: Chunks distributed via SSH (Port 22) to 4 storage nodes
5. **Metadata**: MySQL (Port 3306) stores chunk locations and checksums
6. **Scaling**: MQTT messages trigger dynamic container deployment

## Database Schema

### Core Tables
- `users`: User profiles with encrypted passwords
- `file_metadata`: File information and status
- `file_chunks`: Individual chunk details with CRC32
- `file_permissions`: ACL for file sharing
- `system_logs`: Audit trail for all operations
- `load_balancer_stats`: Performance metrics
- `storage_node_health`: Node monitoring

## Development Guidelines

### Required Tools
- NetBeans IDE with Maven
- Scene Builder for JavaFX UI
- JUnit for testing
- Doxygen for documentation

### Build Requirements
- JRE 20 or higher
- Output: Executable JAR file
- Concurrency: Semaphore-based file
