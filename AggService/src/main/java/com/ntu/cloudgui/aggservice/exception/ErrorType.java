package com.ntu.cloudgui.aggservice.exception;

public enum ErrorType {
    ENCRYPTION_FAILED("ENCRYPTION_FAILED"),
    DECRYPTION_FAILED("DECRYPTION_FAILED"),
    CHUNK_STORAGE_ERROR("CHUNK_STORAGE_ERROR"),
    CRC_VALIDATION_ERROR("CRC_VALIDATION_ERROR"),
    SFTP_CONNECTION_ERROR("SFTP_CONNECTION_ERROR"),
    FILE_NOT_FOUND("FILE_NOT_FOUND"),
    INVALID_INPUT("INVALID_INPUT"),
    UNKNOWN_ERROR("UNKNOWN_ERROR");

    private final String code;

    ErrorType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
