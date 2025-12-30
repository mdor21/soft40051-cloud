package com.ntu.cloudgui.app.service;

import com.ntu.cloudgui.app.model.LogEntry;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LoggingService {

    private static final LoggingService INSTANCE = new LoggingService();
    private final List<LogEntry> entries = new LinkedList<>();

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
        entries.add(0, e);
        // TODO: persist to MySQL and optionally SQLite
    }

    public List<LogEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
