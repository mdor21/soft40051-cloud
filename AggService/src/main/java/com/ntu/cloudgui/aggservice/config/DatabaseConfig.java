// ============================================================================
// MISSING ENTRY #1: DatabaseConfig - MySQL Connection Pool
// ============================================================================
// LOCATION: src/main/java/com/ntu/cloudgui/aggservice/config/DatabaseConfig.java
// CONNECTIVITY: MySQL Database
// PROTOCOL: JDBC over TCP/IP
// PORT: 3306

package com.ntu.cloudgui.aggservice.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MISSING ENTRY - Configuration for MySQL database connectivity.
 * 
 * CONNECTIVITY CONFIGURATION:
 * - Database Host: mysql (Docker service name in soft40051_network)
 * - Port: 3306 (MySQL default, internal Docker)
 * - Database: cloudgui_db
 * - User: cloudgui_user
 * - Password: from environment variable MYSQL_PASSWORD
 * 
 * PURPOSE:
 * - Store file metadata (id, name, owner, total_chunks, encryption_algo, upload_timestamp)
 * - Store chunk metadata (id, file_id, chunk_index, server_host, remote_path, crc32, size)
 * - Store user profiles and access control lists (ACLs)
 * 
 * SCHEMA TABLES:
 * 1. file_metadata
 *    - fileId (UUID, PRIMARY KEY)
 *    - originalName (VARCHAR)
 *    - totalChunks (INT)
 *    - encryptionAlgo (VARCHAR) - e.g., "AES/GCM/NoPadding"
 *    - uploadTimestamp (TIMESTAMP)
 * 
 * 2. chunk_metadata
 *    - id (INT, PRIMARY KEY AUTO_INCREMENT)
 *    - fileId (UUID, FOREIGN KEY)
 *    - chunkIndex (INT) - order of chunk in file
 *    - serverHost (VARCHAR) - "storage1", "storage2", etc.
 *    - remotePath (VARCHAR) - path on File Server
 *    - crc32Checksum (BIGINT) - for integrity validation
 *    - sizeBytes (BIGINT)
 * 
 * 3. user_profiles
 *    - username (VARCHAR, PRIMARY KEY)
 *    - password_hash (VARCHAR)
 *    - role (VARCHAR) - "ADMIN" or "STANDARD"
 * 
 * 4. file_acl
 *    - file_id (UUID)
 *    - user_id (VARCHAR)
 *    - permission (VARCHAR) - "READ" or "READ_WRITE"
 */
public class DatabaseConfig {

    // JDBC URL for MySQL - points to mysql service in Docker network
    private static final String JDBC_URL = System.getenv().getOrDefault(
        "MYSQL_JDBC_URL",
        "jdbc:mysql://mysql:3306/cloudgui_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
    );

    private static final String DB_USER = System.getenv().getOrDefault("MYSQL_USER", "cloudgui_user");
    private static final String DB_PASSWORD = System.getenv().getOrDefault("MYSQL_PASSWORD", "cloudgui_password");

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    /**
     * Get a JDBC connection to MySQL.
     * Called by repositories for executing queries.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Initialize database schema on startup.
     * Creates tables if they don't exist.
     */
    public void initializeSchema() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create file_metadata table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS file_metadata (" +
                "  fileId VARCHAR(36) PRIMARY KEY," +
                "  originalName VARCHAR(255) NOT NULL," +
                "  totalChunks INT NOT NULL," +
                "  encryptionAlgo VARCHAR(50) DEFAULT 'AES/GCM/NoPadding'," +
                "  uploadTimestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create chunk_metadata table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS chunk_metadata (" +
                "  id INT PRIMARY KEY AUTO_INCREMENT," +
                "  fileId VARCHAR(36) NOT NULL," +
                "  chunkIndex INT NOT NULL," +
                "  serverHost VARCHAR(50) NOT NULL," +
                "  remotePath VARCHAR(255) NOT NULL," +
                "  crc32Checksum BIGINT," +
                "  sizeBytes BIGINT," +
                "  FOREIGN KEY (fileId) REFERENCES file_metadata(fileId) ON DELETE CASCADE," +
                "  INDEX idx_fileId (fileId)" +
                ")"
            );

            // Create user_profiles table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS user_profiles (" +
                "  username VARCHAR(100) PRIMARY KEY," +
                "  password_hash VARCHAR(255) NOT NULL," +
                "  role VARCHAR(20) DEFAULT 'STANDARD'" +
                ")"
            );

            // Create file_acl table for access control
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS file_acl (" +
                "  id INT PRIMARY KEY AUTO_INCREMENT," +
                "  file_id VARCHAR(36) NOT NULL," +
                "  user_id VARCHAR(100) NOT NULL," +
                "  permission VARCHAR(20)," +
                "  FOREIGN KEY (file_id) REFERENCES file_metadata(fileId) ON DELETE CASCADE," +
                "  INDEX idx_file_user (file_id, user_id)" +
                ")"
            );

            System.out.println("Database schema initialized successfully");
        }
    }
}
