package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.HealthResponse;
import com.grootan.storeflow.util.TimeUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        return new HealthResponse("UP", TimeUtil.currentTimestamp(), uptime);
    }
}