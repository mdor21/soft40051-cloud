# soft40051-cloud Project Analysis

## AggService

### 1. Service Overview
The `AggService` is a core component of the system, responsible for processing all file uploads and downloads. It acts as a data aggregator, taking incoming files, breaking them into smaller chunks, encrypting them, and distributing them across a set of SFTP servers for storage. It also manages the metadata for the files and chunks, storing it in a MySQL database. This service is critical for the system's data storage and retrieval capabilities.

### 2. Folder Structure
The `AggService` follows a standard Maven project structure. The key directories and files are:

```
AggService/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ntu/
    │   │           └── cloudgui/
    │   │               └── aggservice/
    │   │                   ├── config/
    │   │                   ├── controller/
    │   │                   ├── dto/
    │   │                   ├── exception/
    │   │                   ├── model/
    │   │                   ├── repository/
    │   │                   ├── service/
    │   │                   └── AggService.java
    │   └── resources/
    └── test/
```

### 3. Key Classes and Logical Flow

#### `AggService.java`
This is the main entry point for the service. It starts an embedded HTTP server that listens for upload and download requests. It defines two primary endpoints:
- `POST /files/upload`: Handles file uploads.
- `GET /files/{fileId}/download`: Handles file downloads.

The request handling methods in this class delegate the actual processing to the `FileProcessingService`.

#### `service/FileProcessingService.java`
This is the heart of the `AggService`. It orchestrates the entire file processing workflow, which includes:
- **File Validation:** Checks if the file exists, is readable, and is within the size limits.
- **Chunking:** Splits the file into 5MB chunks.
- **Encryption:** Encrypts each chunk using AES-256.
- **CRC32 Checksum:** Calculates a CRC32 checksum for each chunk to ensure data integrity.
- **Storage:** Stores each encrypted chunk on one of the available SFTP servers in a round-robin fashion.
- **Metadata Management:** Saves metadata for the file and each chunk to the database.

The `processUpload` method handles the entire upload workflow, while the `processDownload` method retrieves the chunks, decrypts them, and reassembles the file. The class also includes a `rollbackFileUpload` method to clean up in case of an error.

#### `service/EncryptionService.java`
This service is responsible for encrypting and decrypting the file chunks. It uses AES-256 as the encryption algorithm.

#### `service/ChunkStorageService.java`
This service manages the storage of the encrypted chunks on the SFTP servers. It uses the JSch library to connect to the SFTP servers and upload the chunks. It also selects the server to use for each chunk in a round-robin fashion.

#### `service/CrcValidationService.java`
This service calculates the CRC32 checksum for each chunk before it is encrypted and stored. This checksum is used to verify the integrity of the data when the file is downloaded.

#### `service/MetadataService.java` & `repository/`
The `MetadataService` and the repositories (`FileMetadataRepository`, `ChunkMetadataRepository`) are responsible for interacting with the MySQL database to store and retrieve the metadata for the files and chunks.

### 4. Service-to-Service Interactions
- **Load Balancer (`cloudlb`):** The `cloudlb` service forwards file upload and download requests to the `AggService`.
- **MySQL Database (`lamp-server`):** The `AggService` connects to the MySQL database to store and retrieve file and chunk metadata.
- **SFTP Servers (`soft40051-files-container*`):** The `AggService` connects to the SFTP servers to store the encrypted file chunks.

## cloud-gui

### 1. Service Overview
The `cloud-gui` service is a JavaFX-based desktop application that provides the user interface for the file storage system. It allows users to authenticate, manage their files, and perform upload and download operations. It communicates with the `cloudlb` service to perform file operations and with a MySQL database for user and file metadata. It also features an offline mode, where it uses a local SQLite database to cache session information.

### 2. Folder Structure
The `cloud-gui` service follows a standard Maven project structure. The key directories and files are:

```
cloud-gui/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ntu/
    │   │           └── cloudgui/
    │   │               └── app/
    │   │                   ├── client/
    │   │                   ├── controller/
    │   │                   ├── db/
    │   │                   ├── model/
    │   │                   ├── service/
    │   │                   ├── session/
    │   │                   └── MainApp.java
    │   └── resources/
    └── test/
```

### 3. Key Classes and Logical Flow

