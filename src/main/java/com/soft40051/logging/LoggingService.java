package com.soft40051.logging;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggingService {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Connection mysqlConnection;
    private Connection sqliteConnection;
    
    public LoggingService(Connection mysqlConnection, Connection sqliteConnection) {
        this.mysqlConnection = mysqlConnection;
        this.sqliteConnection = sqliteConnection;
    }
    
    // Log Levels
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
    
    /**
     * Log to both MySQL (async) and SQLite (local)
     */
    public void log(String service, LogLevel level, String username, String action, String details) {
        logToSQLite(service, level, username, action, details, null);
        logToMySQLAsync(service, level, username, action, details, null);
    }
    
    /**
     * Log exception with stack trace
     */
    public void logException(String service, String username, String action, Exception ex) {
        String stackTrace = getStackTraceAsString(ex);
        logToSQLite(service, LogLevel.ERROR, username, action, ex.getMessage(), stackTrace);
        logToMySQLAsync(service, LogLevel.ERROR, username, action, ex.getMessage(), stackTrace);
    }
    
    /**
     * Log User Management Events (GUI)
     */
    public void logUserCreated(String username) {
        log("JavaFX-GUI", LogLevel.INFO, username, "ACCOUNT_CREATED", 
            "New user account created: " + username);
    }
    
    public void logLoginSuccessful(String username) {
        log("JavaFX-GUI", LogLevel.INFO, username, "LOGIN_SUCCESSFUL", 
            "User logged in successfully");
    }
    
    public void logLoginFailed(String username, String reason) {
        log("JavaFX-GUI", LogLevel.WARN, username, "LOGIN_FAILED", reason);
    }
    
    public void logPasswordUpdated(String username) {
        log("JavaFX-GUI", LogLevel.INFO, username, "PASSWORD_UPDATED", 
            "Password changed successfully");
    }
    
    public void logLogout(String username) {
        log("JavaFX-GUI", LogLevel.INFO, username, "LOGOUT", 
            "User logged out");
    }
    
    /**
     * Log Access Control Events (GUI)
     */
    public void logAccessDenied(String username, String filename, String permission) {
        log("JavaFX-GUI", LogLevel.WARN, username, "ACCESS_DENIED", 
            "Attempted to access file '" + filename + "' without " + permission + " permission");
    }
    
    /**
     * Log Terminal Actions (GUI)
     */
    public void logLocalCommandExecution(String username, String command, int exitCode) {
        log("JavaFX-GUI", LogLevel.INFO, username, "LOCAL_COMMAND_EXEC", 
            "Command: " + command + " | Exit Code: " + exitCode);
    }
    
    public void logRemoteCommandExecution(String username, String command, String server, int exitCode) {
        log("JavaFX-GUI", LogLevel.INFO, username, "REMOTE_COMMAND_EXEC", 
            "Server: " + server + " | Command: " + command + " | Exit Code: " + exitCode);
    }
    
    /**
     * Log Load Balancer Events
     */
    public void logSchedulingDecision(String algorithm, String selectedServer, String requestId) {
        log("LoadBalancer", LogLevel.INFO, null, "SCHEDULING_DECISION", 
            "Algorithm: " + algorithm + " | Selected Server: " + selectedServer + " | Request ID: " + requestId);
    }
    
    public void logLatencySimulation(String requestId, long delayMs) {
        log("LoadBalancer", LogLevel.DEBUG, null, "LATENCY_SIMULATION", 
            "Request ID: " + requestId + " | Delay Applied: " + delayMs + "ms");
    }
    
    public void logHealthCheckResult(String serverId, String status) {
        log("LoadBalancer", LogLevel.INFO, null, "HEALTH_CHECK", 
            "Server ID: " + serverId + " | Status: " + status);
    }
    
    /**
     * Log File Aggregator Events
     */
    public void logEncryptionStarted(String fileId, String filename) {
        log("FileAggregator", LogLevel.INFO, null, "ENCRYPTION_STARTED", 
            "File ID: " + fileId + " | Filename: " + filename);
    }
    
    public void logChunking(String fileId, int chunkCount) {
        log("FileAggregator", LogLevel.INFO, null, "FILE_PARTITIONED", 
            "File ID: " + fileId + " | Chunks: " + chunkCount);
    }
    
    public void logDecryptionSuccessful(String fileId) {
        log("FileAggregator", LogLevel.INFO, null, "DECRYPTION_SUCCESSFUL", 
            "File ID: " + fileId + " | File reconstructed and decrypted");
    }
    
    public void logCRC32Validation(String chunkId, String checksum, boolean valid) {
        LogLevel level = valid ? LogLevel.DEBUG : LogLevel.ERROR;
        log("FileAggregator", level, null, "CRC32_VALIDATION", 
            "Chunk ID: " + chunkId + " | Checksum: " + checksum + " | Valid: " + valid);
    }
    
    /**
     * Log Host Manager Events
     */
    public void logMQTTMessageReceived(String topic, String payload) {
        log("HostManager", LogLevel.INFO, null, "MQTT_MESSAGE_RECEIVED", 
            "Topic: " + topic + " | Payload: " + payload);
    }
    
    public void logDockerOperation(String operation, String containerId, String command) {
        log("HostManager", LogLevel.INFO, null, "DOCKER_OPERATION", 
            "Operation: " + operation + " | Container: " + containerId + " | Command: " + command);
    }
    
    /**
     * Log Concurrency Control Events
     */
    public void logSemaphoreLocked(String serverId, String fileId) {
        log("Concurrency", LogLevel.DEBUG, null, "SEMAPHORE_LOCKED", 
            "Server: " + serverId + " | File: " + fileId);
    }
    
    public void logSemaphoreReleased(String serverId, String fileId) {
        log("Concurrency", LogLevel.DEBUG, null, "SEMAPHORE_RELEASED", 
            "Server: " + serverId + " | File: " + fileId);
    }
    
    /**
     * Log Synchronisation Events
     */
    public void logSyncStarted() {
        log("Sync", LogLevel.INFO, null, "SYNC_STARTED", 
            "SQLite to MySQL synchronisation initiated");
    }
    
    public void logSyncConflictResolved(String entityType, String entityId, String resolution) {
        log("Sync", LogLevel.INFO, null, "CONFLICT_RESOLVED", 
            "Entity Type: " + entityType + " | Entity ID: " + entityId + " | Resolution: " + resolution);
    }
    
    public void logSyncCompleted(int recordsSynced) {
        log("Sync", LogLevel.INFO, null, "SYNC_COMPLETED", 
            "Total records synced: " + recordsSynced);
    }
    
    // ============ PRIVATE HELPER METHODS ============
    
    private void logToSQLite(String service, LogLevel level, String username, String action, String details, String stackTrace) {
        try {
            String sql = "INSERT INTO local_logs (service, log_level, message, stack_trace) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = sqliteConnection.prepareStatement(sql)) {
                stmt.setString(1, service);
                stmt.setString(2, level.name());
                stmt.setString(3, formatLogMessage(username, action, details));
                stmt.setString(4, stackTrace);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error logging to SQLite: " + e.getMessage());
        }
    }
    
    private void logToMySQLAsync(String service, LogLevel level, String username, String action, String details, String stackTrace) {
       