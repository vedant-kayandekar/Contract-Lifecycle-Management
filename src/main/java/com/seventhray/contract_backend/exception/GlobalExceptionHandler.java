package com.seventhray.contract_backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. catch 400 bad request - bussiness rules fail eg duplicate email or inavlid status jump

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
        // Triggered when business rules fail (e.g., duplicate email, invalid status jump)
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // 2. catch 404 Not found
    // when id does not exists in databse
    @ExceptionHandler (ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>>handleNotFound(ResourceNotFoundException ex){

        return buildResponse(HttpStatus.NOT_FOUND, "NotFound", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String,Object>>handleUnauthorized(UnauthorizedException ex){

        return buildResponse(HttpStatus.UNAUTHORIZED,"Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Map<String, Object>>handleFileStorage(FileStorageException ex){

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "fILE Storage Error", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>>handleValidation(MethodArgumentNotValidException ex){
        

        Map<String, String> filedErrors = new HashMap<>();
        for(FieldError error: ex.getBindingResult().getFieldErrors()){
            filedErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String,Object> body = new HashMap<>();
        body.put("timestamp",LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "validation Failed");
        body.put("error",filedErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

     @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Catches any unexpected NullPointerException or system error
        // so the server NEVER crashes or outputs a raw Java stack trace to the user
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", ex.getMessage());
    }



    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String message){
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", message);

        return new ResponseEntity<>(body, status);
    }
}