#### `MainApp.java`
This is the main entry point for the JavaFX application. It initializes the application, tests the connection to the MySQL database, and loads the login screen. It also handles the initialization of the local SQLite session cache for offline mode.

#### `controller/LoginController.java`
This controller handles the user authentication process. It takes the username and password from the login screen, calls the `AuthService` to validate the credentials, and then loads the main dashboard if the login is successful.

#### `controller/DashboardController.java`
This controller manages the main dashboard of the application. It loads the different views (files, terminals, users, logs) into the main content area. It also handles the logout process.

#### `controller/FilesController.java`
This is the most important controller in the `cloud-gui` service. It handles all the file management operations, including:
- **Listing files:** It retrieves the list of files for the current user from the `FileService`.
- **Uploading files:** It opens a file chooser to select a file, and then uses the `LoadBalancerClient` to upload the file to the `cloudlb` service.
- **Downloading files:** It uses the `LoadBalancerClient` to download the selected file from the `cloudlb` service.
- **Creating, saving, and deleting files:** It interacts with the `FileService` to perform these operations.

#### `client/LoadBalancerClient.java`
This class is responsible for communicating with the `cloudlb` service to perform file uploads and downloads. It uses HTTP requests to send and receive file data.

#### `service/AuthService.java` and `service/FileService.java`
These services contain the business logic for user authentication and file management. They interact with the database to store and retrieve user and file metadata.

### 4. Service-to-Service Interactions
- **Load Balancer (`cloudlb`):** The `cloud-gui` service communicates with the `cloudlb` service to upload and download files.
- **MySQL Database (`lamp-server`):** The `cloud-gui` service connects to the MySQL database to store and retrieve user and file metadata.
- **Local SQLite Database:** The `cloud-gui` service uses a local SQLite database to cache session information for offline mode.

## cloudlb

### 1. Service Overview
The `cloudlb` service is a custom-built load balancer that acts as the central entry point for all file operations. It receives upload and download requests from the `cloud-gui`, queues them, and then distributes them to the available `AggService` instances based on a configurable scheduling algorithm. It also monitors the health of the aggregator nodes and can request to scale them up or down by communicating with the `hostmanager` service via an MQTT broker.

### 2. Folder Structure
The `cloudlb` service follows a standard Maven project structure. The key directories and files are:

```
cloudlb/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ntu/
    │   │           └── cloudgui/
    │   │               └── cloudlb/
    │   │                   ├── core/
    │   │                   ├── LoadBalancerAPIServer.java
    │   │                   ├── LoadBalancerWorker.java
    │   │                   ├── MainLb.java
    │   │                   └── ScalingService.java
    │   └── resources/
    └── test/
```

### 3. Key Classes and Logical Flow

#### `MainLb.java`
This is the main entry point for the load balancer. It initializes all the core components, including the request queue, the node registry, the scheduler, the health checker, and the scaling service. It also starts the `LoadBalancerWorker` and the `LoadBalancerAPIServer`.

#### `LoadBalancerAPIServer.java`
This class starts an embedded HTTP server that listens for incoming requests from the `cloud-gui`. It defines endpoints for file uploads, downloads, and health checks. When a request is received, it is added to the `RequestQueue` for processing.

#### `LoadBalancerWorker.java`
This class runs in a separate thread and continuously processes requests from the `RequestQueue`. It retrieves the next request, selects the best `AggService` instance to handle it using the configured scheduler, and then forwards the request to that instance.

#### `core/Scheduler`
The `cloudlb` service includes three different scheduling algorithms:
- `FcfsScheduler`: First-Come, First-Served
- `SjnScheduler`: Shortest Job Next
- `RoundRobinScheduler`: Round-robin

The scheduler to use is configured via an environment variable.

#### `ScalingService.java`
This service monitors the size of the `RequestQueue`. If the queue size exceeds a certain threshold, it publishes a message to an MQTT topic to request that a new `AggService` instance be started. If the queue size is below another threshold, it requests that an instance be stopped.

### 4. Service-to-Service Interactions
- **GUI Client (`cloud-gui`):** The `cloudlb` service receives file upload and download requests from the `cloud-gui`.
- **Aggregation Service (`AggService`):** The `cloudlb` service forwards the requests to the `AggService` instances for processing.
- **Host Manager (`hostmanager`):** The `cloudlb` service communicates with the `hostmanager` service via an MQTT broker to request scaling of the `AggService` instances.
- **MQTT Broker (`mqtt-broker`):** The `cloudlb` service uses the MQTT broker to publish scaling requests.

