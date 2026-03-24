package com.grootan.storeflow.unit.config;

import com.grootan.storeflow.config.SecurityConfig;
import com.grootan.storeflow.filter.AuthRateLimitFilter;
import com.grootan.storeflow.security.CustomAccessDeniedHandler;
import com.grootan.storeflow.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        JwtAuthenticationFilter jwtAuthenticationFilter = mock(JwtAuthenticationFilter.class);
        AuthRateLimitFilter authRateLimitFilter = mock(AuthRateLimitFilter.class);
        CustomAccessDeniedHandler customAccessDeniedHandler = mock(CustomAccessDeniedHandler.class);

        securityConfig = new SecurityConfig(
                jwtAuthenticationFilter,
                authRateLimitFilter,
                customAccessDeniedHandler
        );
    }

    @Test
    void passwordEncoderShouldNotBeNull() {
        BCryptPasswordEncoder encoder = securityConfig.passwordEncoder();
        assertNotNull(encoder);
    }
}