package com.ntu.cloudgui.aggservice;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;

public class SchemaManager {

    private static final Logger logger = LoggerFactory.getLogger(SchemaManager.class);

    public void resetDatabaseSchema(Connection connection) {
        if (connection == null) {
            logger.error("Cannot reset database schema with a null connection.");
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            logger.info("Starting database schema reset...");

            // 1. Drop existing tables
            logger.debug("Dropping existing tables...");
            stmt.execute("DROP TABLE IF EXISTS `ACL`;");
            stmt.execute("DROP TABLE IF EXISTS `File_Metadata`;");
            stmt.execute("DROP TABLE IF EXISTS `System_Logs`;");
            stmt.execute("DROP TABLE IF EXISTS `User_Profiles`;");
            logger.info("Dropped legacy tables successfully.");

            // 2. Recreate tables
            logger.debug("Recreating tables...");
            stmt.execute("""
                CREATE TABLE `User_Profiles` (
                  `user_id` int(11) NOT NULL AUTO_INCREMENT,
                  `username` varchar(255) NOT NULL UNIQUE,
                  `encrypted_password` varchar(255) NOT NULL,
                  `role` varchar(50) NOT NULL,
                  PRIMARY KEY (`user_id`)
                );
            """);

            stmt.execute("""
                CREATE TABLE `File_Metadata` (
                  `file_id` int(11) NOT NULL AUTO_INCREMENT,
                  `user_id` int(11) NOT NULL,
                  `file_name` varchar(255) NOT NULL,
                  `file_size_bytes` bigint NOT NULL,
                  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`file_id`),
                  FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`)
                );
            """);

            stmt.execute("""
                CREATE TABLE `ACL` (
                  `acl_id` int(11) NOT NULL AUTO_INCREMENT,
                  `file_id` int(11) NOT NULL,
                  `user_id` int(11) NOT NULL,
                  `permission` varchar(50) NOT NULL,
                  PRIMARY KEY (`acl_id`),
                  FOREIGN KEY (`file_id`) REFERENCES `File_Metadata`(`file_id`),
                  FOREIGN KEY (`user_id`) REFERENCES `User_Profiles`(`user_id`)
                );
            """);

            stmt.execute("""
                CREATE TABLE `System_Logs` (
                  `log_id` int(11) NOT NULL AUTO_INCREMENT,
                  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `event_type` varchar(255) NOT NULL,
                  `user_id` int(11) NULL,
                  `description` text NOT NULL,
                  PRIMARY KEY (`log_id`)
                );
            """);
            logger.info("Core tables recreated successfully.");

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

            logger.info("Database schema reset completed successfully.");

        } catch (SQLException e) {
            logger.error("Failed to reset database schema.", e);
            // It's critical to throw this exception to stop the application startup
            // if the database isn't in a valid state.
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