## hostmanager

### 1. Service Overview
The `hostmanager` service is responsible for managing the lifecycle of the Docker containers that run the `AggService`. It listens for scaling requests from the `cloudlb` service on an MQTT topic and then uses the Docker command-line interface to start or stop `AggService` containers as needed. It also includes a health checker to monitor the status of the containers and can restart them if they become unhealthy.

### 2. Folder Structure
The `hostmanager` service follows a standard Maven project structure. The key directories and files are:

```
hostmanager/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── ntu/
    │   │           └── cloudgui/
    │   │               └── hostmanager/
    │   │                   ├── container/
    │   │                   ├── docker/
    │   │                   ├── health/
    │   │                   ├── model/
    │   │                   ├── mqtt/
    │   │                   ├── scaling/
    │   │                   └── HostManager.java
    │   └── resources/
    └── test/
```

### 3. Key Classes and Logical Flow

#### `HostManager.java`
This is the main entry point for the host manager. It initializes all the core components, including the Docker command executor, the container manager, the MQTT connection manager, and the scaling logic. It also subscribes to the MQTT topic for scaling requests and starts the health check scheduler.

#### `mqtt/MqttConnectionManager.java`
This class manages the connection to the MQTT broker. It handles connecting, subscribing to topics, and publishing messages.

#### `scaling/ScalingLogic.java`
This class contains the logic for scaling the `AggService` containers up and down. It receives scaling requests from the `HostManager` and then uses the `DockerCommandExecutor` to start or stop the containers.

#### `docker/DockerCommandExecutor.java`
This class is responsible for executing Docker commands on the host machine. It uses the Docker command-line interface to start, stop, and inspect containers.

### 4. Service-to-Service Interactions
- **Load Balancer (`cloudlb`):** The `hostmanager` service receives scaling requests from the `cloudlb` service via the MQTT broker.
- **MQTT Broker (`mqtt-broker`):** The `hostmanager` service uses the MQTT broker to receive scaling requests.
- **Docker Engine:** The `hostmanager` service interacts with the Docker engine on the host machine to manage the `AggService` containers.

## Overall Service Integration

The services in the `soft40051-cloud` project work together to provide a distributed file storage system with a graphical user interface. The following is a summary of the key interactions between the services:

1.  **`cloud-gui` -> `cloudlb` (HTTP):** The user interacts with the `cloud-gui` to upload and download files. The `cloud-gui` sends HTTP requests to the `cloudlb` to perform these operations.
2.  **`cloud-gui` -> `lamp-server` (MySQL):** The `cloud-gui` connects to the MySQL database on the `lamp-server` to manage user accounts and file metadata.
3.  **`cloudlb` -> `AggService` (HTTP):** The `cloudlb` receives the file operation requests and forwards them to one of the available `AggService` instances. The choice of which `AggService` to use is determined by the configured scheduling algorithm.
4.  **`cloudlb` -> `mqtt-broker` (MQTT):** The `cloudlb` monitors its request queue and publishes scaling requests to the `mqtt-broker` if the queue size gets too large or too small.
5.  **`hostmanager` -> `mqtt-broker` (MQTT):** The `hostmanager` subscribes to the scaling requests from the `mqtt-broker`.
6.  **`hostmanager` -> Docker Engine:** When the `hostmanager` receives a scaling request, it uses the Docker command-line interface to start or stop `AggService` containers.
7.  **`AggService` -> `lamp-server` (MySQL):** The `AggService` connects to the MySQL database on the `lamp-server` to store and retrieve metadata about the files and their chunks.
8.  **`AggService` -> `soft40051-files-container*` (SFTP):** The `AggService` connects to the SFTP servers on the `soft40051-files-container`s to store the encrypted file chunks.

This architecture allows for a scalable and resilient file storage system. The `cloudlb` and `hostmanager` services work together to ensure that there are always enough `AggService` instances to handle the current load, and the `AggService` itself distributes the file data across multiple storage servers to provide redundancy.
