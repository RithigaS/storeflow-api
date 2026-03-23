package com.grootan.storeflow.unit.exception;

import com.grootan.storeflow.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppExceptionTest {

    @Test
    void shouldStoreMessageAndStatus() {
        AppException exception = new AppException("Bad request", HttpStatus.BAD_REQUEST);

        assertEquals("Bad request", exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}