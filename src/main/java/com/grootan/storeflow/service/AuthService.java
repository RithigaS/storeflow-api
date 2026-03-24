package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.auth.*;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(String token, ResetPasswordRequest request);
    UserProfileResponse getCurrentUserProfile(String email);
}