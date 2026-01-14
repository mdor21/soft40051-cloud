package com.ntu.cloudgui.app.db;

import java.sql.*;

public class SystemLogRepository {

    public void logEvent(String eventType, String userId, String description) throws SQLException {
        logEvent(eventType, userId == null ? null : Long.valueOf(userId), description, "INFO");
    }

    public void logEvent(String eventType, Long userId, String description, String severity) throws SQLException {
        String sql = "INSERT INTO System_Logs (event_type, user_id, description, severity) VALUES (?, ?, ?, ?)";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            if (userId == null) {
                pstmt.setNull(2, Types.INTEGER);
            } else {
                pstmt.setLong(2, userId);
            }
            pstmt.setString(3, description);
            pstmt.setString(4, severity);
            pstmt.executeUpdate();
        }
    }
}
