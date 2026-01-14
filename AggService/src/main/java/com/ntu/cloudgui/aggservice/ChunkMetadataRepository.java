package com.ntu.cloudgui.aggservice;

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

    public void save(ChunkMetadata chunkMetadata) throws SQLException {
        String sql = "INSERT INTO Chunk_Metadata (file_id, chunk_sequence, crc32_checksum, server_location, chunk_size, storage_path) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, chunkMetadata.getFileId());
                pstmt.setInt(2, chunkMetadata.getChunkIndex());
                pstmt.setString(3, formatCrc32(chunkMetadata.getCrc32()));
                pstmt.setString(4, chunkMetadata.getFileServerName());
                pstmt.setLong(5, chunkMetadata.getChunkSize());
                pstmt.setString(6, chunkMetadata.getChunkPath());
                pstmt.executeUpdate();
                logger.debug("Saved chunk metadata for fileId: {}, chunkIndex: {}", chunkMetadata.getFileId(), chunkMetadata.getChunkIndex());
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public List<ChunkMetadata> findByFileIdOrderByChunkIndexAsc(String fileId) throws SQLException {
        List<ChunkMetadata> chunks = new ArrayList<>();
        String sql = "SELECT * FROM Chunk_Metadata WHERE file_id = ? ORDER BY chunk_sequence ASC";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        ChunkMetadata chunk = new ChunkMetadata();
                        chunk.setId(rs.getLong("chunk_id"));
                        chunk.setFileId(rs.getString("file_id"));
                        chunk.setChunkIndex(rs.getInt("chunk_sequence"));
                        chunk.setCrc32(parseCrc32(rs.getString("crc32_checksum")));
                        chunk.setFileServerName(rs.getString("server_location"));
                        chunk.setChunkPath(rs.getString("storage_path"));
                        chunk.setChunkSize(rs.getLong("chunk_size"));
                        chunks.add(chunk);
                    }
                }
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
        return chunks;
    }

    public void deleteByFileId(String fileId) throws SQLException {
        String sql = "DELETE FROM Chunk_Metadata WHERE file_id = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, fileId);
                int affectedRows = pstmt.executeUpdate();
                logger.info("Deleted {} chunk metadata records for fileId: {}", affectedRows, fileId);
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    private String formatCrc32(long crc32) {
        return String.format("%08x", crc32);
    }

    private long parseCrc32(String crc32Hex) {
        if (crc32Hex == null || crc32Hex.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseUnsignedLong(crc32Hex.trim(), 16);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
