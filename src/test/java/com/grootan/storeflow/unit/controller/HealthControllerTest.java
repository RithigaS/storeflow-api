package com.grootan.storeflow.unit.controller;

import com.grootan.storeflow.controller.HealthController;
import com.grootan.storeflow.dto.HealthResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void health_shouldReturnValidResponse() {
        HealthResponse response = healthController.health();

        assertNotNull(response);
        assertEquals("UP", response.getStatus());
        assertNotNull(response.getTimestamp());
        assertTrue(response.getUptime() >= 0);
    }
}