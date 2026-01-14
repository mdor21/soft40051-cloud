package com.ntu.cloudgui.app.db;

import com.ntu.cloudgui.app.session.SessionState;

import java.sql.*;

public class AclRepository {

    public void grantPermission(String fileId, String username, String permission) throws SQLException {
        String granter = SessionState.getInstance().getCurrentUser() != null
            ? SessionState.getInstance().getCurrentUser().getUsername()
            : username;
        String sql = "INSERT INTO ACL (file_id, user_id, permission_level, granted_by) " +
                     "VALUES (?, (SELECT user_id FROM User_Profiles WHERE username = ?), ?, " +
                     "(SELECT user_id FROM User_Profiles WHERE username = ?)) " +
                     "ON DUPLICATE KEY UPDATE " +
                     "permission_level = VALUES(permission_level), " +
                     "granted_by = VALUES(granted_by), " +
                     "granted_at = CURRENT_TIMESTAMP";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            pstmt.setString(3, permission);
            pstmt.setString(4, granter);
            pstmt.executeUpdate();
        }
    }

    public void revokePermission(String fileId, String username) throws SQLException {
        String sql = "DELETE FROM ACL WHERE file_id = ? AND user_id = " +
                     "(SELECT user_id FROM User_Profiles WHERE username = ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, fileId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        }
    }

    // Additional methods like findPermissionsForFile, etc. would be added here.
}
