package com.ntu.cloudgui.aggservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class LogEntryRepository {

    private static final Logger logger = LoggerFactory.getLogger(LogEntryRepository.class);
    private final DatabaseManager databaseManager;

    public LogEntryRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(LogEntry logEntry) {
        String sql = "INSERT INTO audit_logs (username, event_type, event_description, status, created_at) VALUES (?, ?, ?, ?, ?)";
        Connection conn = null;
        try {
            conn = databaseManager.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, logEntry.getUsername());
                pstmt.setString(2, logEntry.getEventType());
                pstmt.setString(3, logEntry.getEventDescription());
                pstmt.setString(4, logEntry.getStatus());
                pstmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            // Log the error but don't throw, as a logging failure should not crash the main application flow.
            logger.error("Failed to save audit log entry", e);
        } finally {
            databaseManager.releaseConnection(conn);
        }
    }
}
