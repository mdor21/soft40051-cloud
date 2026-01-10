# Database and Default Admin Account Report

This report provides a definitive record of the MySQL and SQLite database schemas, as well as the creation process for the default `admin` user.

## MySQL Database (`lamp-server`)

*   **Database Name:** `dbtutorial`
*   **Tables:**
    *   `ACL`
    *   `File_Metadata`
    *   `System_Logs`
    *   `User_Profiles`
*   **Schema Definitions:**
    *   **`User_Profiles`**:
        ```sql
        CREATE TABLE `User_Profiles` (
          `user_id` int(11) NOT NULL AUTO_INCREMENT,
          `username` varchar(255) NOT NULL,
          `encrypted_password` varchar(255) NOT NULL,
          `role` varchar(50) NOT NULL,
          PRIMARY KEY (`user_id`),
          UNIQUE KEY `username` (`username`)
        ) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1
        ```
    *   **`File_Metadata`**:
        ```sql
        CREATE TABLE `File_Metadata` (
          `file_id` int(11) NOT NULL AUTO_INCREMENT,
          `user_id` int(11) NOT NULL,
          `file_name` varchar(255) NOT NULL,
          `file_size_bytes` bigint(20) NOT NULL,
          `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
          PRIMARY KEY (`file_id`),
          KEY `user_id` (`user_id`),
          CONSTRAINT `File_Metadata_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `User_Profiles` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=latin1
        ```
    *   **`ACL`**:
        ```sql
        CREATE TABLE `ACL` (
          `acl_id` int(11) NOT NULL AUTO_INCREMENT,
          `file_id` int(11) NOT NULL,
          `user_id` int(11) NOT NULL,
          `permission` varchar(50) NOT NULL,
          PRIMARY KEY (`acl_id`),
          KEY `file_id` (`file_id`),
          KEY `user_id` (`user_id`),
          CONSTRAINT `ACL_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `File_Metadata` (`file_id`),
          CONSTRAINT `ACL_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `User_Profiles` (`user_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=latin1
        ```
    *   **`System_Logs`**:
        ```sql
        CREATE TABLE `System_Logs` (
          `log_id` int(11) NOT NULL AUTO_INCREMENT,
          `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          `event_type` varchar(255) NOT NULL,
          `user_id` int(11) DEFAULT NULL,
          `description` text NOT NULL,
          PRIMARY KEY (`log_id`)
        ) ENGINE=InnoDB DEFAULT CHARSET=latin1
        ```

## SQLite Database (local session DB)

*   **File Path:** `/app/comp20081.db` (relative to the `cloud-gui` application's working directory)
*   **Tables:**
    *   `file_cache`
    *   `session_users`
    *   `sync_queue`
*   **Schema Definitions:**
    ```sql
    CREATE TABLE session_users (
      id INTEGER PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      password TEXT NOT NULL,
      role TEXT NOT NULL,
      last_synced TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      last_modified TIMESTAMP
    );

    CREATE TABLE file_cache (
      id TEXT PRIMARY KEY,
      name TEXT NOT NULL,
      owner TEXT NOT NULL,
      size_bytes INTEGER,
      sync_status TEXT,
      local_path TEXT,
      last_modified TIMESTAMP
    );

    CREATE TABLE sync_queue (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      operation TEXT NOT NULL,
      entity_type TEXT NOT NULL,
      entity_id TEXT,
      payload TEXT,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    ```

## Default `admin` Account

Yes, the default `admin` account (with the password "admin") is automatically created by the application.

This logic is located in the `AggService` module, specifically in the `SchemaManager.java` file within the `resetDatabaseSchema` method.

Here is the relevant code snippet from `AggService/src/main/java/com/ntu/cloudgui/aggservice/SchemaManager.java`:

```java
// 3. Seed default admin user
logger.debug("Seeding default admin user...");
String adminPassword = "admin";
String hashedPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt());

String sql = "INSERT INTO `User_Profiles` (`username`, `encrypted_password`, `role`) VALUES (?, ?, ?);";
try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
    pstmt.setString(1, "admin");
    pstmt.setString(2, hashedPassword);
    pstmt.setString(3, "admin");
    pstmt.executeUpdate();
    logger.info("Default admin user created successfully.");
}
```

This logic runs during the normal Docker startup flow because the `AggServiceServer`'s `main` method creates a `DatabaseManager`, which in turn calls the `SchemaManager.resetDatabaseSchema()` method. This ensures that every time the `aggregator` service starts with a fresh database, the default admin account is created.
