package com.grootan.storeflow.unit.service;

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
import com.grootan.storeflow.service.EmailService;
import com.grootan.storeflow.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

    @InjectMocks
    private AuthServiceImpl authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
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
    void forgotPasswordShouldCallEmailServiceExactlyOnceWithCorrectResetLink() {
        when(userRepository.findByEmailIgnoreCase("user@gmail.com"))
                .thenReturn(Optional.of(user));

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
}