package com.grootan.storeflow.unit.exception;

import com.grootan.storeflow.dto.ErrorResponse;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.exception.GlobalExceptionHandler;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerBranchTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
    }

    @Test
    void shouldHandleAppException() {
        AppException ex = new AppException("Custom app error", HttpStatus.BAD_REQUEST);

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex, request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Custom app error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void shouldHandleNotFoundException() throws Exception {
        NoHandlerFoundException ex = new NoHandlerFoundException("GET", "/unknown", null);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Resource not found", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void shouldHandleDataIntegrityViolation() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("duplicate key");

        ResponseEntity<Map<String, Object>> response = handler.handleDataIntegrityViolation(ex, request);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Data integrity violation", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void shouldHandleJwtException() {
        JwtException ex = new JwtException("bad jwt");

        ResponseEntity<Map<String, Object>> response = handler.handleJwtException(ex, request);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Invalid or expired token", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void shouldHandleBadCredentialsException() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(ex, request);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Bad credentials", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void shouldHandleAuthenticationException() {
        InsufficientAuthenticationException ex =
                new InsufficientAuthenticationException("Authentication required");

        ResponseEntity<Map<String, Object>> response = handler.handleAuthenticationException(ex, request);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Authentication required", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void shouldHandleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("forbidden");

        ResponseEntity<Map<String, Object>> response = handler.handleAccessDeniedException(ex, request);

        assertEquals(403, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().get("message"));
        assertEquals("/api/test", response.getBody().get("path"));
    }

    @Test
    void shouldHandleGenericExceptionWhenMessageIsNull() {
        Exception ex = new Exception((String) null);

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void shouldHandleGenericExceptionWhenMessageIsBlank() {
        Exception ex = new Exception("   ");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }

    @Test
    void shouldHandleGenericExceptionWhenMessageIsPresent() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        assertEquals(500, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
    }
}