# AggService

## 1) Service Purpose
The Aggregator securely processes files: encrypts, chunks, validates CRC32, and
stores chunk metadata in MySQL while writing encrypted chunks to SFTP storage
nodes. It also performs the reverse on download.

## 2) Runtime & Interfaces
- Container name: `aggregator`
- Internal ports: `SERVER_PORT` (default `9000`), `LB_LOG_PORT` (default `9100`)
- Protocols: HTTP, JDBC (MySQL), SFTP (SSH)
- Incoming: HTTP requests from Load Balancer, HTTP log ingestion from LB
- Outgoing: JDBC to MySQL, SFTP to storage nodes

## 3) End-to-End Flow (service-specific)
Upload: LB -> Aggregator -> encrypt -> chunk -> CRC32 -> MySQL metadata -> SFTP chunks  
Download: LB -> Aggregator -> fetch chunk metadata -> SFTP chunks -> CRC32 check -> decrypt -> response

## 4) Code Flow (key execution path)
- Startup: `AggServiceServer` -> `Configuration` -> `DatabaseManager` -> `SchemaManager`
  (optional reset) -> `LbLogServer` -> `AggApiServer`.
- Request handling: `AggApiServer` -> `FileProcessingService`.
- Error/logging: `DatabaseLoggingService` -> `LogEntryRepository` -> `System_Logs`.

## 5) Class Summary (short notes)
- `AggServiceServer` - service bootstrap and lifecycle.
- `AggApiServer` - HTTP endpoints for upload/download/delete.
- `FileProcessingService` - encryption, chunking, CRC32, metadata.
- `EncryptionService` - AES encryption/decryption.
- `CrcValidationService` - CRC32 checksum validation.
- `ChunkStorageService` - SFTP upload/download/delete for chunks.
- `FileMetadataRepository` - MySQL file metadata access.
- `ChunkMetadataRepository` - MySQL chunk metadata access.
- `DatabaseLoggingService` - audit logging to MySQL.
- `LbLogServer` - log ingestion from Load Balancer.

## 6) Directory Tree (depth 4)
```
AggService
|-- src
|   |-- main
|   |   |-- java
|   |   |   `-- com
|   |   `-- resources
|   |       `-- application.properties
|   `-- test
|       `-- java
|           `-- com
|-- src.test.java
|   `-- com
|       `-- ntu
|           `-- cloudgui
`-- pom.xml
```

## 7) Environment Variables
Loaded by `Configuration` (env overrides properties):
- `MYSQL_HOST` default: `lamp-server`
- `MYSQL_PORT` default: `3306`
- `MYSQL_DATABASE` default: `dbtutorial`
- `MYSQL_USER` default: `admin`
- `MYSQL_PASSWORD` default: `admin`
- `DB_CONNECT_RETRIES` default: `10`
- `DB_CONNECT_DELAY_MS` default: `3000`
- `LB_LOG_PORT` default: `9100`
- `SERVER_PORT` default: `9000`
- `THREAD_POOL_SIZE` default: `10`
- `CHUNK_SIZE_BYTES` default: `1048576`
- `ENCRYPTION_KEY` required (no default)
- `STORAGE_NODES` default: `soft40051-files-container1,...,soft40051-files-container4`
- `FILE_SERVER_MAX_CONNECTIONS` default: `5`
- `RESET_SCHEMA` default: `false`
- `SFTP_USER`, `SFTP_PASS`, `SFTP_PORT` used by SFTP client

## 8) How to Run / Verify
- Docker Compose: `docker compose up -d --build`
- Verify port:
  - `curl -X POST http://localhost:9000/api/files/upload -H 'X-File-Name:test.txt' --data 'hello'`
- Verify logs:
  - `docker logs aggregator | tail`

## 9) Known Issues / Troubleshooting
- `ENCRYPTION_KEY` missing: service fails on startup.
- SFTP upload fails: verify `SFTP_USER/SFTP_PASS` and storage node connectivity.
- MySQL not ready: check `lamp-server` healthcheck and credentials.
- Schema not re-applied: remove volumes or set `RESET_SCHEMA=true`.
