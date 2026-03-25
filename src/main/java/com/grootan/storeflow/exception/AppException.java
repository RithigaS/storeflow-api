package com.grootan.storeflow.exception;

import org.springframework.http.HttpStatus;
import java.util.Map;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, String> errors;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errors = null;
    }

    public AppException(String message, HttpStatus status, Map<String, String> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }


    public HttpStatus getStatus() {
        return status;
    }


    public HttpStatus getStatusCode() {
        return status;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}