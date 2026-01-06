package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.ChunkMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ChunkMetadataDao {

    private final Connection connection;

    public ChunkMetadataDao(Connection connection) {
        this.connection = connection;
    }

    public void save(ChunkMetadata chunkMetadata) throws SQLException {
        String sql = "INSERT INTO chunk_metadata (chunk_id, file_id, chunk_index, server_host, remote_path, original_size, encrypted_size, crc32, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, chunkMetadata.getChunkId());
            statement.setString(2, chunkMetadata.getFileId());
            statement.setInt(3, chunkMetadata.getChunkIndex());
            statement.setString(4, chunkMetadata.getServerHost());
            statement.setString(5, chunkMetadata.getRemotePath());
            statement.setLong(6, chunkMetadata.getOriginalSize());
            statement.setLong(7, chunkMetadata.getEncryptedSize());
            statement.setLong(8, chunkMetadata.getCrc32());
            statement.setTimestamp(9, java.sql.Timestamp.valueOf(chunkMetadata.getCreatedAt()));
            statement.executeUpdate();
        }
    }

    public List<ChunkMetadata> findByFileIdOrderByChunkIndex(String fileId) throws SQLException {
        List<ChunkMetadata> chunks = new ArrayList<>();
        String sql = "SELECT * FROM chunk_metadata WHERE file_id = ? ORDER BY chunk_index";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    chunks.add(new ChunkMetadata(
                            resultSet.getString("chunk_id"),
                            resultSet.getString("file_id"),
                            resultSet.getInt("chunk_index"),
                            resultSet.getString("server_host"),
                            resultSet.getString("remote_path"),
                            resultSet.getLong("original_size"),
                            resultSet.getLong("encrypted_size"),
                            resultSet.getLong("crc32"),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return chunks;
    }

    public void deleteByFileId(String fileId) throws SQLException {
        String sql = "DELETE FROM chunk_metadata WHERE file_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileId);
            statement.executeUpdate();
        }
    }
}
