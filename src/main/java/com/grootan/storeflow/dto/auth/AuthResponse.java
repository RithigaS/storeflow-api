package com.grootan.storeflow.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after successful authentication")
public class AuthResponse {

    @Schema(
            description = "JWT access token used for authentication",
            example = "eyJhbGciOiJIUzI1NiJ9.access.token"
    )
    private String accessToken;

    @Schema(
            description = "JWT refresh token used to obtain new access tokens",
            example = "eyJhbGciOiJIUzI1NiJ9.refresh.token"
    )
    private String refreshToken;

    @Schema(
            description = "Token type",
            example = "Bearer"
    )
    private String tokenType;

    @Schema(
            description = "Unique ID of the authenticated user",
            example = "1"
    )
    private Long userId;

    @Schema(
            description = "Full name of the user",
            example = "Rithi S"
    )
    private String fullName;

    @Schema(
            description = "Email of the user",
            example = "rithi@example.com"
    )
    private String email;

    @Schema(
            description = "Role assigned to the user",
            example = "USER"
    )
    private String role;

    public AuthResponse() {}

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public Long getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(String role) { this.role = role; }
}