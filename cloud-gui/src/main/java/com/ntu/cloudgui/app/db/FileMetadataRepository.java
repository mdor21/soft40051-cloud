package com.ntu.cloudgui.app.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Note: A corresponding FileMetadata model class is assumed to exist.
// This class would have fields like id, name, owner, size, etc.

public class FileMetadataRepository {

    public void save(String fileId, String name, String owner, long size) throws SQLException {
        String sql = "INSERT INTO File_Metadata " +
                     "(file_id, owner_id, original_filename, file_size, total_chunks) " +
                     "VALUES (?, (SELECT user_id FROM User_Profiles WHERE username = ?), ?, ?, ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, owner);
            pstmt.setString(3, name);
            pstmt.setLong(4, size);
            pstmt.setInt(5, 1);
            pstmt.executeUpdate();
        }
    }

    public void saveOrUpdate(String fileId, String name, String owner, long size) throws SQLException {
        String sql = "INSERT INTO File_Metadata " +
                     "(file_id, owner_id, original_filename, file_size, total_chunks) " +
                     "VALUES (?, (SELECT user_id FROM User_Profiles WHERE username = ?), ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "original_filename = VALUES(original_filename), " +
                     "file_size = VALUES(file_size), " +
                     "total_chunks = VALUES(total_chunks)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, owner);
            pstmt.setString(3, name);
            pstmt.setLong(4, size);
            pstmt.setInt(5, 1);
            pstmt.executeUpdate();
        }
    }

    public void delete(String fileId) throws SQLException {
        String sql = "DELETE FROM File_Metadata WHERE file_id = ?";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.executeUpdate();
        }
    }

    public FileRecord findById(String fileId) throws SQLException {
        String sql = "SELECT fm.file_id, fm.owner_id, up.username AS owner_username, " +
                     "fm.original_filename, fm.file_size, fm.total_chunks, fm.created_at " +
                     "FROM File_Metadata fm " +
                     "JOIN User_Profiles up ON fm.owner_id = up.user_id " +
                     "WHERE fm.file_id = ?";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRecord(rs);
                }
            }
        }
        return null;
    }

    public String findFileIdByNameAndOwner(String filename, String ownerUsername) throws SQLException {
        String sql = "SELECT fm.file_id " +
                     "FROM File_Metadata fm " +
                     "WHERE fm.original_filename = ? " +
                     "AND fm.owner_id = (SELECT user_id FROM User_Profiles WHERE username = ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, filename);
            pstmt.setString(2, ownerUsername);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_id");
                }
            }
        }
        return null;
    }

    public List<FileRecord> findAccessibleFiles(String username) throws SQLException {
        return findAccessibleFilesSince(username, 0L);
    }

    public List<FileRecord> findAccessibleFilesSince(String username, long sinceMs) throws SQLException {
        List<FileRecord> files = new ArrayList<>();
        String sql = "SELECT fm.file_id, fm.owner_id, up.username AS owner_username, " +
                     "fm.original_filename, fm.file_size, fm.total_chunks, fm.created_at, " +
                     "a.granted_at " +
                     "FROM File_Metadata fm " +
                     "JOIN User_Profiles up ON fm.owner_id = up.user_id " +
                     "LEFT JOIN ACL a ON a.file_id = fm.file_id " +
                     "AND a.user_id = (SELECT user_id FROM User_Profiles WHERE username = ?) " +
                     "WHERE (fm.owner_id = (SELECT user_id FROM User_Profiles WHERE username = ?) " +
                     "OR a.user_id IS NOT NULL) " +
                     "AND (fm.created_at > ? OR a.granted_at > ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Timestamp since = new Timestamp(sinceMs);
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setTimestamp(3, since);
            pstmt.setTimestamp(4, since);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    files.add(mapRecord(rs));
                }
            }
        }
        return files;
    }

    private FileRecord mapRecord(ResultSet rs) throws SQLException {
        FileRecord record = new FileRecord();
        record.fileId = rs.getString("file_id");
        record.ownerId = rs.getLong("owner_id");
        record.ownerUsername = rs.getString("owner_username");
        record.filename = rs.getString("original_filename");
        record.fileSize = rs.getLong("file_size");
        record.totalChunks = rs.getInt("total_chunks");
        Timestamp createdAt = rs.getTimestamp("created_at");
        record.createdAt = createdAt == null ? 0L : createdAt.getTime();
        Timestamp grantedAt = null;
        try {
            grantedAt = rs.getTimestamp("granted_at");
        } catch (SQLException ignored) {
        }
        record.aclGrantedAt = grantedAt == null ? 0L : grantedAt.getTime();
        return record;
    }

    public static class FileRecord {
        public String fileId;
        public long ownerId;
        public String ownerUsername;
        public String filename;
        public long fileSize;
        public int totalChunks;
        public long createdAt;
        public long aclGrantedAt;

        public long getLastChangeTimestamp() {
            return Math.max(createdAt, aclGrantedAt);
        }
    }
}
