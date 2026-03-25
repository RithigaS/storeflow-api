package com.grootan.storeflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.filter.AuthRateLimitFilter;
import com.grootan.storeflow.repository.PasswordResetTokenRepository;
import com.grootan.storeflow.repository.RefreshTokenRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.EmailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")


class AuthIntegrationTest {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;


    @TestConfiguration
    static class NoRateLimitConfig {
        @Bean
        @Primary
        public AuthRateLimitFilter authRateLimitFilter() {
            return new AuthRateLimitFilter() {
                @Override
                protected boolean shouldNotFilter(HttpServletRequest request) {
                    return false;
                }

                @Override
                protected void doFilterInternal(HttpServletRequest request,
                                                HttpServletResponse response,
                                                FilterChain filterChain)
                        throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        reset(emailService);
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullSignupFlowShouldReturnJwtAndAllowMeEndpoint() throws Exception {
        String email = uniqueEmail("signup");

        String signupBody = """
                {
                  "fullName": "Signup User",
                  "email": "%s",
                  "password": "password123"
                }
                """.formatted(email);

        String signupResponse = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken = readJson(signupResponse).get("accessToken").asText();

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Signup User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void loginWithCorrectCredentialsShouldReturnAccessTokenAndRefreshToken() throws Exception {
        createUser("loginuser@gmail.com", "password123", Role.USER, "Login User");

        String loginBody = """
                {
                  "email": "loginuser@gmail.com",
                  "password": "password123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void loginWithWrongPasswordShouldReturn401() throws Exception {
        createUser("wrongpass@gmail.com", "password123", Role.USER, "Wrong Pass User");

        String loginBody = """
                {
                  "email": "wrongpass@gmail.com",
                  "password": "wrongpassword"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessingProtectedEndpointWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessingAdminOnlyEndpointAsUserRoleShouldReturn403() throws Exception {
        String email = uniqueEmail("user");
        String userToken = signupAndGetAccessToken(email, "password123", "Normal User");

        mockMvc.perform(delete("/api/products/{id}", 999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void fullPasswordResetFlowShouldWorkEndToEnd() throws Exception {
        createUser("resetuser@gmail.com", "password123", Role.USER, "Reset User");

        String forgotBody = """
                {
                  "email": "resetuser@gmail.com"
                }
                """;

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forgotBody))
                .andExpect(status().isOk());

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);

        verify(emailService, times(1))
                .sendPasswordResetEmail(toCaptor.capture(), linkCaptor.capture());

        assertEquals("resetuser@gmail.com", toCaptor.getValue());

        String resetLink = linkCaptor.getValue();
        assertTrue(resetLink.contains("/api/auth/reset-password/"));

        String token = resetLink.substring(resetLink.lastIndexOf("/") + 1);

        String resetBody = """
                {
                  "newPassword": "newpassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/reset-password/{token}", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk());

        String loginBody = """
                {
                  "email": "resetuser@gmail.com",
                  "password": "newpassword123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void refreshWithValidRefreshTokenShouldReturnNewAccessToken() throws Exception {
        String email = uniqueEmail("refresh");
        String signupResponse = signupAndReturnResponse(email, "password123", "Refresh User");

        String refreshToken = readJson(signupResponse).get("refreshToken").asText();

        String refreshBody = """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken));
    }

    private User createUser(String email, String rawPassword, Role role, String fullName) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String signupAndGetAccessToken(String email, String password, String fullName) throws Exception {
        String response = signupAndReturnResponse(email, password, fullName);
        return readJson(response).get("accessToken").asText();
    }

    private String signupAndReturnResponse(String email, String password, String fullName) throws Exception {
        String signupBody = """
                {
                  "fullName": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(fullName, email, password);

        return mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    @Test
    void signupWithInvalidFieldsReturns400() throws Exception {
        String body = """
            {
              "fullName": "",
              "email": "invalid",
              "password": "123"
            }
            """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.fullName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginWithInvalidFieldsReturns400() throws Exception {
        String body = """
            {
              "email": "",
              "password": ""
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadAvatarShouldReturnProfileWithAvatarUrl() throws Exception {
        String email = uniqueEmail("avatar");
        String accessToken = signupAndGetAccessToken(email, "password123", "Avatar User");

        MockMultipartFile avatarFile = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "fake-avatar-content".getBytes()
        );

        mockMvc.perform(multipart("/api/auth/me/avatar")
                        .file(avatarFile)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.avatarUrl").exists());
    }

    private JsonNode readJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
    }


}