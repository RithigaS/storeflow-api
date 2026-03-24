package com.grootan.storeflow.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends AppException {
    public InsufficientStockException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}