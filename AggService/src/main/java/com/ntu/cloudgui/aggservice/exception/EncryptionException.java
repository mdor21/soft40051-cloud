package com.ntu.cloudgui.aggservice.exception;

/**
 * EncryptionException - Encryption/Decryption Error
 * 
 * Thrown when cryptographic operations fail.
 * Covers key generation, encryption, and decryption errors.
 * 
 * Used for:
 * - Key generation failures
 * - Encryption failures
 * - Decryption failures
 * - Invalid cipher configuration
 * - Authentication tag verification failures
 * - IV/nonce errors
 * 
 * Example:
 * <pre>
 * try {
 *     byte[] encrypted = encryptionService.encrypt(data, key);
 * } catch (EncryptionException e) {
 *     logger.error("Encryption failed: {}", e.getMessage());
 *     // Handle error...
 * }
 * </pre>
 */
public class EncryptionException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Encryption error types
     */
    public enum ErrorType {
        KEY_GENERATION_FAILED("Failed to generate encryption key"),
        KEY_NOT_FOUND("Encryption key not found"),
        INVALID_KEY_SIZE("Invalid encryption key size"),
        CIPHER_INIT_FAILED("Failed to initialize cipher"),
        CIPHER_NOT_AVAILABLE("Cipher algorithm not available"),
        ENCRYPTION_FAILED("Encryption operation failed"),
        DECRYPTION_FAILED("Decryption operation failed"),
        AUTH_TAG_INVALID("Authentication tag verification failed - data may be tampered"),
        INVALID_IV("Invalid initialization vector"),
        INVALID_ALGORITHM("Invalid encryption algorithm"),
        UNSUPPORTED_ALGORITHM("Encryption algorithm not supported"),
        UNKNOWN("Unknown encryption error");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String algorithm;
    private final int keySize;
    
    /**
     * Constructor - with message only
     * 
     * @param message Error description
     */
    public EncryptionException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.algorithm = null;
        this.keySize = 0;
    }
    
    /**
     * Constructor - with message and cause
     * 
     * @param message Error description
     * @param cause Root cause exception
     */
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN;
        this.algorithm = null;
        this.keySize = 0;
    }
    
    /**
     * Constructor - with error type and algorithm
     * 
     * @param errorType Type of encryption error
     * @param algorithm Encryption algorithm (e.g., "AES")
     */
    public EncryptionException(ErrorType errorType, String algorithm) {
        super(errorType.getDescription() + " (algorithm: " + algorithm + ")");
        this.errorType = errorType;
        this.algorithm = algorithm;
        this.keySize = 0;
    }
    
    /**
     * Constructor - with error type, algorithm, and cause
     * 
     * @param errorType Type of encryption error
     * @param algorithm Encryption algorithm
     * @param cause Root cause exception
     */
    public EncryptionException(ErrorType errorType, String algorithm, Throwable cause) {
        super(errorType.getDescription() + " (algorithm: " + algorithm + ")", cause);
        this.errorType = errorType;
        this.algorithm = algorithm;
        this.keySize = 0;
    }
    
    /**
     * Constructor - with error type, algorithm, and key size
     * 
     * @param errorType Type of encryption error
     * @param algorithm Encryption algorithm
     * @param keySize Key size in bits (e.g., 256)
     */
    public EncryptionException(ErrorType errorType, String algorithm, int keySize) {
        super(errorType.getDescription() + 
              " (algorithm: " + algorithm + ", keySize: " + keySize + " bits)");
        this.errorType = errorType;
        this.algorithm = algorithm;
        this.keySize = keySize;
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
     * Get encryption algorithm
     * 
     * @return Algorithm name (e.g., "AES/256/GCM")
     */
    public String getAlgorithm() {
        return algorithm;
    }
    
    /**
     * Get key size
     * 
     * @return Key size in bits (0 if not specified)
     */
    public int getKeySize() {
        return keySize;
    }
    
    /**
     * Check if error indicates data tampering
     * 
     * Auth tag failures indicate the data may have been modified.
     * 
     * @return true if data tampering is suspected
     */
    public boolean isDataTampered() {
        return errorType == ErrorType.AUTH_TAG_INVALID;
    }
    
    /**
     * Check if error is retryable
     * 
     * Some errors (like cipher not available) cannot be retried.
     * Others (like temporary failures) might be retried.
     * 
     * @return true if operation can be retried
     */
    public boolean isRetryable() {
        return errorType != ErrorType.KEY_NOT_FOUND &&
               errorType != ErrorType.INVALID_KEY_SIZE &&
               errorType != ErrorType.CIPHER_NOT_AVAILABLE &&
               errorType != ErrorType.UNSUPPORTED_ALGORITHM &&
               errorType != ErrorType.AUTH_TAG_INVALID;
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
        if (isDataTampered()) {
            return "File integrity verification failed - file may be corrupted or tampered.";
        }
        
        switch (errorType) {
            case KEY_NOT_FOUND:
            case KEY_GENERATION_FAILED:
            case INVALID_KEY_SIZE:
                return "Encryption key error. Please contact support.";
                
            case CIPHER_INIT_FAILED:
            case CIPHER_NOT_AVAILABLE:
            case INVALID_ALGORITHM:
            case UNSUPPORTED_ALGORITHM:
                return "Encryption configuration error. Please contact support.";
                
            case ENCRYPTION_FAILED:
                return "Failed to encrypt file. Please try again.";
                
            case DECRYPTION_FAILED:
                return "Failed to decrypt file. File may be corrupted.";
                
            case INVALID_IV:
                return "Encryption data error. File may be corrupted.";
                
            default:
                return "Encryption operation failed. Please try again.";
        }
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
        sb.append("EncryptionException: ").append(errorType.getDescription());
        
        if (algorithm != null) {
            sb.append(" | Algorithm: ").append(algorithm);
        }
        
        if (keySize > 0) {
            sb.append(" | KeySize: ").append(keySize).append(" bits");
        }
        
        if (getCause() != null) {
            sb.append(" | Cause: ").append(getCause().getMessage());
        }
        
        if (isDataTampered()) {
            sb.append(" | WARNING: Data tampering suspected!");
        }
        
        return sb.toString();
    }
}
