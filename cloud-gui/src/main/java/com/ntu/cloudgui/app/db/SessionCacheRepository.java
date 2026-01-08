package com.ntu.cloudgui.app.db;

import com.ntu.cloudgui.app.model.User;
import com.ntu.cloudgui.app.model.Role;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite repository for local session caching and offline operations.
 *
 * Purpose:
 * - Provides offline capability when MySQL is unavailable.
 * - Caches user session data for quick access.
 * - Stores temporary file metadata and sync status.
 * - Queues database operations that occur while offline.
 */
public class SessionCacheRepository {

    /**
     * Initializes the SQLite database schema. Creates tables if they don't exist.
     * @throws SQLException if schema initialization fails
     */
    public void initSchema() throws SQLException {
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create session_users table for caching user data
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS session_users (" +
                "  id INTEGER PRIMARY KEY," +
                "  username TEXT UNIQUE NOT NULL," +
                "  password TEXT NOT NULL," +
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
                "  entity_type TEXT NOT NULL," +
                "  entity_id TEXT," +
                "  payload TEXT," + // JSON representation of the data
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")"
            );

            System.out.println("SQLite schema initialized successfully.");
        }
    }

    // --- User Cache Methods ---

    public void cacheUser(User user) throws SQLException {
        String sql = "INSERT OR REPLACE INTO session_users (id, username, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPasswordHash());
            pstmt.setString(4, user.getRole().name());
            pstmt.executeUpdate();
        }
    }

    public User findCachedUserByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, password, role FROM session_users WHERE username = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password"));
                user.setRole(Role.valueOf(rs.getString("role").toUpperCase()));
                return user;
            }
        }
        return null;
    }

    // --- Sync Queue Methods ---

    public static class SyncOperation {
        public int id;
        public String operation;
        public String entityType;
        public String entityId;
        public String payload;
    }

    public void queueOperation(String operation, String entityType, String entityId, String payload) throws SQLException {
        String sql = "INSERT INTO sync_queue (operation, entity_type, entity_id, payload) VALUES (?, ?, ?, ?)";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, operation);
            pstmt.setString(2, entityType);
            pstmt.setString(3, entityId);
            pstmt.setString(4, payload);
            pstmt.executeUpdate();
        }
    }

    public List<SyncOperation> getQueuedOperations() throws SQLException {
        List<SyncOperation> operations = new ArrayList<>();
        String sql = "SELECT id, operation, entity_type, entity_id, payload FROM sync_queue ORDER BY created_at ASC";
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                SyncOperation op = new SyncOperation();
                op.id = rs.getInt("id");
                op.operation = rs.getString("operation");
                op.entityType = rs.getString("entity_type");
                op.entityId = rs.getString("entity_id");
                op.payload = rs.getString("payload");
                operations.add(op);
            }
        }
        return operations;
    }

    public void deleteQueuedOperation(int id) throws SQLException {
        String sql = "DELETE FROM sync_queue WHERE id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }
}
