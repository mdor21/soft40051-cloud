# System Architecture

## Overview

The SOFT40051 Cloud-Based Distributed File Storage System is a comprehensive distributed computing project that simulates a secure, scalable cloud storage infrastructure. The system employs a microservices architecture with multiple specialized components working together to provide file storage, retrieval, and management capabilities.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Client Layer                                │
│                                                                      │
│  ┌──────────────────┐                    ┌──────────────────┐     │
│  │   JavaFX GUI     │◄───RDP/SSH────────►│   Web Browser    │     │
│  │  (cloud-gui)     │                    │  (phpMyAdmin)    │     │
│  └────────┬─────────┘                    └──────────────────┘     │
└───────────┼──────────────────────────────────────────────────────────┘
            │
            │ HTTP/TCP
            ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      Load Balancer Layer                            │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────┐    │
│  │               Load Balancer (cloudlb)                     │    │
│  │  • Request Queue Management                               │    │
│  │  • Scheduling (FCFS/SJN/Round Robin)                     │    │
│  │  • Health Checking                                        │    │
│  │  • Simulated Latency (1-5s)                             │    │
│  │  • Concurrency Control (Semaphores)                      │    │
│  └────────────────┬──────────────────────────────────────────┘    │
└──────────────────┼───────────────────────────────────────────────────┘
                   │
                   │ HTTP
                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     Processing Layer                                │
│                                                                      │
│  ┌───────────────────────────────────────────────────────────┐    │
│  │            Aggregator Service (AggService)                │    │
│  │  • File Encryption (AES)                                  │    │
│  │  • File Chunking                                          │    │
│  │  • CRC32 Checksum Generation                             │    │
│  │  • Chunk Distribution                                     │    │
│  │  • File Reconstruction                                    │    │
│  │  • Database Integration                                   │    │
│  └────────────────┬──────────────────────────────────────────┘    │
└──────────────────┼───────────────────────────────────────────────────┘
                   │
                   │ SSH/SFTP
                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       Storage Layer                                 │
│                                                                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │
│  │  Storage    │  │  Storage    │  │  Storage    │  │ Storage  │ │
│  │  Node 1     │  │  Node 2     │  │  Node 3     │  │ Node 4   │ │
│  │  :4848      │  │  :4849      │  │  :4850      │  │ :4851    │ │
│  │  Encrypted  │  │  Encrypted  │  │  Encrypted  │  │Encrypted │ │
│  │   Chunks    │  │   Chunks    │  │   Chunks    │  │ Chunks   │ │
│  └─────────────┘  └─────────────┘  └─────────────┘  └──────────┘ │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                     Supporting Services                             │
│                                                                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐            │
│  │    MySQL     │  │     MQTT     │  │     Host     │            │
│  │   Database   │  │    Broker    │  │   Manager    │            │
│  │   :3306      │  │    :1883     │  │              │            │
│  │              │  │              │  │              │            │
│  │ • Metadata   │  │ • Monitoring │  │ • Health     │            │
│  │ • Logs       │  │ • Scaling    │  │ • Discovery  │            │
│  │ • Users      │  │ • Events     │  │ • SSH Check  │            │
│  └──────────────┘  └──────────────┘  └──────────────┘            │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐                              │
│  │   Jenkins    │  │    Gitea     │                              │
│  │    :8081     │  │    :3000     │                              │
│  │              │  │              │                              │
│  │ • CI/CD      │  │ • Git Repos  │                              │
│  │ • Automation │  │ • Collab     │                              │
│  └──────────────┘  └──────────────┘                              │
└─────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Client Layer

#### JavaFX GUI (cloud-gui)
- **Purpose**: User interface for file operations
- **Technology**: JavaFX, Java 20
- **Access**: RDP (port 3390), SSH (port 2022)
- **Features**:
  - File upload/download interface
  - User authentication
  - File management
  - Progress tracking

### 2. Load Balancer Layer

#### Load Balancer (cloudlb)
- **Purpose**: Distribute requests across backend services
- **Technology**: Java, Multi-threaded
- **Port**: 6869 (internal)
- **Features**:
  - **Scheduling Algorithms**:
    - FCFS (First-Come-First-Served)
    - SJN (Shortest-Job-Next)
    - Round Robin
  - **Health Checking**: Periodic probes of backend nodes
  - **Simulated Latency**: 1-5 second delays for realism
  - **Starvation Prevention**: Aging mechanism for long-waiting requests
  - **Concurrency Control**: Semaphore-based locking per node
  - **Request Queue**: Thread-safe priority queue

### 3. Processing Layer

#### Aggregator Service (AggService)
- **Purpose**: Process files before storage
- **Technology**: Java, Socket-based server
- **Port**: 9000 (internal)
- **Features**:
  - **Encryption**: AES encryption for security
  - **Chunking**: Split files into manageable pieces
  - **Checksums**: CRC32 for data integrity
  - **Distribution**: Store chunks across multiple nodes
  - **Reconstruction**: Reassemble files on download
  - **Database Integration**: Metadata persistence

### 4. Storage Layer

#### Storage Nodes (1-4)
- **Purpose**: Persistent encrypted chunk storage
- **Technology**: SSH/SFTP enabled containers
- **Ports**: 4848-4851 (external SSH)
- **Features**:
  - Individual volume management
  - Encrypted chunk storage
  - SSH-based file transfer
  - Independent scaling

### 5. Supporting Services

