package com.grootan.storeflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request payload for user login")
public class LoginRequest {

    @Schema(
            description = "User email address",
            example = "rithi@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(
            description = "User password",
            example = "Password@123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    private String password;
}