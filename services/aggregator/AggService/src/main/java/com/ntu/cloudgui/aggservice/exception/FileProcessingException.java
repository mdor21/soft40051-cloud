package com.ntu.cloudgui.aggservice.exception;

/**
 * FileProcessingException - File Processing Error
 * 
 * Thrown when file processing operations fail.
 * Covers encryption, chunking, decryption, and reassembly errors.
 * 
 * Used for:
 * - Encryption/decryption failures
 * - Chunking/reassembly failures
 * - File system errors
 * - Integrity validation failures
 * 
 * Example:
 * <pre>
 * try {
 *     fileProcessingService.encryptAndChunk(filename, data);
 * } catch (FileProcessingException e) {
 *     logger.error("Processing failed: {}", e.getMessage());
 *     // Handle error...
 * }
 * </pre>
 */
public class FileProcessingException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructor - with message only
     * 
     * @param message Error description
     */
    public FileProcessingException(String message) {
        super(message);
    }
    
    /**
     * Constructor - with message and cause
     * 
     * @param message Error description
     * @param cause Root cause exception
     */
    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Constructor - with cause only
     * 
     * @param cause Root cause exception
     */
    public FileProcessingException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Get user-friendly error message
     * 
     * @return Error message suitable for client response
     */
    public String getUserMessage() {
        String message = getMessage();
        if (message == null) {
            return "File processing failed";
        }
        
        // Remove technical details for client response
        if (message.contains("encryption")) {
            return "Encryption failed - please try again";
        }
        if (message.contains("checksum") || message.contains("CRC")) {
            return "File integrity check failed - file may be corrupted";
        }
        if (message.contains("storage") || message.contains("SFTP")) {
            return "Storage server unreachable - please try again later";
        }
        if (message.contains("database") || message.contains("SQL")) {
            return "Database error - please try again later";
        }
        
        return "File processing failed - please try again";
    }
}
