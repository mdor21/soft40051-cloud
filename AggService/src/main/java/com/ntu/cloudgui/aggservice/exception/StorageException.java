package com.ntu.cloudgui.aggservice.exception;

/**
 * Thrown when chunks cannot be stored or retrieved from File Servers.
 *
 * Typical cases:
 * - SSH/SFTP connection failures.
 * - Remote path not found or permission issues.
 * - Timeouts when communicating with File Server containers.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
