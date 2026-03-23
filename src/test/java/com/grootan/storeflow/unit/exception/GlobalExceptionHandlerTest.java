package com.grootan.storeflow.unit.exception;

import com.grootan.storeflow.dto.ErrorResponse;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericException_shouldReturn500() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        RuntimeException exception = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleAppException_shouldPreserveCustomStatus() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        AppException exception = new AppException("Custom error", HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponse> response = handler.handleAppException(exception, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Custom error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void handleNotFound_shouldReturn404WithConsistentShape() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/nonexistent");

        NoHandlerFoundException exception =
                new NoHandlerFoundException("GET", "/api/nonexistent", null);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Resource not found", response.getBody().getMessage());
        assertEquals("/api/nonexistent", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }


@Test
void handleGenericException_shouldReturnDefaultMessageWhenExceptionMessageIsNull() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/test");

    Exception exception = new Exception((String) null);

    ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

    assertEquals(500, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(500, response.getBody().getStatus());
    assertEquals("Internal server error", response.getBody().getMessage());
    assertEquals("/api/test", response.getBody().getPath());
    assertNotNull(response.getBody().getTimestamp());
}}