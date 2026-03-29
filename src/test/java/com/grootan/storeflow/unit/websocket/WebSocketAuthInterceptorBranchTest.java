package com.grootan.storeflow.unit.websocket;

import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.security.JwtService;
import com.grootan.storeflow.security.WebSocketAuthInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorBranchTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WebSocketAuthInterceptor interceptor =
            new WebSocketAuthInterceptor(jwtService, userRepository);

    private final ServerHttpResponse response = mock(ServerHttpResponse.class);
    private final WebSocketHandler wsHandler = mock(WebSocketHandler.class);

    @Test
    void shouldRejectWhenTokenMissing() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertFalse(result);
        verify(response).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectWhenTokenBlank() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", "   ");
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertFalse(result);
        verify(response).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldExtractTokenFromAuthorizationHeaderAndAllowWhenValid() {
        String token = "valid-token";
        String email = "user@test.com";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(Role.USER);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertTrue(result);
        assertEquals(1L, attributes.get("userId"));
        assertEquals(email, attributes.get("email"));
        assertEquals("USER", attributes.get("role"));
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void shouldExtractTokenFromQueryParamAndAllowWhenValid() {
        String token = "query-token";
        String email = "query@test.com";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("token", token);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        User user = new User();
        user.setId(2L);
        user.setEmail(email);
        user.setRole(Role.ADMIN);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertTrue(result);
        assertEquals(2L, attributes.get("userId"));
        assertEquals(email, attributes.get("email"));
        assertEquals("ADMIN", attributes.get("role"));
    }

    @Test
    void shouldRejectWhenJwtValidButUserNotFound() {
        String token = "valid-token";
        String email = "missing@test.com";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtService.extractClaims(token)).thenReturn(claims);
        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.empty());

        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertFalse(result);
        verify(response).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectWhenJwtServiceThrowsJwtException() {
        String token = "bad-token";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        when(jwtService.extractClaims(token)).thenThrow(mock(JwtException.class));

        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertFalse(result);
        verify(response).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldRejectWhenJwtServiceThrowsIllegalArgumentException() {
        String token = "bad-token";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        when(jwtService.extractClaims(token)).thenThrow(new IllegalArgumentException("bad token"));

        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                response,
                wsHandler,
                attributes
        );

        assertFalse(result);
        verify(response).setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
    }

    @Test
    void afterHandshakeShouldDoNothing() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        assertDoesNotThrow(() ->
                interceptor.afterHandshake(
                        new ServletServerHttpRequest(servletRequest),
                        response,
                        wsHandler,
                        null
                )
        );
    }
}