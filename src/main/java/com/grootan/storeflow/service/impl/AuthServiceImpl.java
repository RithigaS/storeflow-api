package com.grootan.storeflow.service.impl;

import com.grootan.storeflow.dto.auth.*;
import com.grootan.storeflow.entity.PasswordResetToken;
import com.grootan.storeflow.entity.RefreshToken;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.repository.PasswordResetTokenRepository;
import com.grootan.storeflow.repository.RefreshTokenRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.security.JwtService;
import com.grootan.storeflow.service.AuthService;
import com.grootan.storeflow.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    // ================= SIGNUP =================
    @Override
    public AuthResponse signup(SignupRequest request) {

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new AppException("Email already registered", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setEnabled(true);

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );

        refreshTokenRepository.save(
                buildRefreshToken(user, refreshToken)
        );

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ================= LOGIN =================
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshToken = jwtService.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );

        refreshTokenRepository.save(
                buildRefreshToken(user, refreshToken)
        );

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    // ================= REFRESH =================
    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken token = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException("Invalid refresh token", HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException("Refresh token expired or revoked", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();

        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return buildAuthResponse(user, newAccessToken, token.getToken());
    }

    // ================= FORGOT PASSWORD =================
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        String token = UUID.randomUUID().toString();

        passwordResetTokenRepository.save(
                buildPasswordResetToken(user, token)
        );

        String resetLink = "http://localhost:8080/api/auth/reset-password/" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    // ================= RESET PASSWORD =================
    @Override
    public void resetPassword(String token, ResetPasswordRequest request) {

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException("Invalid reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isUsed()) {
            throw new AppException("Token already used", HttpStatus.BAD_REQUEST);
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException("Reset token expired", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // ================= GET PROFILE =================
    @Override
    public UserProfileResponse getCurrentUserProfile(String email) {

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        return response;
    }

    // ================= HELPERS =================
    private RefreshToken buildRefreshToken(User user, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(token);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        return refreshToken;
    }

    private PasswordResetToken buildPasswordResetToken(User user, String token) {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(30));
        resetToken.setUsed(false);
        return resetToken;
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setUserId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());

        return response;
    }
}