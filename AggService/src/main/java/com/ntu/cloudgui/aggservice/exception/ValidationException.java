package com.ntu.cloudgui.aggservice.exception;

/**
 * ValidationException - Input Validation Error
 * 
 * Thrown when input validation fails.
 * Covers request validation, data format, and constraint errors.
 * 
 * Used for:
 * - Invalid filename format
 * - Invalid file size
 * - Missing required fields
 * - Invalid data types
 * - Constraint violations
 * - Format validation failures
 * 
 * Example:
 * <pre>
 * try {
 *     validateFilename(filename);
 * } catch (ValidationException e) {
 *     logger.warn("Validation failed: {}", e.getMessage());
 *     // Return 400 Bad Request
 * }
 * </pre>
 */
public class ValidationException extends Exception {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Validation error types
     */
    public enum ErrorType {
        MISSING_FIELD("Required field is missing"),
        INVALID_FORMAT("Invalid format"),
        INVALID_LENGTH("Invalid length"),
        INVALID_VALUE("Invalid value"),
        FILE_NOT_FOUND("File not found"),
        FILE_EMPTY("File is empty"),
        FILE_TOO_LARGE("File is too large"),
        FILE_TOO_SMALL("File is too small"),
        INVALID_FILENAME("Invalid filename"),
        PATH_TRAVERSAL("Path traversal not allowed"),
        INVALID_UUID("Invalid UUID format"),
        INVALID_EMAIL("Invalid email format"),
        INVALID_CHECKSUM("Invalid checksum"),
        CONSTRAINT_VIOLATION("Constraint violation"),
        UNKNOWN("Validation failed");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ErrorType errorType;
    private final String fieldName;
    private final String fieldValue;
    
    /**
     * Constructor - with message only
     * 
     * @param message Error description
     */
    public ValidationException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN;
        this.fieldName = null;
        this.fieldValue = null;
    }
    
    /**
     * Constructor - with error type and field name
     * 
     * @param errorType Type of validation error
     * @param fieldName Field that failed validation
     */
    public ValidationException(ErrorType errorType, String fieldName) {
        super(errorType.getDescription() + ": " + fieldName);
        this.errorType = errorType;
        this.fieldName = fieldName;
        this.fieldValue = null;
    }
    
    /**
     * Constructor - with error type, field name, and value
     * 
     * @param errorType Type of validation error
     * @param fieldName Field that failed validation
     * @param fieldValue Value that failed validation
     */
    public ValidationException(ErrorType errorType, String fieldName, String fieldValue) {
        super(errorType.getDescription() + ": " + fieldName + 
              " (value: " + fieldValue + ")");
        this.errorType = errorType;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
    
    /**
     * Constructor - with error type, field name, and cause
     * 
     * @param errorType Type of validation error
     * @param fieldName Field that failed validation
     * @param cause Root cause exception
     */
    public ValidationException(ErrorType errorType, String fieldName, Throwable cause) {
        super(errorType.getDescription() + ": " + fieldName, cause);
        this.errorType = errorType;
        this.fieldName = fieldName;
        this.fieldValue = null;
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
     * Get field name
     * 
     * @return Name of field that failed validation
     */
    public String getFieldName() {
        return fieldName;
    }
    
    /**
     * Get field value
     * 
     * @return Value that failed validation (may be null for sensitive fields)
     */
    public String getFieldValue() {
        return fieldValue;
    }
    
    /**
     * Check if error is due to security concern
     * 
     * Path traversal and similar attacks should be logged differently.
     * 
     * @return true if security-related validation failure
     */
    public boolean isSecurityIssue() {
        return errorType == ErrorType.PATH_TRAVERSAL;
    }
    
    /**
     * Get user-friendly error message
     * 
     * Returns a message suitable for client response.
     * Avoids exposing technical details.
     * 
     * @return Error message for client
     */
    public String getUserMessage() {
        switch (errorType) {
            case MISSING_FIELD:
                return "Required field '" + fieldName + "' is missing.";
                
            case INVALID_FORMAT:
                return "'" + fieldName + "' has an invalid format.";
                
            case INVALID_LENGTH:
                return "'" + fieldName + "' has an invalid length.";
                
            case INVALID_VALUE:
                return "'" + fieldName + "' contains an invalid value.";
                
            case FILE_NOT_FOUND:
                return "File not found.";
                
            case FILE_EMPTY:
                return "File is empty.";
                
            case FILE_TOO_LARGE:
                return "File is too large. Maximum size is 5GB.";
                
            case FILE_TOO_SMALL:
                return "File is too small.";
                
            case INVALID_FILENAME:
                return "Filename is invalid.";
                
            case PATH_TRAVERSAL:
                return "Invalid filename - path traversal not allowed.";
                
            case INVALID_UUID:
                return "'" + fieldName + "' must be a valid UUID.";
                
            case INVALID_EMAIL:
                return "'" + fieldName + "' must be a valid email address.";
                
            case INVALID_CHECKSUM:
                return "Checksum validation failed - file may be corrupted.";
                
            case CONSTRAINT_VIOLATION:
                return "Data constraint violation.";
                
            default:
                return "Validation failed. Please check your input.";
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
        sb.append("ValidationException: ").append(errorType.getDescription());
        
        if (fieldName != null) {
            sb.append(" | Field: ").append(fieldName);
        }
        
        if (fieldValue != null && !isSecurityIssue()) {
            // Don't log sensitive values for security issues
            sb.append(" | Value: ").append(fieldValue);
        }
        
        if (getCause() != null) {
            sb.append(" | Cause: ").append(getCause().getMessage());
        }
        
        return sb.toString();
    }
}
