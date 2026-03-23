package com.grootan.storeflow.unit.dto;

import com.grootan.storeflow.dto.ErrorResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {

    @Test
    void constructorAndGettersShouldWork() {
        ErrorResponse response = new ErrorResponse("2026-03-23T10:00:00Z", 404, "Not Found", "/api/x");

        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getMessage());
        assertEquals("/api/x", response.getPath());
    }

    @Test
    void settersShouldWork() {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp("2026-03-23T10:00:00Z");
        response.setStatus(500);
        response.setMessage("Internal Error");
        response.setPath("/api/test");

        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(500, response.getStatus());
        assertEquals("Internal Error", response.getMessage());
        assertEquals("/api/test", response.getPath());
    }
}