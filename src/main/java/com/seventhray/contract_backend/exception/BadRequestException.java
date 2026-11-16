package com.seventhray.contract_backend.exception;

/**
 * Thrown when client sends bad data, violates business rules, or attempts
 * an invalid state change (e.g., trying to jump from DRAFT to APPROVED directly).
 * 
 * Resulting HTTP Status: 400 BAD REQUEST
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
