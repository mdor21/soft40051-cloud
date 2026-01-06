package com.ntu.cloudgui.aggservice.repository;

import com.ntu.cloudgui.aggservice.model.FileMetadata;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class FileMetadataDao {

    private final Connection connection;

    public FileMetadataDao(Connection connection) {
        this.connection = connection;
    }

    public void save(FileMetadata fileMetadata) throws SQLException {
        String sql = "INSERT INTO file_metadata (file_id, original_name, total_chunks, size_bytes, encryption_algo, uploaded_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileMetadata.getFileId());
            statement.setString(2, fileMetadata.getOriginalName());
            statement.setInt(3, fileMetadata.getTotalChunks());
            statement.setLong(4, fileMetadata.getSizeBytes());
            statement.setString(5, fileMetadata.getEncryptionAlgo());
            statement.setTimestamp(6, java.sql.Timestamp.valueOf(fileMetadata.getUploadedAt()));
            statement.executeUpdate();
        }
    }

    public Optional<FileMetadata> findById(String fileId) throws SQLException {
        String sql = "SELECT * FROM file_metadata WHERE file_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new FileMetadata(
                            resultSet.getString("file_id"),
                            resultSet.getString("original_name"),
                            resultSet.getInt("total_chunks"),
                            resultSet.getLong("size_bytes"),
                            resultSet.getString("encryption_algo"),
                            resultSet.getTimestamp("uploaded_at").toLocalDateTime()
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public void deleteById(String fileId) throws SQLException {
        String sql = "DELETE FROM file_metadata WHERE file_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, fileId);
            statement.executeUpdate();
        }
    }
}
