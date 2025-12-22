package com.ntu.cloudgui.aggservice.exception;

public enum ErrorType {
    STORAGE_ERROR("Storage operation failed"),
    VALIDATION_ERROR("Validation failed"),
    ENCRYPTION_ERROR("Encryption/Decryption failed"),
    INVALID_FILE("Invalid file"),
    FILE_TOO_LARGE("File size exceeds limit"),
    FILE_READ_ERROR("Failed to read file"),
    FILE_NOT_FOUND("File not found"),
    PROCESSING_ERROR("Processing failed"),
    NETWORK_ERROR("Network operation failed"),
    DATABASE_ERROR("Database operation failed"),
    CONFIGURATION_ERROR("Configuration error"),
    AUTHENTICATION_ERROR("Authentication failed");

    private final String message;

    ErrorType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
