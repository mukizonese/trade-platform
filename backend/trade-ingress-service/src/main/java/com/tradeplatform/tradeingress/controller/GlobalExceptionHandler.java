package com.tradeplatform.tradeingress.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error", ex);
        Map<String, Object> errors = new HashMap<>();
        errors.put("status", "VALIDATION_ERROR");
        errors.put("message", "Validation failed");
        errors.put("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ))
                .collect(Collectors.toList()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
    
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Request body error: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("status", "BAD_REQUEST");
        
        String message = ex.getMessage();
        if (message != null && message.contains("Required request body is missing")) {
            error.put("message", "Request body is required");
        } else if (message != null && message.contains("JSON parse error")) {
            error.put("message", "Invalid JSON format in request body");
        } else {
            error.put("message", "Invalid request body: " + (message != null ? message : "Unknown error"));
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        Map<String, Object> error = new HashMap<>();
        error.put("status", "ERROR");
        error.put("message", "An unexpected error occurred: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

