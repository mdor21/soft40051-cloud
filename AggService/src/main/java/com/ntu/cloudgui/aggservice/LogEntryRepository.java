package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LogEntryRepository {

    private static final Logger logger = LoggerFactory.getLogger(LogEntryRepository.class);
    private final DatabaseManager databaseManager;

    public LogEntryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(LogEntry logEntry) {
        String sql = "INSERT INTO System_Logs (event_type, user_id, description, severity, service_name) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            Long userId = resolveUserId(conn, logEntry.getUsername());
            String severity = "INFO";
            if ("FAILURE".equalsIgnoreCase(logEntry.getStatus())) {
                severity = "ERROR";
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, logEntry.getEventType());
                if (userId == null) {
                    pstmt.setNull(2, java.sql.Types.BIGINT);
                } else {
                    pstmt.setLong(2, userId);
                }
                pstmt.setString(3, logEntry.getEventDescription());
                pstmt.setString(4, severity);
                pstmt.setString(5, "aggregator");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Log the error but don't throw, as a logging failure should not crash the main application flow.
            logger.error("Failed to save audit log entry", e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }

    private Long resolveUserId(Connection conn, String username) throws SQLException {
        if (username == null || username.isBlank()) {
            return null;
        }
        String sql = "SELECT user_id FROM User_Profiles WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.trim());
            try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("user_id");
                }
            }
        }
        return null;
    }
}
