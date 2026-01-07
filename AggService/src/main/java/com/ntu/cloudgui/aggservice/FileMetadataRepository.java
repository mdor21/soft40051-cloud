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
        String sql = "INSERT INTO file_metadata (username, filename, file_size, file_path, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, fileMetadata.getUsername());
                pstmt.setString(2, fileMetadata.getFilename());
                pstmt.setLong(3, fileMetadata.getFileSize());
                pstmt.setString(4, fileMetadata.getFilePath());
                pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            fileMetadata.setId(generatedKeys.getLong(1));
                        }
                    }
                }
                logger.info("Saved file metadata for filename: {}", fileMetadata.getFilename());
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public FileMetadata findById(long id) throws SQLException {
        String sql = "SELECT * FROM file_metadata WHERE id = ?";
        FileMetadata fileMetadata = null;
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        fileMetadata = new FileMetadata();
                        fileMetadata.setId(rs.getLong("id"));
                        fileMetadata.setUsername(rs.getString("username"));
                        fileMetadata.setFilename(rs.getString("filename"));
                        fileMetadata.setFileSize(rs.getLong("file_size"));
                        fileMetadata.setFilePath(rs.getString("file_path"));
                        fileMetadata.setCreatedAt(rs.getTimestamp("created_at"));
                    }
                }
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
        return fileMetadata;
    }

    public void deleteById(long id) throws SQLException {
        String sql = "DELETE FROM file_metadata WHERE id = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
                logger.info("Deleted file metadata for id: {}", id);
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }
}
