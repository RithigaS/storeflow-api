package com.grootan.storeflow.unit.dto;

import com.grootan.storeflow.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthResponseTest {

    @Test
    void constructorAndGettersShouldWork() {
        HealthResponse response = new HealthResponse("UP", "2026-03-23T10:00:00Z", 1000L);

        assertEquals("UP", response.getStatus());
        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(1000L, response.getUptime());
    }

    @Test
    void settersShouldWork() {
        HealthResponse response = new HealthResponse();
        response.setStatus("UP");
        response.setTimestamp("2026-03-23T10:00:00Z");
        response.setUptime(500L);

        assertEquals("UP", response.getStatus());
        assertEquals("2026-03-23T10:00:00Z", response.getTimestamp());
        assertEquals(500L, response.getUptime());
    }
}