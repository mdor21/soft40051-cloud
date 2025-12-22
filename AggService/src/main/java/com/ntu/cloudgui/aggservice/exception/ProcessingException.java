package com.ntu.cloudgui.aggservice.exception;

public class ProcessingException extends RuntimeException {
    private ErrorType errorType;

    public ProcessingException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN_ERROR;
    }

    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = ErrorType.UNKNOWN_ERROR;
    }

    public ProcessingException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public ProcessingException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }
}
