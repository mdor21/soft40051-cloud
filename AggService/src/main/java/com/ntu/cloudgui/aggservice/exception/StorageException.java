package com.ntu.cloudgui.aggservice.exception;

/**
 * StorageException - Storage Operation Error
 * 
 * Thrown when SFTP/file storage operations fail.
 * Covers upload, download, and verification errors.
 * 
 * Used for:
 * - SFTP connection failures
 * - File upload/download failures
 * - Remote file system errors
 * - SSH authentication failures
 * - Path/directory creation failures
 * 
 * Example:
 * <pre>
 * try {
 *     chunkStorageService.storeChunk(fileId, index, data);
 * } catch (StorageException e) {
 *     logger.error("Storage failed: {}", e.getMessage());
 *     // Handle error...
 * }
 * </pre>
 */
public class StorageException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Storage error types
     */
    public enum ErrorType {
        CONNECTION_FAILED("Failed to connect to storage server"),
        AUTHENTICATION_FAILED("Authentication to storage server failed"),
        UPLOAD_FAILED("Failed to upload file to storage"),
        DOWNLOAD_FAILED("Failed to download file from storage"),
        DIRECTORY_CREATION_FAILED("Failed to create directory on storage"),
        FILE_NOT_FOUND("File not found on storage server"),
        PERMISSION_DENIED("Permission denied on storage server"),
        DISK_FULL("Disk space full on storage server"),
        TIMEOUT("Storage operation timed out"),
        UNKNOWN("Unknown storage error");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String serverHost;
    
    /**
     * Constructor - with message only
     * 
     * @param message Error description
     */
    public StorageException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.serverHost = null;
    }
    
    /**
     * Constructor - with message and cause
     * 
     * @param message Error description
     * @param cause Root cause exception
     */
    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
        this.serverHost = null;
    }
    
    /**
     * Constructor - with error type and server host
     * 
     * @param errorType Type of storage error
     * @param serverHost Server where error occurred
     */
    public StorageException(ErrorType errorType, String serverHost) {
        super(errorType.getDescription() + " (server: " + serverHost + ")");
        this.errorType = errorType;
        this.serverHost = serverHost;
    }
    
    /**
     * Constructor - with error type, server, and cause
     * 
     * @param errorType Type of storage error
     * @param serverHost Server where error occurred
     * @param cause Root cause exception
     */
    public StorageException(ErrorType errorType, String serverHost, Throwable cause) {
        super(errorType.getDescription() + " (server: " + serverHost + ")", cause);
        this.errorType = errorType;
        this.serverHost = serverHost;
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
     * Get server host
     * 
     * @return Server hostname where error occurred
     */
    public String getServerHost() {
        return serverHost;
    }
    
    /**
     * Check if error is retryable
     * 
     * Some errors (like connection timeout) can be retried.
     * Others (like authentication failed) should not be retried.
     * 
     * @return true if operation can be retried
     */
    public boolean isRetryable() {
        return errorType == ErrorType.TIMEOUT ||
               errorType == ErrorType.DISK_FULL ||
               errorType == ErrorType.CONNECTION_FAILED;
    }
    
    /**
     * Get user-friendly error message
     * 
     * @return Error message suitable for client response
     */
    public String getUserMessage() {
        switch (errorType) {
            case CONNECTION_FAILED:
                return "Storage server is unreachable. Please try again later.";
            case AUTHENTICATION_FAILED:
                return "Storage server authentication failed. Please contact support.";
            case UPLOAD_FAILED:
            case DOWNLOAD_FAILED:
                return "Failed to access storage. Please try again.";
            case FILE_NOT_FOUND:
                return "File not found on storage server.";
            case PERMISSION_DENIED:
                return "Storage access denied. Please contact support.";
            case DISK_FULL:
                return "Storage server is full. Please try again later.";
            case TIMEOUT:
                return "Storage operation timed out. Please try again.";
            default:
                return "Storage operation failed. Please try again later.";
        }
    }
}
