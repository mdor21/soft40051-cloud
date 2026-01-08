package com.ntu.cloudgui.app.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// Note: A corresponding FileMetadata model class is assumed to exist.
// This class would have fields like id, name, owner, size, etc.

public class FileMetadataRepository {

    public void save(String fileId, String name, String owner, long size) throws SQLException {
        String sql = "INSERT INTO File_Metadata (file_id, file_name, owner_username, size_bytes) VALUES (?, ?, ?, ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, name);
            pstmt.setString(3, owner);
            pstmt.setLong(4, size);
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

    // Additional methods like findById, findAllByOwner, etc. would be added here.
}
