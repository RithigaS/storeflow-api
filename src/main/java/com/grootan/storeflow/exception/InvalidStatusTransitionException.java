package com.grootan.storeflow.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends AppException {
    public InvalidStatusTransitionException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}