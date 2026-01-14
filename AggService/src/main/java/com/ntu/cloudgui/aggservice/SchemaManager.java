package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaManager {

    private static final Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    /**
     * Ensures the database schema is correctly configured.
     * <p>
     * This method is idempotent. It mirrors the MySQL init SQL so a
     * RESET_SCHEMA=true run still produces the expected schema.
     *
     * @param connection The database connection.
     */
    public void resetDatabaseSchema(Connection connection) {
        if (connection == null) {
            logger.error("Cannot ensure database schema with a null connection.");
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            logger.info("Ensuring database schema exists...");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `User_Profiles` (
                    `user_id` INT AUTO_INCREMENT PRIMARY KEY,
                    `username` VARCHAR(50) UNIQUE NOT NULL,
                    `password_hash` VARCHAR(255) NOT NULL,
                    `password_salt` VARCHAR(64) NOT NULL,
                    `role` ENUM('standard', 'admin') DEFAULT 'standard',
                    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX `idx_username` (`username`)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `File_Metadata` (
                    `metadata_id` INT AUTO_INCREMENT PRIMARY KEY,
                    `file_id` VARCHAR(36) NOT NULL,
                    `owner_id` INT NOT NULL,
                    `original_filename` VARCHAR(255) NOT NULL,
                    `file_size` BIGINT NOT NULL,
                    `total_chunks` INT NOT NULL,
                    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (`owner_id`) REFERENCES `User_Profiles`(`user_id`) ON DELETE CASCADE,
                    INDEX `idx_file_id` (`file_id`)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `Chunk_Metadata` (
                    `chunk_id` INT AUTO_INCREMENT PRIMARY KEY,
                    `file_id` VARCHAR(36) NOT NULL,
                    `chunk_sequence` INT NOT NULL,
                    `crc32_checksum` VARCHAR(8) NOT NULL,
                    `server_location` VARCHAR(50) NOT NULL,
                    `chunk_size` INT NOT NULL,
                    `storage_path` VARCHAR(255) NOT NULL,
                    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (`file_id`) REFERENCES `File_Metadata`(`file_id`) ON DELETE CASCADE,
                    INDEX `idx_file_chunk` (`file_id`, `chunk_sequence`)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `ACL` (
                    `acl_id` INT AUTO_INCREMENT PRIMARY KEY,
                    `file_id` VARCHAR(36) NOT NULL,
                    `user_id` INT NOT NULL,
                    `permission_level` ENUM('read', 'write') NOT NULL,
                    `granted_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    `granted_by` INT NOT NULL,
                    FOREIGN KEY (`file_id`) REFERENCES `File_Metadata`(`file_id`) ON DELETE CASCADE,
                    FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`) ON DELETE CASCADE,
                    UNIQUE KEY `unique_file_user` (`file_id`, `user_id`)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `System_Logs` (
                    `log_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                    `timestamp` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    `event_type` VARCHAR(50) NOT NULL,
                    `user_id` INT,
                    `description` TEXT NOT NULL,
                    `severity` ENUM('INFO', 'WARNING', 'ERROR', 'CRITICAL') DEFAULT 'INFO',
                    `service_name` VARCHAR(50),
                    FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`) ON DELETE SET NULL,
                    INDEX `idx_timestamp` (`timestamp`),
                    INDEX `idx_event_type` (`event_type`)
                )
            """);

            logger.info("Schema verification complete. All required tables exist.");

            // Seeding the admin user is handled by the Docker init SQL.

        } catch (SQLException e) {
            logger.error("Failed to ensure database schema.", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
