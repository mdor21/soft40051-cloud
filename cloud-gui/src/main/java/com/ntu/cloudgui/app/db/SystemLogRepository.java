package com.ntu.cloudgui.app.db;

import com.ntu.cloudgui.app.model.LogEntry;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public List<LogEntry> fetchRecent(int limit) throws SQLException {
        List<LogEntry> entries = new ArrayList<>();
        String sql = "SELECT l.timestamp, l.event_type, l.description, l.severity, " +
                     "COALESCE(u.username, 'SYSTEM') AS username " +
                     "FROM System_Logs l " +
                     "LEFT JOIN User_Profiles u ON l.user_id = u.user_id " +
                     "ORDER BY l.timestamp DESC LIMIT ?";
        try (Connection conn = MySqlConnectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry entry = new LogEntry();
                    Timestamp ts = rs.getTimestamp("timestamp");
                    if (ts != null) {
                        entry.setTimestamp(ts.toInstant());
                    } else {
                        entry.setTimestamp(Instant.now());
                    }
                    entry.setUsername(rs.getString("username"));
                    entry.setAction(rs.getString("event_type"));
                    entry.setDetails(rs.getString("description"));
                    String severity = rs.getString("severity");
                    boolean success = severity == null
                        || (!"ERROR".equalsIgnoreCase(severity) && !"CRITICAL".equalsIgnoreCase(severity));
                    entry.setSuccess(success);
                    entries.add(entry);
                }
            }
        }
        return entries;
    }
}
