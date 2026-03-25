package com.grootan.storeflow.unit.security;

import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.security.JwtAuthenticationFilter;
import com.grootan.storeflow.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultHeader;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassRequestWhenValidJwtPresent() throws Exception {
        String token = "valid-token";
        String email = "user@gmail.com";

        request.setServletPath("/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        User user = new User();
        user.setEmail(email);
        user.setRole(Role.USER);

        when(userRepository.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(email, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void shouldPassRequestWhenTokenMissing() throws Exception {
        request.setServletPath("/api/auth/me");

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void shouldReturn401WhenTokenExpired() throws Exception {
        String token = "expired-token";
        request.setServletPath("/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);

        Header header = new DefaultHeader();
        Claims claims = new DefaultClaims();
        when(jwtService.extractClaims(token))
                .thenThrow(new ExpiredJwtException(header, claims, "Token expired"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token expired"));
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void shouldReturn401WhenTokenMalformed() throws Exception {
        String token = "bad-token";
        request.setServletPath("/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractClaims(token))
                .thenThrow(new MalformedJwtException("Invalid token"));

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid token"));
        verify(filterChain, never()).doFilter(request, response);
    }
}