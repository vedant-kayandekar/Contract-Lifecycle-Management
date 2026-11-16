package com.seventhray.contract_backend.exception;

/**
 * Resulting HTTP Status: 500 INTERNAL SERVER ERROR
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    // This overload allows wrapping the original underlying Java IOException (cause)
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}