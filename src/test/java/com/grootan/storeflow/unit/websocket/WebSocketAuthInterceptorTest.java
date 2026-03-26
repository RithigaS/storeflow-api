package com.grootan.storeflow.unit.websocket;

import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.security.JwtService;
import com.grootan.storeflow.security.WebSocketAuthInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorTest {

    @Test
    void rejects_when_no_token() {
        JwtService jwt = mock(JwtService.class);
        UserRepository repo = mock(UserRepository.class);

        WebSocketAuthInterceptor interceptor = new WebSocketAuthInterceptor(jwt, repo);

        MockHttpServletRequest req = new MockHttpServletRequest();

        ServerHttpResponse response = mock(ServerHttpResponse.class);

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(req),
                response,
                mock(org.springframework.web.socket.WebSocketHandler.class), // avoid null warning
                new HashMap<>()
        );

        assertFalse(result);

        // optional verify
        verify(response).setStatusCode(any());
    }
}