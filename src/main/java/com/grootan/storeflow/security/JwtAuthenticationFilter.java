package com.grootan.storeflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/api/auth/signup")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/forgot-password")
                || path.startsWith("/api/auth/reset-password/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
        // ❌ REMOVED actuator skip → now it will be secured properly
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // If no token → continue (Spring Security will handle access)
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtService.extractClaims(token);
            String email = claims.getSubject();

            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // ✅ Convert role → ROLE_ADMIN format
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            writeUnauthorized(response, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            writeUnauthorized(response, "Invalid token");
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        new ObjectMapper().writeValue(response.getWriter(),
                Map.of(
                        "status", 401,
                        "message", message
                ));
    }
}