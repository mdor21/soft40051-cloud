# Cloud Simulation Platform

This repository contains a multi-service cloud simulation platform that supports
file storage, encryption, load balancing, scaling, and a JavaFX GUI for users.

## Architecture Overview

High-level flow:

```
JavaFX GUI  ->  Load Balancer  ->  Aggregator  ->  SFTP Storage Nodes
     |                |               |                |
  SQLite cache    MQTT broker      MySQL metadata   /data volumes
     |                |
     +----> HostManager (Docker CLI via ProcessBuilder)
```

Key points:
- The GUI talks to the Load Balancer for uploads/downloads and metadata.
- The Load Balancer schedules traffic across healthy nodes and publishes
  scaling requests via MQTT.
- The Aggregator encrypts files, splits them into chunks, validates CRC32,
  stores chunk metadata in MySQL, and writes chunks to SFTP nodes.
- The Host Manager runs on the host (not in Docker) and scales storage
  containers via Docker CLI.
- The GUI keeps a local SQLite cache for offline/low-connectivity operation.

## Service Docs

- `cloud-gui/README.md`
- `cloudlb/README.md`
- `AggService/README.md`
- `hostmanager/README.md`
- `config/mysql/README.md`
- `mqtt/README.md`
- `storage/README.md`

## Services and Roles

- `ntu-vm-soft40051`: Desktop VM container hosting the JavaFX GUI environment
  (RDP/SSH access).
- `load-balancer`: HTTP API for file operations, scheduling, and MQTT scaling.
- `aggregator`: Encrypts, chunks, validates, and stores file metadata.
- `lamp-server`: MySQL database for user profiles, file metadata, ACL, logs.
- `mqtt-broker`: MQTT broker for scaling coordination.
- `soft40051-files-container*`: SFTP storage nodes for encrypted chunks.
- `hostmanager` (host-only): Listens to MQTT and uses Docker CLI to scale.
- `jenkins-soft40051`, `gitea`: Optional CI/CD and Git services.

## Container Names and Ports

- `ntu-vm-soft40051`: RDP `3390`, SSH `2022`
- `load-balancer`: `6869`
- `aggregator`: `9000`
- `lamp-server`: `3306`
- `mqtt-broker`: `1883` (and `9001` optional)
- `soft40051-files-container1..4`: `4848-4851`
- `jenkins-soft40051`: `8081`
- `gitea`: `3000` (SSH `2222`)

## Default Credentials

- GUI login: `admin` / `admin` (seeded in MySQL)
- MySQL: `admin` / `admin` (see `.env`)
- SFTP storage nodes: `ntu-user` / `ntu-user` (see `.env`)

## Environment Variables

Create a `.env` file at the repo root. Defaults below match the current example:

```
# MySQL
MYSQL_ROOT_PASSWORD=admin
MYSQL_USER=admin
MYSQL_PASSWORD=admin
MYSQL_DB=dbtutorial

# MQTT
MQTT_BROKER_URL=tcp://mqtt-broker:1883

# Service ports
LB_PORT=6869
AGG_PORT=9000
LB_LOG_PORT=9100

# Load balancer
SCHEDULER_TYPE=ROUNDROBIN
LB_DELAY_MS_MIN=1000
LB_DELAY_MS_MAX=5000

# SFTP storage
SFTP_USER=ntu-user
SFTP_PASS=ntu-user
SFTP_PORT=22
ENCRYPTION_KEY=a-secure-key-for-testing
```

Additional env vars used by services:

- Load Balancer: `MQTT_BROKER_HOST`, `MQTT_BROKER_PORT`, `STORAGE_NODES`,
  `SERVER_PORT`.
- Aggregator: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`,
  `MYSQL_PASSWORD`, `STORAGE_NODES`, `SERVER_PORT`, `RESET_SCHEMA`,
  `LB_LOG_PORT`.
- Host Manager: `MQTT_BROKER_HOST`, `MQTT_BROKER_PORT`, `MQTT_TOPIC`,
  `MQTT_TOPIC_EVENTS`, `STORAGE_BASE_DIR`.

## Docker Compose Setup

Prerequisites:
- Docker + Docker Compose
- Java 21+
- Maven 3.9+

Start all containerized services:

```bash
docker compose up -d --build
```

## Fresh Start (Reset Volumes)

Use this when you need a clean database and storage state:

```bash
docker-compose down -v
docker-compose up -d --build
```

If you are using the Compose v2 plugin:
```bash
docker compose down -v
docker compose up -d --build
```

## Database Initialization

MySQL schema and seed data are initialized from:
- `config/mysql/01-init-core-schema.sql`
- `scripts/mysql-init.sh` (healthcheck + schema enforcement)

Init scripts run only once for a new `mysql_data` volume. To re-run them,
remove volumes via `docker compose down -v`.

The schema includes `User_Profiles`, `File_Metadata`, `Chunk_Metadata`, `ACL`,
and `System_Logs`, with a default admin user.

The Aggregator can also re-apply the schema when `RESET_SCHEMA=true`
(`AggService/src/main/java/com/ntu/cloudgui/aggservice/SchemaManager.java`).

## Startup Sequence

1) Create `.env` and set required values.  
2) Start containers:

```bash
docker compose up -d --build
```

3) Build and run the Host Manager (host machine, not in Docker):

```bash
mvn -f hostmanager/pom.xml clean package
java -jar hostmanager/target/hostmanager-1.0-SNAPSHOT.jar
```

Or use the helper script:

```bash
./scripts/hostmanager-start.sh
```

4) Connect to the GUI:
- RDP: `localhost:3390`
- SSH: `ssh -p 2022 ntu-user@localhost`

## Running the GUI Locally (Optional)

If you want to run the JavaFX GUI on your host:

```bash
mvn -f cloud-gui/pom.xml javafx:run
```

## Testing

JUnit tests are currently available in the AggService module:

```bash
mvn -f AggService/pom.xml test
```

Note: existing test code lives in `AggService/src.test.java/...` and will not
be picked up by Maven unless moved to `AggService/src/test/java` or the
`testSourceDirectory` is updated.

## Service Roles in Detail

### Load Balancer
- Exposes HTTP API for upload/download/delete.
- Uses scheduling (Round Robin/FCFS/SJN) and node health checks.
- Injects artificial latency for realism.
- Publishes scale-up/down requests via MQTT.

### Aggregator
- Encrypts and chunks file content.
- Stores chunk metadata in MySQL with CRC32 checksums.
- Writes encrypted chunks to SFTP storage nodes.
- Reassembles and decrypts on download.
- Logs events to `System_Logs`.

### Host Manager
- Subscribes to MQTT scaling requests.
- Uses Docker CLI via `ProcessBuilder` to start/stop storage containers.
- Tracks container health and lifecycle.

### GUI and Offline Support
- JavaFX UI for users (files, sharing, users, logs, terminals).
- Local SQLite cache for sessions, file metadata, and queued ops.
- Sync service reconciles SQLite <-> MySQL when online.
