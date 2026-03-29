package com.grootan.storeflow.unit.filter;

import com.grootan.storeflow.filter.AuthRateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class AuthRateLimitFilterTest {

    private final TestableAuthRateLimitFilter filter = new TestableAuthRateLimitFilter();

    static class TestableAuthRateLimitFilter extends AuthRateLimitFilter {
        public boolean callShouldNotFilter(HttpServletRequest request) {
            return super.shouldNotFilter(request);
        }

        public void callDoFilterInternal(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain) throws ServletException, IOException {
            super.doFilterInternal(request, response, filterChain);
        }
    }

    @Test
    void shouldNotFilterReturnsFalseForAuthEndpoints() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/auth/login");
        assertFalse(filter.callShouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/auth/signup");
        assertFalse(filter.callShouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/auth/refresh");
        assertFalse(filter.callShouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/auth/forgot-password");
        assertFalse(filter.callShouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/auth/reset-password/token");
        assertFalse(filter.callShouldNotFilter(request));
    }

    @Test
    void shouldNotFilterReturnsTrueForNonAuthEndpoints() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getServletPath()).thenReturn("/api/products");
        assertTrue(filter.callShouldNotFilter(request));

        when(request.getServletPath()).thenReturn("/api/orders");
        assertTrue(filter.callShouldNotFilter(request));
    }

    @Test
    void doFilterInternalAllowsRequestWhenBucketHasCapacity() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.callDoFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void doFilterInternalBlocksRequestWhenRateLimitExceeded() throws ServletException, IOException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        when(response.getWriter()).thenReturn(printWriter);

        for (int i = 0; i < 50; i++) {
            filter.callDoFilterInternal(request, response, filterChain);
        }

        filter.callDoFilterInternal(request, response, filterChain);

        verify(response, atLeastOnce()).setStatus(429);
        verify(response, atLeastOnce()).setContentType("application/json");
        assertTrue(stringWriter.toString().contains("Too many requests"));
    }
}