package com.ntu.cloudgui.aggservice.exception;

/**
 * Thrown when something goes wrong during file upload/download processing.
 *
 * Typical cases:
 * - Encryption / decryption failures.
 * - CRC32 integrity check failures.
 * - Unexpected I/O problems while streaming.
 */
public class FileProcessingException extends RuntimeException {

    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
