package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.model.FileMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class FileMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(FileMetadataRepository.class);

    private final DatabaseManager databaseManager;

    public FileMetadataRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(FileMetadata metadata) {
        String sql = "INSERT INTO file_metadata (fileId, originalName, totalChunks, sizeBytes, encryptionAlgo) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, metadata.getFileId());
                stmt.setString(2, metadata.getOriginalName());
                stmt.setInt(3, metadata.getTotalChunks());
                stmt.setLong(4, metadata.getSizeBytes());
                stmt.setString(5, metadata.getEncryptionAlgo());
                stmt.executeUpdate();
                logger.info("File metadata saved: {}", metadata.getFileId());
            }
        } catch (SQLException e) {
            logger.error("Failed to save file metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public FileMetadata findById(String fileId) {
        String sql = "SELECT * FROM file_metadata WHERE fileId = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToFileMetadata(rs);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find file metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
        return null;
    }

    public void deleteById(String fileId) {
        String sql = "DELETE FROM file_metadata WHERE fileId = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                stmt.executeUpdate();
                logger.info("File metadata deleted: {}", fileId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete file metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    private FileMetadata mapResultSetToFileMetadata(ResultSet rs) throws SQLException {
        return new FileMetadata(
                rs.getString("fileId"),
                rs.getString("originalName"),
                rs.getInt("totalChunks"),
                rs.getLong("sizeBytes"),
                rs.getString("encryptionAlgo"),
                rs.getTimestamp("uploadTimestamp").toLocalDateTime()
        );
    }
}
