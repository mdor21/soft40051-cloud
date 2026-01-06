package com.ntu.cloudgui.aggservice;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChunkMetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(ChunkMetadataRepository.class);

    private final DatabaseManager databaseManager;

    public ChunkMetadataRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(ChunkMetadata metadata) {
        String sql = "INSERT INTO chunk_metadata (fileId, chunkIndex, serverHost, remotePath, originalSize, encryptedSize, crc32) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, metadata.getFileId());
                stmt.setInt(2, metadata.getChunkIndex());
                stmt.setString(3, metadata.getServerHost());
                stmt.setString(4, metadata.getRemotePath());
                stmt.setLong(5, metadata.getOriginalSize());
                stmt.setLong(6, metadata.getEncryptedSize());
                stmt.setLong(7, metadata.getCrc32());
                stmt.executeUpdate();
                logger.info("Chunk metadata saved: {}[{}]", metadata.getFileId(), metadata.getChunkIndex());
            }
        } catch (SQLException e) {
            logger.error("Failed to save chunk metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public List<ChunkMetadata> findByFileIdOrderByChunkIndex(String fileId) {
        String sql = "SELECT * FROM chunk_metadata WHERE fileId = ? ORDER BY chunkIndex";
        List<ChunkMetadata> chunks = new ArrayList<>();
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        chunks.add(mapResultSetToChunkMetadata(rs));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to find chunk metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
        return chunks;
    }

    public void deleteByFileId(String fileId) {
        String sql = "DELETE FROM chunk_metadata WHERE fileId = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, fileId);
                stmt.executeUpdate();
                logger.info("Chunk metadata deleted for fileId: {}", fileId);
            }
        } catch (SQLException e) {
            logger.error("Failed to delete chunk metadata", e);
            throw new RuntimeException(e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    private ChunkMetadata mapResultSetToChunkMetadata(ResultSet rs) throws SQLException {
        return new ChunkMetadata(
                rs.getString("fileId"),
                rs.getInt("chunkIndex"),
                rs.getString("serverHost"),
                rs.getString("remotePath"),
                rs.getLong("originalSize"),
                rs.getLong("encryptedSize"),
                rs.getLong("crc32"),
                rs.getTimestamp("createdAt").toLocalDateTime()
        );
    }
}
