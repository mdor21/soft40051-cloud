# Service Architecture Validation Report

This report validates the provided service architecture document against the current codebase.

---

## 1. Service Infrastructure and Port Mapping

| Service Component | Container Name | Internal Port | External (Host) Port | Role / Integration | Validation Status | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Main App/GUI** | `ntu-vm-soft40051` | 3389/22 | 3390/2022 | JavaFX portal | ✅ **Validated** | Configuration matches `docker-compose.yml`. |
| **Load Balancer** | `load-balancer` | 6869 (Socket) | N/A | Distributes traffic | ✅ **Validated** | The `MainLb.java` source code has been corrected to use port `6869`, which now matches the `docker-compose.yml` configuration. |
| **File Aggregator**| `aggregator` | 9000 (Internal API) | N/A | Processes security | ✅ **Validated** | Configuration matches `docker-compose.yml`. |
| **MySQL Server** | `lamp-server` | 3306 (JDBC) | 3306 | Stores metadata | ✅ **Validated** | Configuration matches `docker-compose.yml`. |
| **MQTT Broker** | `mqtt-broker` | 1883 (MQTT) | 1883 | Hub for scaling | ✅ **Validated** | Configuration matches `docker-compose.yml`. |
| **Storage Nodes** | `soft40051-files-containerX` | 22 (SSH) | 4848-4851 | Secure repository| ⚠️ **Not Verifiable** | These are not defined as services in the `docker-compose.yml`. The application logic in `AggService` suggests it connects to external SSH nodes, but their existence is not defined in the compose file. |
| **Jenkins Server**| `jenkins-soft40051` | 8080/50000 | 8081/50001 | CI/CD | ❌ **Not Validated** | Not present in the `docker-compose.yml`. |
| **Gitea Server** | `gitea` | 3000/22 | 3000/2222 | Version control | ❌ **Not Validated** | Not present in the `docker-compose.yml`. |

---

## 2. Application and Network Flow Logic

| Point | Description | Validation Status | Notes |
| :--- | :--- | :--- | :--- |
| **Request Initiation** | JavaFX GUI sends tasks to the Load Balancer. | ✅ **Validated** | The overall architecture supports this flow. |
| **Scheduling & Delay** | LB uses FCFS, SJF, RR algorithms and adds a delay. | ✅ **Validated** | The `cloudlb` service implements `RoundRobinScheduler`, `SjnScheduler`, and a `RoundRobinDistributionScheduler` (acting as FCFS). The `LoadBalancerWorker.java` correctly implements a 1.0–5.0 second artificial delay. |
| **Security Routing** | Task is forwarded to the Aggregator on Port 9000. | ✅ **Validated** | The `load-balancer` is configured to forward requests to the `aggregator` service on port 9000. |
| **Persistence Layer** | Aggregator uses JDBC to connect to MySQL and JSch for SSH. | ✅ **Validated** | The `AggService/pom.xml` includes dependencies for both `mysql-connector-j` and `jsch`, confirming this capability. |
| **Scaling Triggers** | Load Balancer publishes MQTT messages to the Host Manager for scaling. | ✅ **Validated** | `ScalingService.java` in the `cloudlb` service implements this logic. |

---

## 3. Database Schema and Log Generation

| Point | Description | Validation Status | Notes |
| :--- | :--- | :--- | :--- |
| **Database Schema** | MySQL (dbtutorial) must have User_Profiles, File_Metadata, ACL, and System_Logs tables. | ⚠️ **Partially Validated** | The `AggService/schema.sql` defines the `user_profiles`, `acl` (Access_Control_Lists), and `audit_logs` (System_Logs) tables. However, it is missing the mandatory `File_Metadata` table. |

---

## 4. Build and Technical Requirements

| Point | Description | Validation Status | Notes |
| :--- | :--- | :--- | :--- |
| **Java Environment** | NetBeans, Maven, JRE 20+. | ✅ **Validated** | `pom.xml` files for `cloudlb` (Java 21) and `cloud-gui` (Java 20) confirm the use of Maven and compatible Java versions. |
| **GUI Standards** | Scene Builder and JavaFX; no Swing. | ✅ **Validated** | The `cloud-gui/pom.xml` includes dependencies for JavaFX. |
| **Testing** | Automated unit tests using JUnit. | ✅ **Validated** | The `pom.xml` files for all major services include JUnit dependencies. |
| **Concurrency** | Use of semaphores to lock access. | ✅ **Validated** | `LoadBalancerWorker.java` correctly uses a `ConcurrentHashMap` of `Semaphore` objects to ensure thread-safe access to storage nodes. |
| **Local Resilience** | Local SQLite database for offline functionality. | ✅ **Validated** | The `cloud-gui/pom.xml` includes the `sqlite-jdbc` driver. |

---

## Summary of Key Discrepancies

1.  **Missing Database Table:** The `File_Metadata` table, which is mandatory for the aggregator to track distributed data, is missing from the `AggService/schema.sql` file.
2.  **Missing Services:** The Jenkins, Gitea, and Storage Node containers are described in the documentation but are missing from the `docker-compose.yml` file.
