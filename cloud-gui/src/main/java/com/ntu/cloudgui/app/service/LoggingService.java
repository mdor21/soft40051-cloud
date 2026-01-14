package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.model.LogEntry;
import com.ntu.cloudgui.app.db.DatabaseManager;
import com.ntu.cloudgui.app.db.SessionCacheRepository;
import com.ntu.cloudgui.app.db.SystemLogRepository;
import com.ntu.cloudgui.app.session.SessionState;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LoggingService {

    private static final LoggingService INSTANCE = new LoggingService();
    private final List<LogEntry> entries = new LinkedList<>();
    private final SessionCacheRepository sessionCacheRepository = new SessionCacheRepository();
    private final SystemLogRepository systemLogRepository = new SystemLogRepository();

    private LoggingService() {}

    public static LoggingService getInstance() {
        return INSTANCE;
    }

    public void log(String username, String action, String details, boolean success) {
        LogEntry e = new LogEntry();
        e.setUsername(username);
        e.setAction(action);
        e.setDetails(details);
        e.setSuccess(success);
        e.setTimestamp(Instant.now());
        entries.add(0, e);

        String severity = success ? "INFO" : "ERROR";
        Long userId = null;
        if (SessionState.getInstance().getCurrentUser() != null &&
            username.equals(SessionState.getInstance().getCurrentUser().getUsername())) {
            userId = SessionState.getInstance().getCurrentUser().getId();
        }

        try {
            long logId = sessionCacheRepository.logLocalEvent(action, userId, details, severity);
            if (DatabaseManager.isMysqlConnected()) {
                systemLogRepository.logEvent(action, userId, details, severity);
                if (logId > 0) {
                    sessionCacheRepository.markLogSynced(logId);
                }
            }
        } catch (Exception ex) {
            // Avoid recursive logging if logging fails.
            System.err.println("LoggingService: Failed to persist log entry: " + ex.getMessage());
        }
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
