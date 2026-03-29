package com.grootan.storeflow.unit.dto;

import com.grootan.storeflow.dto.ErrorResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorResponseTest {

    @Test
    void constructorAndGettersShouldWork() {
        ErrorResponse response = new ErrorResponse(
                "2026-03-23T10:00:00Z",
                404,
                "Not Found",
                "/api/x"
        );

        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getMessage());
        assertEquals("/api/x", response.getPath());
    }

    @Test
    void fullConstructorShouldWork() {
        ErrorResponse response = new ErrorResponse(
                "2026-03-23T10:00:00Z",
                400,
                "Bad Request",
                "Validation failed",
                "/api/products",
                Map.of("name", "Product name is required")
        );

        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", response.getError());
        assertEquals("Validation failed", response.getMessage());
        assertEquals("/api/products", response.getPath());
        assertEquals("Product name is required", response.getErrors().get("name"));
    }

    @Test
    void settersShouldWork() {
        ErrorResponse response = new ErrorResponse();
        response.setTimestamp("2026-03-23T10:00:00Z");
        response.setStatus(500);
        response.setError("Internal Server Error");
        response.setMessage("Internal Error");
        response.setPath("/api/test");
        response.setErrors(Map.of("error", "Something went wrong"));

        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", response.getError());
        assertEquals("Internal Error", response.getMessage());
        assertEquals("/api/test", response.getPath());
        assertEquals("Something went wrong", response.getErrors().get("error"));
    }
}