package com.grootan.storeflow.integration;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Product product;
    private User defaultUser;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        defaultUser = new User();
        defaultUser.setEmail("user@test.com");
        defaultUser.setPassword("password123");
        defaultUser.setFullName("Test User");
        defaultUser = userRepository.saveAndFlush(defaultUser);

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
        org.junit.jupiter.api.Assertions.assertEquals(8, updatedProduct.getStockQuantity());
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());
    }

    @Test
    void getOrdersReturnsOrdersForAuthenticatedFallbackUser() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].customerEmail").value("user@test.com"))
                .andExpect(jsonPath("$[0].referenceNumber").exists());
    }

    @Test
    void getOrderByIdReturnsOrderWithItemsAndProductDetails() throws Exception {
        Order order = createOrderForDefaultUser();

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.referenceNumber").value(order.getReferenceNumber()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productName").value("Phone"));
    }

    @Test
    void getOrderByIdReturns404ForMissingOrder() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", 999999L))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void patchOrderStatusReturns400ForInvalidStatusTransition() throws Exception {
        Order order = createOrderForDefaultUser();
        order.setStatus(OrderStatus.DELIVERED);
        order = orderRepository.saveAndFlush(order);

        String requestBody = """
                {
                  "status": "PENDING"
                }
                """;

        mockMvc.perform(patch("/api/orders/{id}/status", order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    private Order createOrderForDefaultUser() {
        Order order = new Order();
        order.setCustomer(defaultUser);
        order.setShippingAddress(new com.grootan.storeflow.entity.ShippingAddress(
                "Street 1", "Coimbatore", "India", "641001"
        ));

        com.grootan.storeflow.entity.OrderItem item = new com.grootan.storeflow.entity.OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        return orderRepository.saveAndFlush(order);
    }
}