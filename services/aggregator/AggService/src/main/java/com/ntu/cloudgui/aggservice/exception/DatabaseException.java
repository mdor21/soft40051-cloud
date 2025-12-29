package com.ntu.cloudgui.aggservice.exception;

/**
 * DatabaseException - Database Operation Error
 * 
 * Thrown when database operations fail.
 * Covers connection, query, and data integrity errors.
 * 
 * Used for:
 * - Connection pool exhaustion
 * - SQL query failures
 * - Data integrity violations
 * - Transaction failures
 * - Schema initialization errors
 * 
 * Example:
 * <pre>
 * try {
 *     fileMetadataRepository.save(metadata);
 * } catch (DatabaseException e) {
 *     logger.error("Database error: {}", e.getMessage());
 *     // Handle error...
 * }
 * </pre>
 */
public class DatabaseException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Database error types
     */
    public enum ErrorType {
        CONNECTION_FAILED("Failed to acquire database connection"),
        CONNECTION_TIMEOUT("Database connection timeout"),
        QUERY_FAILED("Database query failed"),
        UPDATE_FAILED("Database update failed"),
        DELETE_FAILED("Database delete failed"),
        TRANSACTION_FAILED("Database transaction failed"),
        CONSTRAINT_VIOLATION("Database constraint violation"),
        DUPLICATE_KEY("Duplicate key error"),
        NOT_FOUND("Record not found in database"),
        SCHEMA_ERROR("Database schema error"),
        POOL_EXHAUSTED("Connection pool exhausted"),
        UNKNOWN("Unknown database error");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String tableName;
    private final String sqlState;
    
    /**
     * Constructor - with message only
     * 
     * @param message Error description
     */
    public DatabaseException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.tableName = null;
        this.sqlState = null;
    }
    
    /**
     * Constructor - with message and cause
     * 
     * @param message Error description
     * @param cause Root cause exception (usually SQLException)
     */
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
        this.tableName = null;
        this.sqlState = extractSqlState(cause);
    }
    
    /**
     * Constructor - with error type and table name
     * 
     * @param errorType Type of database error
     * @param tableName Table where error occurred
     */
    public DatabaseException(ErrorType errorType, String tableName) {
        super(errorType.getDescription() + " (table: " + tableName + ")");
        this.errorType = errorType;
        this.tableName = tableName;
        this.sqlState = null;
    }
    
    /**
     * Constructor - with error type, table, and cause
     * 
     * @param errorType Type of database error
     * @param tableName Table where error occurred
     * @param cause Root cause exception
     */
    public DatabaseException(ErrorType errorType, String tableName, Throwable cause) {
        super(errorType.getDescription() + " (table: " + tableName + ")", cause);
        this.errorType = errorType;
        this.tableName = tableName;
        this.sqlState = extractSqlState(cause);
    }
    
    /**
     * Get error type
     * 
     * @return ErrorType enum value
     */
    public ErrorType getErrorType() {
        return errorType;
    }
    
    /**
     * Get affected table name
     * 
     * @return Table name where error occurred
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Get SQL state code
     * 
     * SQL state is a standardized error code from JDBC.
     * Useful for debugging specific database errors.
     * 
     * @return SQL state code or null
     */
    public String getSqlState() {
        return sqlState;
    }
    
    /**
     * Check if error is retryable
     * 
     * Some errors (like connection timeout) can be retried.
     * Others (like constraint violations) cannot be retried.
     * 
     * @return true if operation can be retried
     */
    public boolean isRetryable() {
        return errorType == ErrorType.CONNECTION_FAILED ||
               errorType == ErrorType.CONNECTION_TIMEOUT ||
               errorType == ErrorType.POOL_EXHAUSTED ||
               errorType == ErrorType.TRANSACTION_FAILED;
    }
    
    /**
     * Check if error is due to duplicate key
     * 
     * Useful for distinguishing insert failures.
     * 
     * @return true if duplicate key violation
     */
    public boolean isDuplicateKey() {
        return errorType == ErrorType.DUPLICATE_KEY ||
               (sqlState != null && sqlState.startsWith("23"));
    }
    
    /**
     * Get user-friendly error message
     * 
     * Returns a message suitable for client response.
     * Technical details are hidden.
     * 
     * @return Error message for client
     */
    public String getUserMessage() {
        switch (errorType) {
            case CONNECTION_FAILED:
            case CONNECTION_TIMEOUT:
            case POOL_EXHAUSTED:
                return "Database service temporarily unavailable. Please try again.";
                
            case DUPLICATE_KEY:
                return "This record already exists. Please check your input.";
                
            case CONSTRAINT_VIOLATION:
                return "Data validation failed. Please check your input.";
                
            case NOT_FOUND:
                return "Record not found. Please verify and try again.";
                
            case TRANSACTION_FAILED:
                return "Database transaction failed. Please try again.";
                
            case SCHEMA_ERROR:
                return "Database configuration error. Please contact support.";
                
            case QUERY_FAILED:
            case UPDATE_FAILED:
            case DELETE_FAILED:
                return "Database operation failed. Please try again.";
                
            default:
                return "A database error occurred. Please try again later.";
        }
    }
    
    /**
     * Extract SQL state from SQLException
     * 
     * Analyzes the cause to extract SQL state code.
     * 
     * @param cause Exception to analyze
     * @return SQL state or null
     */
    private static String extractSqlState(Throwable cause) {
        if (cause == null) {
            return null;
        }
        
        try {
            // Check if it's a SQLException
            if (cause.getClass().getName().equals("java.sql.SQLException")) {
                // Use reflection to get SQLState without importing SQLException
                return (String) cause.getClass()
                    .getMethod("getSQLState")
                    .invoke(cause);
            }
        } catch (Exception e) {
            // Ignore reflection errors
        }
        
        return null;
    }
    
    /**
     * Get detailed diagnostic information
     * 
     * Used for logging and debugging.
     * 
     * @return Diagnostic string with all details
     */
    public String getDiagnostics() {
        StringBuilder sb = new StringBuilder();
        sb.append("DatabaseException: ").append(errorType.getDescription());
        
        if (tableName != null) {
            sb.append(" | Table: ").append(tableName);
        }
        
        if (sqlState != null) {
            sb.append(" | SQL State: ").append(sqlState);
        }
        
        if (getCause() != null) {
            sb.append(" | Cause: ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
}
