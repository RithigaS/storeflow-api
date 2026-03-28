package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.auth.AuthResponse;
import com.grootan.storeflow.dto.auth.ForgotPasswordRequest;
import com.grootan.storeflow.dto.auth.LoginRequest;
import com.grootan.storeflow.dto.auth.RefreshTokenRequest;
import com.grootan.storeflow.dto.auth.ResetPasswordRequest;
import com.grootan.storeflow.dto.auth.SignupRequest;
import com.grootan.storeflow.dto.auth.UserProfileResponse;
import com.grootan.storeflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Controller", description = "APIs for authentication, token management, password reset, and current user profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns access token, refresh token, and user details"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.access.token",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token",
                                      "userId": 1,
                                      "email": "rithi@example.com",
                                      "fullName": "Rithi S",
                                      "role": "USER"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid signup request"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Signup request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SignupRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "fullName": "Rithi S",
                                      "email": "rithi@example.com",
                                      "password": "Password@123"
                                    }
                                    """)
                    )
            )
            @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @Operation(
            summary = "Login user",
            description = "Authenticates user credentials and returns access token and refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.access.token",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token",
                                      "userId": 1,
                                      "email": "rithi@example.com",
                                      "fullName": "Rithi S",
                                      "role": "USER"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid login request"),
            @ApiResponse(responseCode = "401", description = "Authentication failed")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Login request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "rithi@example.com",
                                      "password": "Password@123"
                                    }
                                    """)
                    )
            )
            @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Generates a new access token and refresh token using a valid refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "accessToken": "eyJhbGciOiJIUzI1NiJ9.new.access.token",
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.new.refresh.token",
                                      "userId": 1,
                                      "email": "rithi@example.com",
                                      "fullName": "Rithi S",
                                      "role": "USER"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "Invalid refresh request"),
            @ApiResponse(responseCode = "401", description = "Refresh token is invalid or expired")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Refresh token request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefreshTokenRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "refreshToken": "eyJhbGciOiJIUzI1NiJ9.refresh.token"
                                    }
                                    """)
                    )
            )
            @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the registered email address"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Password reset email sent"))),
            @ApiResponse(responseCode = "400", description = "Invalid forgot password request")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Forgot password request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ForgotPasswordRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "email": "rithi@example.com"
                                    }
                                    """)
                    )
            )
            @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok("Password reset email sent");
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user password using the token received in email"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successful",
                    content = @Content(mediaType = "text/plain",
                            examples = @ExampleObject(value = "Password reset successful"))),
            @ApiResponse(responseCode = "400", description = "Invalid reset password request"),
            @ApiResponse(responseCode = "401", description = "Reset token is invalid or expired")
    })
    @PostMapping("/reset-password/{token}")
    public ResponseEntity<String> resetPassword(
            @Parameter(description = "Password reset token received in email", required = true, example = "reset-token-123")
            @PathVariable String token,
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Reset password request payload",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResetPasswordRequest.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "newPassword": "NewPassword@123"
                                    }
                                    """)
                    )
            )
            @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(token, request);
        return ResponseEntity.ok("Password reset successful");
    }

    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile details of the currently authenticated user",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile fetched successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(Principal principal) {
        return ResponseEntity.ok(authService.getCurrentUserProfile(principal.getName()));
    }

    @Operation(
            summary = "Upload current user avatar",
            description = "Uploads or updates the avatar image for the currently authenticated user",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Avatar uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file or upload request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            Principal principal,
            @Parameter(
                    description = "Avatar image file (JPEG, PNG, or WEBP)",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            )
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                authService.uploadAvatar(principal.getName(), file)
        );
    }
}