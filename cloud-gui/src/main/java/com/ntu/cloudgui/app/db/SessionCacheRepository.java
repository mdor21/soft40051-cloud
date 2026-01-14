package com.ntu.cloudgui.app.db;

import com.ntu.cloudgui.app.model.FileMetadata;
import com.ntu.cloudgui.app.model.Role;
import com.ntu.cloudgui.app.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Local SQLite repository for offline cache and pending operations.
 */
public class SessionCacheRepository {

    public static final String OP_FILE_CREATE = "FILE_CREATE";
    public static final String OP_FILE_UPDATE = "FILE_UPDATE";
    public static final String OP_FILE_DELETE = "FILE_DELETE";
    public static final String OP_ACL_GRANT = "ACL_GRANT";
    public static final String OP_ACL_REVOKE = "ACL_REVOKE";
    public static final String OP_USER_CREATE = "CREATE_USER";
    public static final String OP_USER_UPDATE = "UPDATE_USER";
    public static final String OP_USER_DELETE = "DELETE_USER";

    /**
     * Initializes the SQLite database schema. Creates tables if they don't exist.
     * @throws SQLException if schema initialization fails
     */
    public void initSchema() throws SQLException {
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS session_users");
            stmt.execute("DROP TABLE IF EXISTS file_cache");
            stmt.execute("DROP TABLE IF EXISTS sync_queue");

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Session_Cache (" +
                "  session_id TEXT PRIMARY KEY," +
                "  user_id INTEGER NOT NULL," +
                "  username TEXT NOT NULL," +
                "  role TEXT NOT NULL," +
                "  login_time INTEGER NOT NULL," +
                "  last_sync INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Local_File_Metadata (" +
                "  file_id TEXT PRIMARY KEY," +
                "  owner_id INTEGER NOT NULL," +
                "  original_filename TEXT NOT NULL," +
                "  file_size INTEGER NOT NULL," +
                "  total_chunks INTEGER NOT NULL," +
                "  sync_status TEXT DEFAULT 'pending'," +
                "  last_modified INTEGER NOT NULL," +
                "  last_sync INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Pending_Operations (" +
                "  operation_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  operation_type TEXT NOT NULL," +
                "  file_id TEXT," +
                "  payload TEXT," +
                "  created_at INTEGER NOT NULL," +
                "  retry_count INTEGER DEFAULT 0" +
                ")"
            );

            stmt.execute(
                "CREATE TABLE IF NOT EXISTS Local_Logs (" +
                "  log_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  timestamp INTEGER NOT NULL," +
                "  event_type TEXT NOT NULL," +
                "  user_id INTEGER," +
                "  description TEXT NOT NULL," +
                "  severity TEXT DEFAULT 'INFO'," +
                "  synced INTEGER DEFAULT 0" +
                ")"
            );
        }
    }

    // --- Session Cache Methods ---

    public void cacheUser(User user) throws SQLException {
        String sessionId = user.getPasswordHash();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = user.getUsername();
        }
        long now = System.currentTimeMillis();
        String sql = "INSERT OR REPLACE INTO Session_Cache " +
                     "(session_id, user_id, username, role, login_time, last_sync) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setLong(2, user.getId() != null ? user.getId() : 0L);
            pstmt.setString(3, user.getUsername());
            pstmt.setString(4, user.getRole().name());
            pstmt.setLong(5, now);
            pstmt.setLong(6, now);
            pstmt.executeUpdate();
        }
    }

    public User findCachedUserByUsername(String username) throws SQLException {
        String sql = "SELECT session_id, user_id, username, role, login_time, last_sync " +
                     "FROM Session_Cache WHERE username = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("user_id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("session_id"));
                user.setRole(Role.valueOf(rs.getString("role").toUpperCase()));
                user.setLastModified(new Timestamp(rs.getLong("last_sync")));
                return user;
            }
        }
        return null;
    }

    // --- Local File Metadata Methods ---

    public void upsertLocalFileMetadata(FileMetadata metadata, long ownerId, String syncStatus) throws SQLException {
        upsertLocalFileMetadata(
            metadata.getId(),
            ownerId,
            metadata.getName(),
            metadata.getSizeBytes(),
            1,
            syncStatus,
            System.currentTimeMillis(),
            0L
        );
    }

    public void upsertLocalFileMetadata(String fileId, long ownerId, String filename, long size, int totalChunks,
                                        String syncStatus, long lastModified, long lastSync) throws SQLException {
        String sql = "INSERT INTO Local_File_Metadata " +
                     "(file_id, owner_id, original_filename, file_size, total_chunks, sync_status, last_modified, last_sync) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT(file_id) DO UPDATE SET " +
                     "owner_id = excluded.owner_id, " +
                     "original_filename = excluded.original_filename, " +
                     "file_size = excluded.file_size, " +
                     "total_chunks = excluded.total_chunks, " +
                     "sync_status = excluded.sync_status, " +
                     "last_modified = excluded.last_modified, " +
                     "last_sync = excluded.last_sync";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setLong(2, ownerId);
            pstmt.setString(3, filename);
            pstmt.setLong(4, size);
            pstmt.setInt(5, totalChunks);
            pstmt.setString(6, syncStatus);
            pstmt.setLong(7, lastModified);
            pstmt.setLong(8, lastSync);
            pstmt.executeUpdate();
        }
    }

    public LocalFileRecord findLocalFile(String fileId) throws SQLException {
        String sql = "SELECT file_id, owner_id, original_filename, file_size, total_chunks, sync_status, " +
                     "last_modified, last_sync FROM Local_File_Metadata WHERE file_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                LocalFileRecord record = new LocalFileRecord();
                record.fileId = rs.getString("file_id");
                record.ownerId = rs.getLong("owner_id");
                record.originalFilename = rs.getString("original_filename");
                record.fileSize = rs.getLong("file_size");
                record.totalChunks = rs.getInt("total_chunks");
                record.syncStatus = rs.getString("sync_status");
                record.lastModified = rs.getLong("last_modified");
                record.lastSync = rs.getLong("last_sync");
                return record;
            }
        }
        return null;
    }

    public LocalFileRecord findLocalFileByName(String filename, Long ownerId) throws SQLException {
        String sql = "SELECT file_id, owner_id, original_filename, file_size, total_chunks, sync_status, " +
                     "last_modified, last_sync FROM Local_File_Metadata WHERE original_filename = ?";
        if (ownerId != null) {
            sql += " AND owner_id = ?";
        }
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filename);
            if (ownerId != null) {
                pstmt.setLong(2, ownerId);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                LocalFileRecord record = new LocalFileRecord();
                record.fileId = rs.getString("file_id");
                record.ownerId = rs.getLong("owner_id");
                record.originalFilename = rs.getString("original_filename");
                record.fileSize = rs.getLong("file_size");
                record.totalChunks = rs.getInt("total_chunks");
                record.syncStatus = rs.getString("sync_status");
                record.lastModified = rs.getLong("last_modified");
                record.lastSync = rs.getLong("last_sync");
                return record;
            }
        }
        return null;
    }

    public void markLocalFileSynced(String fileId, long lastSync) throws SQLException {
        String sql = "UPDATE Local_File_Metadata SET sync_status = 'synced', last_sync = ? WHERE file_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, lastSync);
            pstmt.setString(2, fileId);
            pstmt.executeUpdate();
        }
    }

    public void deleteLocalFile(String fileId) throws SQLException {
        String sql = "DELETE FROM Local_File_Metadata WHERE file_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.executeUpdate();
        }
    }

    public long getLatestFileSyncTimestamp() throws SQLException {
        String sql = "SELECT MAX(last_sync) AS max_sync FROM Local_File_Metadata";
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("max_sync");
            }
        }
        return 0L;
    }

    // --- Pending Operations ---

    public void queueOperation(String operationType, String fileId, String payload) throws SQLException {
        String sql = "INSERT INTO Pending_Operations " +
                     "(operation_type, file_id, payload, created_at, retry_count) VALUES (?, ?, ?, ?, 0)";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, operationType);
            pstmt.setString(2, fileId);
            pstmt.setString(3, payload);
            pstmt.setLong(4, System.currentTimeMillis());
            pstmt.executeUpdate();
        }
    }

    public List<PendingOperation> getPendingOperations() throws SQLException {
        List<PendingOperation> operations = new ArrayList<>();
        String sql = "SELECT operation_id, operation_type, file_id, payload, created_at, retry_count " +
                     "FROM Pending_Operations ORDER BY created_at ASC";
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                PendingOperation op = new PendingOperation();
                op.id = rs.getLong("operation_id");
                op.operationType = rs.getString("operation_type");
                op.fileId = rs.getString("file_id");
                op.payload = rs.getString("payload");
                op.createdAt = rs.getLong("created_at");
                op.retryCount = rs.getInt("retry_count");
                operations.add(op);
            }
        }
        return operations;
    }

    public void deletePendingOperation(long id) throws SQLException {
        String sql = "DELETE FROM Pending_Operations WHERE operation_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public void deletePendingOperationsForFile(String fileId) throws SQLException {
        String sql = "DELETE FROM Pending_Operations WHERE file_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.executeUpdate();
        }
    }

    public void incrementRetryCount(long id, int retryCount) throws SQLException {
        String sql = "UPDATE Pending_Operations SET retry_count = ? WHERE operation_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, retryCount + 1);
            pstmt.setLong(2, id);
            pstmt.executeUpdate();
        }
    }

    // --- Local Logs ---

    public long logLocalEvent(String eventType, Long userId, String description, String severity) throws SQLException {
        String sql = "INSERT INTO Local_Logs (timestamp, event_type, user_id, description, severity, synced) " +
                     "VALUES (?, ?, ?, ?, ?, 0)";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, eventType);
            if (userId == null) {
                pstmt.setNull(3, Types.INTEGER);
            } else {
                pstmt.setLong(3, userId);
            }
            pstmt.setString(4, description);
            pstmt.setString(5, severity);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1L;
    }

    public List<LocalLogEntry> getUnsyncedLogs() throws SQLException {
        List<LocalLogEntry> logs = new ArrayList<>();
        String sql = "SELECT log_id, timestamp, event_type, user_id, description, severity " +
                     "FROM Local_Logs WHERE synced = 0 ORDER BY timestamp ASC";
        try (Connection conn = SqliteConnectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                LocalLogEntry entry = new LocalLogEntry();
                entry.id = rs.getLong("log_id");
                entry.timestamp = rs.getLong("timestamp");
                entry.eventType = rs.getString("event_type");
                entry.userId = rs.getLong("user_id");
                if (rs.wasNull()) {
                    entry.userId = null;
                }
                entry.description = rs.getString("description");
                entry.severity = rs.getString("severity");
                logs.add(entry);
            }
        }
        return logs;
    }

    public void markLogSynced(long id) throws SQLException {
        String sql = "UPDATE Local_Logs SET synced = 1 WHERE log_id = ?";
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        }
    }

    public List<LocalFileRecord> listLocalFiles(Long ownerId) throws SQLException {
        List<LocalFileRecord> files = new ArrayList<>();
        String sql = "SELECT file_id, owner_id, original_filename, file_size, total_chunks, sync_status, " +
                     "last_modified, last_sync FROM Local_File_Metadata";
        if (ownerId != null) {
            sql += " WHERE owner_id = ?";
        }
        try (Connection conn = SqliteConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (ownerId != null) {
                pstmt.setLong(1, ownerId);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                LocalFileRecord record = new LocalFileRecord();
                record.fileId = rs.getString("file_id");
                record.ownerId = rs.getLong("owner_id");
                record.originalFilename = rs.getString("original_filename");
                record.fileSize = rs.getLong("file_size");
                record.totalChunks = rs.getInt("total_chunks");
                record.syncStatus = rs.getString("sync_status");
                record.lastModified = rs.getLong("last_modified");
                record.lastSync = rs.getLong("last_sync");
                files.add(record);
            }
        }
        return files;
    }

    public static class PendingOperation {
        public long id;
        public String operationType;
        public String fileId;
        public String payload;
        public long createdAt;
        public int retryCount;
    }

    public static class LocalLogEntry {
        public long id;
        public long timestamp;
        public String eventType;
        public Long userId;
        public String description;
        public String severity;
    }

    public static class LocalFileRecord {
        public String fileId;
        public long ownerId;
        public String originalFilename;
        public long fileSize;
        public int totalChunks;
        public String syncStatus;
        public long lastModified;
        public long lastSync;
    }
}
