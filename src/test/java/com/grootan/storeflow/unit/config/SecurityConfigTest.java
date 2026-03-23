package com.grootan.storeflow.unit.config;

import com.grootan.storeflow.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void corsConfigurationSource_shouldReturnValidConfiguration() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        CorsConfiguration config = source.getCorsConfiguration(null);
        assertNotNull(config);
        assertNotNull(config.getAllowedOrigins());
        assertTrue(config.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(config.getAllowedOrigins().contains("http://localhost:5173"));
        assertNotNull(config.getAllowedMethods());
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(Boolean.TRUE.equals(config.getAllowCredentials()));
    }
}