package com.seventhray.contract_backend.exception;

// http status code - 401 Unauthorized / 403 Forbidden
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message){
        super(message);
    }
}
