package com.ntu.cloudgui.app.db;

import java.sql.*;

public class AclRepository {

    public void grantPermission(String fileId, String username, String permission) throws SQLException {
        String sql = "INSERT INTO Access_Control_Lists (file_id, username, permission_level) VALUES (?, ?, ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            pstmt.setString(3, permission);
            pstmt.executeUpdate();
        }
    }

    public void revokePermission(String fileId, String username) throws SQLException {
        String sql = "DELETE FROM Access_Control_Lists WHERE file_id = ? AND username = ?";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    // Additional methods like findPermissionsForFile, etc. would be added here.
}
