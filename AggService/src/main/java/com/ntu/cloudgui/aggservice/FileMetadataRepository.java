package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.*;

public class FileMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataRepository.class);
    private final DatabaseManager databaseManager;

    public FileMetadataRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(FileMetadata fileMetadata) throws SQLException {
        String sql = "INSERT INTO File_Metadata (file_id, owner_id, original_filename, file_size, total_chunks) " +
            "VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            long ownerId = fileMetadata.getOwnerId();
            if (ownerId <= 0) {
                ownerId = resolveUserId(conn, fileMetadata.getUsername());
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileMetadata.getFileId());
                pstmt.setLong(2, ownerId);
                pstmt.setString(3, fileMetadata.getOriginalFilename());
                pstmt.setLong(4, fileMetadata.getFileSize());
                pstmt.setInt(5, fileMetadata.getTotalChunks());
                pstmt.executeUpdate();
                fileMetadata.setOwnerId(ownerId);
                logger.info("Saved file metadata for fileId: {}", fileMetadata.getFileId());
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public FileMetadata findByFileId(String fileId) throws SQLException {
        String sql = "SELECT * FROM File_Metadata WHERE file_id = ?";
        FileMetadata fileMetadata = null;
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        fileMetadata = new FileMetadata();
                        fileMetadata.setFileId(rs.getString("file_id"));
                        fileMetadata.setOwnerId(rs.getLong("owner_id"));
                        fileMetadata.setUsername(resolveUsername(conn, rs.getLong("owner_id")));
                        fileMetadata.setOriginalFilename(rs.getString("original_filename"));
                        fileMetadata.setFileSize(rs.getLong("file_size"));
                        fileMetadata.setTotalChunks(rs.getInt("total_chunks"));
                        fileMetadata.setCreatedAt(rs.getTimestamp("created_at"));
                    }
                }
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
        return fileMetadata;
    }

    public void deleteByFileId(String fileId) throws SQLException {
        String sql = "DELETE FROM File_Metadata WHERE file_id = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileId);
                pstmt.executeUpdate();
                logger.info("Deleted file metadata for fileId: {}", fileId);
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    private long resolveUserId(Connection conn, String username) throws SQLException {
        String effectiveUsername = (username == null || username.isBlank()) ? "admin" : username.trim();
        String sql = "SELECT user_id FROM User_Profiles WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, effectiveUsername);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        }
        throw new SQLException("No user_id found for username: " + effectiveUsername);
    }

    private String resolveUsername(Connection conn, long userId) throws SQLException {
        String sql = "SELECT username FROM User_Profiles WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return null;
    }
}
