package com.grootan.storeflow.unit.exception;

import com.grootan.storeflow.dto.ErrorResponse;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.exception.GlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.NoHandlerFoundException;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;


import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleAppException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders");

        AppException exception = new AppException("Order not found", HttpStatus.NOT_FOUND);

        ResponseEntity<ErrorResponse> response = handler.handleAppException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Order not found", response.getBody().getMessage());
        assertEquals("/api/orders", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleNotFoundException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/missing");

        NoHandlerFoundException exception =
                new NoHandlerFoundException("GET", "/api/missing", HttpHeaders.EMPTY);

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Resource not found", response.getBody().getMessage());
        assertEquals("/api/missing", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleGenericExceptionWhenMessageIsPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        Exception exception = new Exception("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Something went wrong", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void shouldHandleGenericExceptionWhenMessageIsNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/test");

        Exception exception = new Exception((String) null);

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal server error", response.getBody().getMessage());
        assertEquals("/api/test", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }


    @Test
    void shouldHandleMethodArgumentNotValidException() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/products");

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "createProductRequest");
        bindingResult.addError(new FieldError("createProductRequest", "name", "Product name is required"));
        bindingResult.addError(new FieldError("createProductRequest", "price", "Price must be a positive value"));

        Method method = this.getClass().getDeclaredMethod("dummyMethod", String.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(methodParameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Validation failed", response.getBody().get("message"));
        assertEquals("/api/products", response.getBody().get("path"));

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("Product name is required", errors.get("name"));
        assertEquals("Price must be a positive value", errors.get("price"));
    }

    @Test
    void shouldHandleDataIntegrityViolationException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/products");

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException("duplicate key value violates unique constraint");

        ResponseEntity<Map<String, Object>> response =
                handler.handleDataIntegrityViolation(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().get("status"));
        assertEquals("Data integrity violation", response.getBody().get("message"));
        assertEquals("/api/products", response.getBody().get("path"));
    }

    @Test
    void shouldHandleJwtException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/auth/me");

        JwtException exception = new JwtException("JWT expired");

        ResponseEntity<Map<String, Object>> response =
                handler.handleJwtException(exception, request);

        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().get("status"));
        assertEquals("Invalid or expired token", response.getBody().get("message"));
        assertEquals("/api/auth/me", response.getBody().get("path"));
    }

    private void dummyMethod(String value) {
    }
}