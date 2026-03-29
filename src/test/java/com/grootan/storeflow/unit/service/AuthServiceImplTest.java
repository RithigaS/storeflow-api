package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.auth.AuthResponse;
import com.grootan.storeflow.dto.auth.ForgotPasswordRequest;
import com.grootan.storeflow.dto.auth.LoginRequest;
import com.grootan.storeflow.dto.auth.RefreshTokenRequest;
import com.grootan.storeflow.dto.auth.ResetPasswordRequest;
import com.grootan.storeflow.dto.auth.SignupRequest;
import com.grootan.storeflow.dto.auth.UserProfileResponse;
import com.grootan.storeflow.entity.PasswordResetToken;
import com.grootan.storeflow.entity.RefreshToken;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.repository.PasswordResetTokenRepository;
import com.grootan.storeflow.repository.RefreshTokenRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.security.JwtService;
import com.grootan.storeflow.service.EmailService;
import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Spy
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendBaseUrl", "http://localhost:3000");

        signupRequest = new SignupRequest();
        signupRequest.setFullName("Rithi User");
        signupRequest.setEmail("user@gmail.com");
        signupRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@gmail.com");
        loginRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setFullName("Rithi User");
        user.setEmail("user@gmail.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setAvatarUrl(null);
    }

    @Test
    void signupShouldHashPasswordWithBCryptAndReturnJwt() {
        when(userRepository.existsByEmailIgnoreCase(signupRequest.getEmail())).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");

        AuthResponse response = authService.signup(signupRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertNotEquals("password123", savedUser.getPassword());
        assertTrue(passwordEncoder.matches("password123", savedUser.getPassword()));

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Rithi User", response.getFullName());
        assertEquals("USER", response.getRole());

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        verify(emailService, times(1)).sendWelcomeEmail("user@gmail.com", "Rithi User");
    }

    @Test
    void signupShouldThrowWhenEmailAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("user@gmail.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.signup(signupRequest));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Email already registered", ex.getMessage());

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendWelcomeEmail(anyString(), anyString());
    }

    @Test
    void loginShouldReturnTokensForValidCredentials() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(1L, "user@gmail.com", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(1L, "user@gmail.com")).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("user@gmail.com", response.getEmail());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void loginShouldThrowAuthenticationExceptionForWrongEmail() {
        when(userRepository.findByEmailIgnoreCase("wrong@gmail.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@gmail.com");
        request.setPassword("password123");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginShouldThrowAuthenticationExceptionForWrongPassword() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("user@gmail.com");
        request.setPassword("wrong-password");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void refreshTokenShouldReturnNewAccessTokenForValidToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token");
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));
        when(jwtService.generateAccessToken(1L, "user@gmail.com", "USER")).thenReturn("new-access-token");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        AuthResponse response = authService.refreshToken(request);

        assertEquals("new-access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void refreshTokenShouldThrowForInvalidToken() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void refreshTokenShouldThrowForRevokedToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token");
        token.setUser(user);
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Refresh token expired or revoked", ex.getMessage());
    }

    @Test
    void refreshTokenShouldThrowForExpiredToken() {
        RefreshToken token = new RefreshToken();
        token.setToken("refresh-token");
        token.setUser(user);
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(token));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("Refresh token expired or revoked", ex.getMessage());
    }

    @Test
    void forgotPasswordShouldCallEmailServiceExactlyOnceWithCorrectResetLink() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("user@gmail.com");

        authService.forgotPassword(request);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository, times(1)).save(tokenCaptor.capture());

        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getToken());
        assertEquals(user, savedToken.getUser());
        assertFalse(savedToken.isUsed());

        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, times(1))
                .sendPasswordResetEmail(emailCaptor.capture(), linkCaptor.capture());

        assertEquals("user@gmail.com", emailCaptor.getValue());

        String resetLink = linkCaptor.getValue();
        assertTrue(resetLink.contains("/reset-password?token="));
        assertTrue(resetLink.contains(savedToken.getToken()));
    }

    @Test
    void forgotPasswordShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@gmail.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("missing@gmail.com");

        AppException ex = assertThrows(AppException.class, () -> authService.forgotPassword(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User not found", ex.getMessage());

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPasswordShouldThrowAppExceptionForExpiredToken() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("expired-token");
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(resetToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newpassword123");

        AppException ex = assertThrows(AppException.class,
                () -> authService.resetPassword("expired-token", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Reset token expired", ex.getMessage());
    }

    @Test
    void resetPasswordShouldThrowAppExceptionForInvalidToken() {
        when(passwordResetTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newpassword123");

        AppException ex = assertThrows(AppException.class,
                () -> authService.resetPassword("invalid-token", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Invalid reset token", ex.getMessage());
    }

    @Test
    void resetPasswordShouldThrowWhenTokenAlreadyUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("used-token");
        resetToken.setUser(user);
        resetToken.setUsed(true);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("used-token"))
                .thenReturn(Optional.of(resetToken));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newpassword123");

        AppException ex = assertThrows(AppException.class,
                () -> authService.resetPassword("used-token", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Token already used", ex.getMessage());
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndMarkTokenUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken("valid-token");
        resetToken.setUser(user);
        resetToken.setUsed(false);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(resetToken));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("newpassword123");

        authService.resetPassword("valid-token", request);

        assertTrue(passwordEncoder.matches("newpassword123", user.getPassword()));
        assertTrue(resetToken.isUsed());

        verify(userRepository, times(1)).save(user);
        verify(passwordResetTokenRepository, times(1)).save(resetToken);
    }

    @Test
    void getCurrentUserProfileShouldReturnProfile() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com")).thenReturn(Optional.of(user));

        UserProfileResponse response = authService.getCurrentUserProfile("user@gmail.com");

        assertEquals(1L, response.getId());
        assertEquals("Rithi User", response.getFullName());
        assertEquals("user@gmail.com", response.getEmail());
        assertEquals("USER", response.getRole());
    }

    @Test
    void getCurrentUserProfileShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@gmail.com")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.getCurrentUserProfile("missing@gmail.com"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void uploadAvatarShouldStoreAvatarAndReturnUpdatedProfile() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com")).thenReturn(Optional.of(user));
        when(fileStorageService.storeAvatar(multipartFile, 1L)).thenReturn("/uploads/avatars/avatar-1.jpg");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = authService.uploadAvatar("user@gmail.com", multipartFile);

        assertEquals("/uploads/avatars/avatar-1.jpg", response.getAvatarUrl());
        assertEquals("user@gmail.com", response.getEmail());

        verify(fileStorageService, times(1)).storeAvatar(multipartFile, 1L);
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void uploadAvatarShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@gmail.com")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.uploadAvatar("missing@gmail.com", multipartFile));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User not found", ex.getMessage());

        verify(fileStorageService, never()).storeAvatar(any(), anyLong());
    }
}