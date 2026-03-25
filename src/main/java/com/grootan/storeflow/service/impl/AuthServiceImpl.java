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
import org.springframework.web.multipart.MultipartFile;

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
    private final com.grootan.storeflow.service.FileStorageService fileStorageService;

    @Override
    public AuthResponse signup(SignupRequest request) {
        String fullName = normalize(request.getFullName());
        String email = normalizeEmail(request.getEmail());
        String password = normalize(request.getPassword());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException("Email already registered", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
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

        refreshTokenRepository.save(buildRefreshToken(user, refreshToken));

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = normalize(request.getPassword());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
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

        refreshTokenRepository.save(buildRefreshToken(user, refreshToken));

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshTokenValue = normalize(request.getRefreshToken());

        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenValue)
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

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        String token = UUID.randomUUID().toString();

        passwordResetTokenRepository.save(buildPasswordResetToken(user, token));

        String resetLink = "http://localhost:8080/api/auth/reset-password/" + token;

        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    public void resetPassword(String token, ResetPasswordRequest request) {
        String normalizedToken = normalize(token);
        String newPassword = normalize(request.getNewPassword());

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(normalizedToken)
                .orElseThrow(() -> new AppException("Invalid reset token", HttpStatus.BAD_REQUEST));

        if (resetToken.isUsed()) {
            throw new AppException("Token already used", HttpStatus.BAD_REQUEST);
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException("Reset token expired", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    @Override
    public UserProfileResponse getCurrentUserProfile(String email) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setAvatarUrl(user.getAvatarUrl());

        return response;
    }
    @Override
    public UserProfileResponse uploadAvatar(String email, MultipartFile file) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        String avatarUrl = fileStorageService.storeAvatar(file, user.getId());
        user.setAvatarUrl(avatarUrl);

        userRepository.save(user);

        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole().name());
        response.setAvatarUrl(user.getAvatarUrl());
        return response;
    }
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

}