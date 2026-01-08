package com.ntu.cloudgui.app.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages the initialization and status of database connections.
 */
public class DatabaseManager {

    private static Connection mysqlConnection;

    /**
     * Initializes the databases. Establishes the initial MySQL connection.
     */
    public static void initializeDatabases() {
        // Initialize SQLite database
        try {
            SessionCacheRepository sessionCacheRepository = new SessionCacheRepository();
            sessionCacheRepository.initSchema();
        } catch (Exception e) {
            System.err.println("Failed to initialize SQLite database: " + e.getMessage());
        }

        // Establish initial MySQL connection
        try {
            getSharedMySqlConnection();
            System.out.println("Successfully established initial MySQL connection.");
        } catch (Exception e) {
            System.err.println("Failed to establish initial MySQL connection: " + e.getMessage());
        }
    }

    /**
     * Gets the shared MySQL connection, creating it if it's null or closed.
     * This method is synchronized to be thread-safe.
     * @return The active MySQL connection.
     * @throws SQLException if a connection cannot be established.
     */
    private static synchronized Connection getSharedMySqlConnection() throws SQLException {
        if (mysqlConnection == null || mysqlConnection.isClosed()) {
            mysqlConnection = MySqlConnectionManager.getConnection();
        }
        return mysqlConnection;
    }

    /**
     * Checks if the MySQL database is connected by validating the existing connection.
     * This is much more efficient than creating a new connection for each check.
     * @return true if the MySQL database is connected and the connection is valid, false otherwise.
     */
    public static boolean isMysqlConnected() {
        try {
            // Get the connection and check if it's valid with a 1-second timeout.
            return getSharedMySqlConnection().isValid(1);
        } catch (SQLException e) {
            // If getting the connection or validating it fails, we are offline.
            return false;
        }
    }

    /**
     * Closes the shared MySQL connection. Should be called on application shutdown.
     */
    public static void closeMySqlConnection() {
        try {
            if (mysqlConnection != null && !mysqlConnection.isClosed()) {
                mysqlConnection.close();
                System.out.println("MySQL connection closed.");
            }
        } catch (SQLException e) {
             System.err.println("Failed to close MySQL connection: " + e.getMessage());
        }
    }
}
