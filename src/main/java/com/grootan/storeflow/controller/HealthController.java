package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.HealthResponse;
import com.grootan.storeflow.util.TimeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;

@RestController
@Tag(name = "Health Controller", description = "API for checking application health status")
public class HealthController {

    @Operation(
            summary = "Health check",
            description = "Returns the current status of the application including uptime and timestamp"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is healthy",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = HealthResponse.class)
                    ))
    })
    @GetMapping("/api/health")
    public HealthResponse health() {
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        return new HealthResponse("UP", TimeUtil.currentTimestamp(), uptime);
    }
}