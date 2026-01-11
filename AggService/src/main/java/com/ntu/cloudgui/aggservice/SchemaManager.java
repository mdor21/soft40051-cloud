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
     * This method has been made idempotent. It uses `CREATE TABLE IF NOT EXISTS`
     * to avoid dropping tables on every application start. The responsibility for
     * creating the schema and seeding the admin user has been moved to the
     * `02-create-schema.sql` script executed by the MySQL container.
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

            // The table creation is now handled by the Docker entrypoint script.
            // This Java code is kept for legacy purposes or for environments
            // where the entrypoint script might not run. It's idempotent.
            logger.debug("Verifying core tables exist...");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `User_Profiles` (
                  `user_id` int(11) NOT NULL AUTO_INCREMENT,
                  `username` varchar(255) NOT NULL UNIQUE,
                  `encrypted_password` varchar(255) NOT NULL,
                  `role` varchar(50) NOT NULL,
                  `last_modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`user_id`)
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS `File_Metadata` (
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
                CREATE TABLE IF NOT EXISTS `ACL` (
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
                CREATE TABLE IF NOT EXISTS `System_Logs` (
                  `log_id` int(11) NOT NULL AUTO_INCREMENT,
                  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `event_type` varchar(255) NOT NULL,
                  `user_id` int(11) NULL,
                  `description` text NOT NULL,
                  PRIMARY KEY (`log_id`)
                );
            """);

            logger.info("Schema verification complete. All required tables exist.");

            // Seeding the admin user is now handled by the `02-create-schema.sql` script.
            // This section is removed to centralize database setup.

        } catch (SQLException e) {
            logger.error("Failed to ensure database schema.", e);
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
