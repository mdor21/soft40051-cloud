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
        String sql = "INSERT INTO chunk_metadata (file_id, chunk_index, crc32, file_server_name, chunk_path, chunk_size, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, chunkMetadata.getFileId());
                pstmt.setInt(2, chunkMetadata.getChunkIndex());
                pstmt.setLong(3, chunkMetadata.getCrc32());
                pstmt.setString(4, chunkMetadata.getFileServerName());
                pstmt.setString(5, chunkMetadata.getChunkPath());
                pstmt.setLong(6, chunkMetadata.getChunkSize());
                pstmt.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                pstmt.executeUpdate();
                logger.debug("Saved chunk metadata for fileId: {}, chunkIndex: {}", chunkMetadata.getFileId(), chunkMetadata.getChunkIndex());
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    public List<ChunkMetadata> findByFileIdOrderByChunkIndexAsc(long fileId) throws SQLException {
        List<ChunkMetadata> chunks = new ArrayList<>();
        String sql = "SELECT * FROM chunk_metadata WHERE file_id = ? ORDER BY chunk_index ASC";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, fileId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        ChunkMetadata chunk = new ChunkMetadata();
                        chunk.setId(rs.getLong("id"));
                        chunk.setFileId(rs.getLong("file_id"));
                        chunk.setChunkIndex(rs.getInt("chunk_index"));
                        chunk.setCrc32(rs.getLong("crc32"));
                        chunk.setFileServerName(rs.getString("file_server_name"));
                        chunk.setChunkPath(rs.getString("chunk_path"));
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

    public void deleteByFileId(long fileId) throws SQLException {
        String sql = "DELETE FROM chunk_metadata WHERE file_id = ?";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, fileId);
                int affectedRows = pstmt.executeUpdate();
                logger.info("Deleted {} chunk metadata records for fileId: {}", affectedRows, fileId);
            }
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }
}
