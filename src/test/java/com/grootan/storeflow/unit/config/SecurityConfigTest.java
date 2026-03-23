package com.grootan.storeflow.unit.config;

import com.grootan.storeflow.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void shouldCreateCorsConfigurationSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertNotNull(source);
    }

    @Test
    void shouldReturnExpectedCorsConfiguration() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertNotNull(config.getAllowedOrigins());
        assertNotNull(config.getAllowedMethods());
        assertNotNull(config.getAllowedHeaders());
        assertEquals(2, config.getAllowedOrigins().size());
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));

        assertEquals(6, config.getAllowedMethods().size());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("PATCH"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));

        assertEquals(1, config.getAllowedHeaders().size());
        assertTrue(config.getAllowedHeaders().contains("*"));
        assertEquals(Boolean.TRUE, config.getAllowCredentials());
    }
}