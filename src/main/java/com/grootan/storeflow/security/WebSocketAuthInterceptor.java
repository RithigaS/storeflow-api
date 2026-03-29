package com.grootan.storeflow.security;

import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        String token = extractToken(request);

        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Claims claims = jwtService.extractClaims(token);
            String email = claims.getSubject();

            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElse(null);

            if (user == null) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }

            //  store user info
            attributes.put("userId", user.getId());
            attributes.put("email", user.getEmail());
            attributes.put("role", user.getRole().name());

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {

        // from header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        //  from query param (?token=...)
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getParameter("token");
        }

        return null;
    }
}