package com.ntu.cloudgui.app.db;

import java.sql.*;

public class SystemLogRepository {

    public void logEvent(String eventType, String userId, String description) throws SQLException {
        String sql = "INSERT INTO System_Logs (event_type, user_id, description) VALUES (?, ?, ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            pstmt.setString(2, userId);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
        }
    }
}
