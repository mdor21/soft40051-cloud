package com.ntu.cloudgui.aggservice.service;

import com.ntu.cloudgui.aggservice.model.LogEntry;
import com.ntu.cloudgui.aggservice.repository.LogEntryRepository;
import org.springframework.stereotype.Service;

@Service
public class DatabaseLoggingService {

    private final LogEntryRepository logEntryRepository;
    private static final String SERVICE_NAME = "Aggregator";

    public DatabaseLoggingService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public void log(String logLevel, String message) {
        try {
            LogEntry logEntry = new LogEntry(SERVICE_NAME, logLevel, message);
            logEntryRepository.save(logEntry);
        } catch (Exception e) {
            // Log to console if DB logging fails to avoid a loop
            System.err.println("Failed to write log to database: " + e.getMessage());
        }
    }

    public void info(String message) {
        log("INFO", message);
    }

    public void error(String message) {
        log("ERROR", message);
    }

    public void warn(String message) {
        log("WARN", message);
    }
}
