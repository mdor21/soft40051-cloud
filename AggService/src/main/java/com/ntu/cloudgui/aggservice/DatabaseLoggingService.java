package com.ntu.cloudgui.aggservice;

public class DatabaseLoggingService {

    private final LogEntryRepository logEntryRepository;

    public DatabaseLoggingService(LogEntryRepository logEntryRepository) {
        this.logEntryRepository = logEntryRepository;
    }

    public void logEvent(String username, String eventType, String description, LogEntry.Status status) {
        LogEntry logEntry = new LogEntry();
        logEntry.setUsername(username);
        logEntry.setEventType(eventType);
        logEntry.setEventDescription(description);
        logEntry.setStatus(status.name());
        logEntryRepository.save(logEntry);
    }
}
