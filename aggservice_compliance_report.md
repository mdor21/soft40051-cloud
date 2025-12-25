# Aggregator Service Compliance Report

**Date:** 2025-12-25
**Status:** Highly Compliant (One Change Required for Distinction Grade)

---

## 1. Executive Summary

This report details a comprehensive audit of the `AggService` microservice against the functional, non-functional, and operational requirements for the SOFT40051 coursework.

**Conclusion:** The `AggService` is a high-quality, robust, and secure implementation that meets **almost all** of the specified requirements. It correctly handles file encryption, chunking, validation, and distribution. However, it is missing the centralized logging feature required for a top grade.

**Required Changes:**
1.  **Implement Centralized Logging:** A mechanism must be added to store key operational logs (e.g., "File Uploaded," "CRC32 Mismatch") into a new table in the MySQL database.

---

## 2. Point-by-Point Compliance Analysis

### I. Functional Requirements (FRs)

| Requirement | Status | Evidence & Analysis |
| :--- | :--- | :--- |
| **FR1: File Encryption** | **Met** | The `EncryptionService` correctly uses Java's `Cipher` with `AES/GCM/NoPadding` (AES-256), a secure and modern standard. |
| **FR2: File Partitioning** | **Met** | `FileProcessingService` correctly reads files and splits them into 5MB chunks. |
| **FR3: Integrity Validation** | **Met** | `CrcValidationService` is used to calculate CRC32 checksums before encryption. `FileProcessingService` correctly validates these checksums after decryption during download. |
| **FR4: Metadata Management** | **Met** | The service correctly uses JPA repositories (`FileMetadataRepository`, `ChunkMetadataRepository`) to persist file and chunk metadata to the MySQL database. |
| **FR5: Distributed Distribution** | **Met** | `ChunkStorageService` uses the JSch library to correctly transmit encrypted chunks to the file server containers via SFTP. |
| **FR6: File Reconstruction** | **Met** | The `processDownload` method correctly and securely reverses the entire upload process, including chunk retrieval, decryption, validation, and reassembly. |

### II. Non-Functional Requirements (NFRs)

| Requirement | Status | Evidence & Analysis |
| :--- | :--- | :--- |
| **NFR1: Decoupled Architecture**| **Met** | `docker-compose.yml` defines the `aggregator` as a standalone, isolated container. |
| **NFR2: Concurrency Control** | **Met** | `FileProcessingService` correctly uses a `java.util.concurrent.Semaphore` to lock all file operations (upload, download, delete), preventing race conditions. |
| **NFR3: Error Handling** | **Met** | The codebase makes excellent use of `try-catch-finally` blocks and includes a critical `rollbackFileUpload` method to clean up failed operations. |
| **NFR4: Scalability Alignment** | **Met** | `ChunkStorageService` is designed to work with a configurable list of file servers and uses a round-robin strategy for distribution, fully supporting the multi-backend design. |

### III. Connectivity and Network Configuration

| Requirement | Status | Evidence & Analysis |
| :--- | :--- | :--- |
| **C1: MySQL Database** | **Met** | The service connects to `lamp-server` on port `3306` via JDBC, as configured in `docker-compose.yml`. |
| **C2: File Servers** | **Met** | The service uses the JSch library to connect to the file servers on the internal port 22. |
| **C3: Internal Network** | **Met** | The container is correctly attached to the `soft40051_network` bridge network. |
| **C4: Load Balancer** | **Met** | The service exposes the necessary HTTP endpoints for the Load Balancer via Java's built-in HTTP server. |

### IV. Configuration and Naming Conventions

| Requirement | Status | Evidence & Analysis |
| :--- | :--- | :--- |
| **NC1: Container Name** | **Met** | `docker-compose.yml` sets `container_name: aggregator`. |
| **NC2: Development Environment**| **Met** | The `pom.xml` confirms this is a Maven project. |
| **NC3: Runtime Compatibility**| **Met** | `pom.xml` specifies Java 21, meeting the JRE 20+ requirement. |

### V. Logs, Metrics, and Monitoring

| Requirement | Status | Evidence & Analysis |
| :--- | :--- | :--- |
| **L1: Comprehensive Logging** | **Met** | The service has excellent, detailed logging for all key events, including encryption, validation, errors, and chunk processing. |
| **L2: Visual Feedback** | *(N/A)* | This is a requirement for the GUI, not the Aggregator. The Aggregator successfully provides the logs. |
| **L3: Centralised Auditing** | **Not Met** | **Change Required.** The service currently logs only to the console/file. There is no implementation for storing operational logs in the MySQL database. |

---

## 3. Detailed Required Changes

### 1. Implement Centralized Logging to MySQL

To achieve a Distinction grade, a centralized logging mechanism must be implemented.

**Recommendation:**

1.  **Create a `LogEntry` Entity:**
    *   Define a new JPA entity named `LogEntry` with fields like `id` (auto-incrementing), `timestamp`, `serviceName` (e.g., "Aggregator"), `logLevel` (e.g., "INFO", "ERROR"), and `message`.
2.  **Create a `LogEntryRepository`:**
    *   Create a standard JPA repository interface for the `LogEntry` entity.
3.  **Create a `DatabaseLoggingService`:**
    *   Create a new service class that takes the `LogEntryRepository` as a dependency.
    *   This service should have a method like `log(level, message)` that creates a `LogEntry` object and saves it to the database.
4.  **Integrate into `FileProcessingService`:**
    *   Inject the `DatabaseLoggingService` into `FileProcessingService`.
    *   At key points in the code (e.g., after a successful upload, on a CRC32 mismatch error), call the `databaseLoggingService.log(...)` method in addition to the existing `logger.info(...)` calls.
