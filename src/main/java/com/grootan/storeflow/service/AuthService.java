package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.auth.*;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(String token, ResetPasswordRequest request);
    UserProfileResponse getCurrentUserProfile(String email);
    UserProfileResponse uploadAvatar(String email, MultipartFile file);
}