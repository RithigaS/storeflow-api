package com.grootan.storeflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.filter.AuthRateLimitFilter;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.EmailService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderFlowIntegrationTest {

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
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        reset(emailService);
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void fullOrderFlow_shouldSendEmailAndUpdateMetrics() throws Exception {
        // user signup + token
        String userEmail = uniqueEmail("user");
        String userSignupResponse = signupAndReturnResponse(userEmail, "password123", "Test User");
        String userToken = readJson(userSignupResponse).get("accessToken").asText();

        // create admin directly in DB + login for admin token
        createUser("admin@gmail.com", "password123", Role.ADMIN, "Admin User");
        String adminToken = loginAndGetAccessToken("admin@gmail.com", "password123");

        // create category + product directly in DB
        Category category = new Category();
        category.setName("Electronics");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Test Laptop");
        product.setSku("TEST-LAP-001");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product = productRepository.save(product);

        // place order with real product id
        String orderBody = """
                {
                  "street": "Main Road",
                  "city": "Coimbatore",
                  "country": "India",
                  "postalCode": "641001",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 1
                    }
                  ]
                }
                """.formatted(product.getId());

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long orderId = readJson(orderResponse).get("id").asLong();

        // confirm order as admin
        String updateBody = """
                {
                  "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // verify email trigger
        verify(emailService, atLeastOnce())
                .sendOrderConfirmationEmail(org.mockito.ArgumentMatchers.eq(userEmail),
                        org.mockito.ArgumentMatchers.anyString());

        // health endpoint
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        // metrics endpoint with admin token
        String metricsResponse = mockMvc.perform(get("/actuator/metrics/order_placed_total")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(metricsResponse.contains("measurements"));
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

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readJson(loginResponse).get("accessToken").asText();
    }

    private JsonNode readJson(String json) throws Exception {
        return objectMapper.readTree(json);
    }

    private String uniqueEmail(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 8) + "@gmail.com";
    }
}