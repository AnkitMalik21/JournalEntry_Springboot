package com.pro.Journal_Entry.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
     //HANDLE VALIDATION ERROR

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidationErrors(MethodArgumentNotValidException ex){
        Map<String,String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error ->{
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName,errorMessage);
        });

        Map<String,Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errors",errors);

        return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
    }

    //Handle resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String,Object>> handleResourceNotFound(
            ResourceNotFoundException ex
    ){
        Map<String,Object> response = new HashMap<>();
        response.put("timestamp",LocalDateTime.now());
        response.put("status",HttpStatus.NOT_FOUND.value());
        response.put("message", ex.getMessage());

        return new ResponseEntity<>(response,HttpStatus.NOT_FOUND);

    }

    //Handle duplicate journal
    @ExceptionHandler(DuplicateJournalException.class)
    public ResponseEntity<Map<String,Object>> handleDuplicateJournal(
            DuplicateJournalException ex
    ){
        Map<String,Object> response = new HashMap<>();
        response.put("timestamp",LocalDateTime.now());
        response.put("status",HttpStatus.CONFLICT.value());
        response.put("message",ex.getMessage());

        return new ResponseEntity<>(response,HttpStatus.CONFLICT);
    }

    //Handle authentication errors
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String,Object>> handleBadCredentials(
            BadCredentialsException ex
    ){
        Map<String,Object> response = new HashMap<>();
        response.put("timestamp",LocalDateTime.now());
        response.put("status",HttpStatus.UNAUTHORIZED.value());
        response.put("message","Invalid username or password");

        return new ResponseEntity<>(response,HttpStatus.UNAUTHORIZED);
    }

    //Handle all other exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleGlobalException(Exception ex){
        log.error("Unexcepted error occurred",ex);

        Map<String,Object> response = new HashMap<>();
        response.put("timestamp",LocalDateTime.now());
        response.put("status",HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message","An unexcepted error occurred");

        return new ResponseEntity<>(response,HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