#### MySQL Database (lamp-server)
- **Purpose**: Metadata and audit storage
- **Port**: 3306
- **Schema**:
  - `users`: User authentication
  - `file_metadata`: File information
  - `file_chunks`: Chunk locations
  - `file_permissions`: Access control
  - `system_logs`: Audit trail
  - `load_balancer_stats`: Performance metrics
  - `storage_node_health`: Node status

#### MQTT Broker
- **Purpose**: Event messaging and monitoring
- **Ports**: 1883 (MQTT), 9001 (WebSocket)
- **Features**:
  - Real-time monitoring messages
  - Scaling event triggers
  - System health notifications
  - Pub/Sub messaging pattern

#### Host Manager
- **Purpose**: Node discovery and health monitoring
- **Technology**: Java
- **Features**:
  - Storage node registration
  - SSH connectivity verification
  - Health status reporting
  - Dynamic node discovery

#### Jenkins
- **Purpose**: CI/CD automation
- **Ports**: 8081 (UI), 50001 (agents)
- **Features**:
  - Automated builds
  - Test execution
  - Deployment pipelines

#### Gitea
- **Purpose**: Version control
- **Ports**: 3000 (HTTP), 2222 (SSH)
- **Features**:
  - Git repository hosting
  - Code collaboration
  - Issue tracking

## Data Flow

### Upload Flow

1. **User Initiates Upload**
   - User selects file in JavaFX GUI
   - GUI sends request to Load Balancer

2. **Load Balancer Processing**
   - Request enters priority queue
   - Scheduler selects based on algorithm
   - Simulated latency applied
   - Health check performed on target node

3. **Aggregator Processing**
   - File received from load balancer
   - File encrypted with AES
   - File split into chunks
   - CRC32 checksum calculated per chunk
   - Metadata prepared for database

4. **Storage Distribution**
   - Chunks distributed via SSH to storage nodes
   - Round-robin or intelligent placement
   - Chunk metadata saved to MySQL
   - File metadata updated

5. **Confirmation**
   - Success/failure logged to database
   - MQTT notification sent
   - Response returned to user

### Download Flow

1. **User Requests Download**
   - User selects file in GUI
   - Request sent to Load Balancer

2. **Load Balancer Routing**
   - Request queued and scheduled
   - Healthy aggregator selected
   - Request forwarded

3. **Aggregator Retrieval**
   - Query database for chunk locations
   - Retrieve chunks from storage nodes via SSH
   - Validate checksums (CRC32)
   - Decrypt chunks
   - Reassemble file

4. **File Delivery**
   - Complete file sent to load balancer
   - Load balancer forwards to client
   - Download logged to database

## Security Architecture

### Encryption
- **Algorithm**: AES (Advanced Encryption Standard)
- **Scope**: All file chunks before storage
- **Key Management**: Environment-based configuration

### Authentication
- **User Authentication**: MySQL-based user table
- **SSH Keys**: For inter-node communication
- **Access Control**: File permission system

### Data Integrity
- **Checksums**: CRC32 per chunk
- **Validation**: On write and read operations
- **Audit Logs**: All operations logged

## Scalability Design

### Horizontal Scaling
- **Storage Nodes**: Easily add more nodes
- **Aggregators**: Multiple instances possible
- **Load Balancer**: Can be replicated

### Vertical Scaling
- **Resource Allocation**: Docker resource limits
- **Database**: MySQL connection pooling
- **Thread Management**: Configurable worker threads

### MQTT-Based Dynamic Scaling
- **Triggers**: High load, node failure
- **Actions**: Spawn new containers
- **Notifications**: System-wide awareness

## High Availability

### Redundancy
- Multiple storage nodes
- Chunk distribution across nodes
- Database backup capabilities

### Health Monitoring
- Load balancer health checks
- MQTT monitoring messages
- Host manager verification

### Failure Handling
- Retry mechanisms
- Graceful degradation
- Error logging and alerting

## Network Architecture

### Internal Network (soft40051_network)
- Docker bridge network
- Service discovery via container names
- Isolated from external networks

### External Access Points
- Port mapping for user-facing services
- RDP/SSH for GUI access
- HTTP for web interfaces

## Technology Stack

### Backend
- Java 20
- Maven build system
- JDBC for database
- JSch for SSH/SFTP

### Frontend
- JavaFX 20
- Scene Builder for UI design

### Infrastructure
- Docker & Docker Compose
- MySQL 8
- Eclipse Mosquitto (MQTT)
- Jenkins
- Gitea

### Development
- NetBeans IDE
- Git version control
- JUnit 5 for testing
- Mockito for mocking

## Design Patterns

### Architectural Patterns
- **Microservices**: Separated concerns
- **Load Balancing**: Even distribution
- **Pub/Sub**: MQTT messaging
- **Repository Pattern**: Database access

### Concurrency Patterns
- **Producer-Consumer**: Request queue
- **Semaphores**: Resource locking
- **Thread Pools**: Worker management

## Future Enhancements

1. **Geographic Distribution**: Multi-region storage
2. **CDN Integration**: Faster content delivery
3. **Advanced Encryption**: Key rotation, HSM
4. **Machine Learning**: Predictive scaling
5. **Blockchain**: Immutable audit logs
6. **Kubernetes**: Container orchestration

## References

- [Distributed Systems Principles](https://www.amazon.com/Designing-Data-Intensive-Applications-Reliable-Maintainable/dp/1449373321)
- [Microservices Patterns](https://microservices.io/patterns/)
- [Docker Documentation](https://docs.docker.com/)
- [MQTT Protocol](https://mqtt.org/)
