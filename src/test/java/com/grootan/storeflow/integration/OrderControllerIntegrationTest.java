package com.grootan.storeflow.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.filter.AuthRateLimitFilter;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.PasswordResetTokenRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.RefreshTokenRepository;
import com.grootan.storeflow.repository.UserRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest extends TestContainerConfig {

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
                                                FilterChain filterChain) throws ServletException, IOException {
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
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;
    private User defaultUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        orderRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        defaultUser = new User();
        defaultUser.setEmail("user@test.com");
        defaultUser.setPassword(passwordEncoder.encode("password123"));
        defaultUser.setFullName("Test User");
        defaultUser.setRole(Role.USER);
        defaultUser.setEnabled(true);
        defaultUser = userRepository.saveAndFlush(defaultUser);

        adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password123"));
        adminUser.setFullName("Admin User");
        adminUser.setRole(Role.ADMIN);
        adminUser.setEnabled(true);
        adminUser = userRepository.saveAndFlush(adminUser);

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic items");
        category = categoryRepository.saveAndFlush(category);

        product = new Product();
        product.setName("Phone");
        product.setDescription("Smart phone");
        product.setSku("PHN-001");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        product = productRepository.saveAndFlush(product);

        userToken = loginAndGetToken("user@test.com", "password123");
        adminToken = loginAndGetToken("admin@test.com", "password123");
    }

    @Test
    void postOrdersPlacesTheOrderDeductsStockAndReturns201WithReference() throws Exception {
        String requestBody = """
                {
                  "street": "Street 1",
                  "city": "Coimbatore",
                  "country": "India",
                  "postalCode": "641001",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 2
                    }
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.referenceNumber").exists())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(1000))
                .andExpect(jsonPath("$.customerEmail").value("user@test.com"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].productName").value("Phone"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(500))
                .andExpect(jsonPath("$.items[0].subtotal").value(1000));

        Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(8, updatedProduct.getStockQuantity());
    }

    @Test
    void postOrdersFailsWith409WhenAnyItemHasInsufficientStock() throws Exception {
        String requestBody = """
                {
                  "street": "Street 1",
                  "city": "Coimbatore",
                  "country": "India",
                  "postalCode": "641001",
                  "items": [
                    {
                      "productId": %d,
                      "quantity": 50
                    }
                  ]
                }
                """.formatted(product.getId());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void getOrdersReturnsOrdersForAuthenticatedUser() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].customerEmail").value("user@test.com"))
                .andExpect(jsonPath("$[0].referenceNumber").exists());
    }

    @Test
    void getOrderByIdReturnsOrderWithItemsAndProductDetails() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.referenceNumber").value(order.getReferenceNumber()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Phone"));
    }

    @Test
    void getOrderByIdReturns404ForMissingOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void patchOrderStatusUpdatesStatusWhenTransitionIsValid() throws Exception {
        Order order = createOrderForDefaultUser();

        String requestBody = """
                {
                  "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(patch("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void patchOrderStatusReturns422ForInvalidStatusTransition() throws Exception {
        Order order = createOrderForDefaultUser();
        order.setStatus(OrderStatus.DELIVERED);
        order = orderRepository.saveAndFlush(order);

        String requestBody = """
                {
                  "status": "PENDING"
                }
                """;

        mockMvc.perform(patch("/api/orders/{id}/status", order.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void postOrdersWithInvalidFieldsReturns400WithValidationErrors() throws Exception {
        String requestBody = """
            {
              "street": "",
              "city": "",
              "country": "",
              "postalCode": "",
              "items": []
            }
            """;

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.street").exists())
                .andExpect(jsonPath("$.errors.city").exists())
                .andExpect(jsonPath("$.errors.country").exists())
                .andExpect(jsonPath("$.errors.postalCode").exists());
    }

    // =========================
    // Phase 6 Added Tests
    // =========================

    @Test
    void getOrderReportReturnsPdfContentType() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders/{id}/report", order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void exportOrdersAsCsvReturnsCsvContentType() throws Exception {
        createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders/export")
                        .param("from", LocalDate.now().minusDays(1).toString())
                        .param("to", LocalDate.now().plusDays(1).toString())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"));
    }

    @Test
    void adminCanDownloadAnyOrderReportWhenAdminTrue() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders/{id}/report", order.getId())
                        .param("admin", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    private Order createOrderForDefaultUser() {
        Order order = new Order();
        order.setCustomer(defaultUser);
        order.setShippingAddress(new ShippingAddress(
                "Street 1", "Coimbatore", "India", "641001"
        ));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        return orderRepository.saveAndFlush(order);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String loginBody = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("accessToken").asText();
    }

    @Test
    void userCannotAccessAnotherUsersOrderById() throws Exception {
        User anotherUser = new User();
        anotherUser.setEmail("another@test.com");
        anotherUser.setPassword(passwordEncoder.encode("password123"));
        anotherUser.setFullName("Another User");
        anotherUser.setRole(Role.USER);
        anotherUser.setEnabled(true);
        anotherUser = userRepository.saveAndFlush(anotherUser);

        Order order = new Order();
        order.setCustomer(anotherUser);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();
        order = orderRepository.saveAndFlush(order);

        mockMvc.perform(get("/api/orders/{id}", order.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }
}