// ============================================================================
// MISSING ENTRY #2: SessionCacheRepository for Local SQLite
// ============================================================================
// LOCATION: src/main/java/com/ntu/cloudgui/app/db/SessionCacheRepository.java
// CONNECTIVITY: Local Embedded SQLite File
// PROTOCOL: SQLite JDBC (local file access)
// LOCATION: ${user.home}/.ntu_cloudgui/session_cache.db

package com.ntu.cloudgui.app.db;

import static com.ntu.cloudgui.app.db.MySqlConnectionManager.getConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * MISSING ENTRY - SQLite repository for local session caching.
 * 
 * CONNECTIVITY CONFIGURATION:
 * - Database Type: SQLite (embedded, file-based)
 * - Storage Location: ${user.home}/.ntu_cloudgui/session_cache.db
 * - Protocol: SQLite JDBC (local file access, no network required)
 * 
 * Purpose:
 * - Provides offline capability when MySQL is unavailable
 * - Caches user session data for quick access
 * - Stores temporary file metadata and sync status
 * 
 * Connection Flow:
 * 1. MainApp calls initLocalSessionCache()
 * 2. SessionCacheRepository.initSchema() creates tables if they don't exist
 * 3. Other services query/update local cache in addition to remote MySQL
 */
public class SessionCacheRepository {

    // SQLite database file location
    private static final String DB_PATH = System.getProperty("user.home") + "/.ntu_cloudgui/session_cache.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    static {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Initialize SQLite database schema for session caching.
     * Creates tables if they don't exist.
     * 
     * @throws SQLException if schema initialization fails
     */
    public void initSchema() throws SQLException {
        // Ensure directory exists
        Path dirPath = Paths.get(System.getProperty("user.home"), ".ntu_cloudgui");
        try {
            Files.createDirectories(dirPath);
        } catch (Exception e) {
            System.err.println("Failed to create cache directory: " + e.getMessage());
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create session_users table for caching user data
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS session_users (" +
                "  id INTEGER PRIMARY KEY," +
                "  username TEXT UNIQUE NOT NULL," +
                "  password_hash TEXT NOT NULL," +
                "  role TEXT NOT NULL," +
                "  last_synced TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create file_cache table for storing local file metadata
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS file_cache (" +
                "  id TEXT PRIMARY KEY," +
                "  name TEXT NOT NULL," +
                "  owner TEXT NOT NULL," +
                "  size_bytes INTEGER," +
                "  sync_status TEXT," +
                "  local_path TEXT," +
                "  last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            // Create sync_queue table for managing offline sync operations
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS sync_queue (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  operation TEXT NOT NULL," +
                "  file_id TEXT NOT NULL," +
                "  data TEXT," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            System.out.println("SQLite schema initialized successfully at: " + DB_PATH);
        }
    }
}
