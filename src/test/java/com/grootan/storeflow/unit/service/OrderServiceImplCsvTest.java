package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.NotificationService;
import com.grootan.storeflow.service.OrderReportPdfService;
import com.grootan.storeflow.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplCsvTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private OrderReportPdfService orderReportPdfService;
    private NotificationService notificationService;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        orderReportPdfService = mock(OrderReportPdfService.class);
        notificationService = mock(NotificationService.class);


        orderService = new OrderServiceImpl(
                orderRepository,
                productRepository,
                userRepository,
                orderReportPdfService,
                notificationService
        );
    }

    @Test
    void exportOrdersAsCsvProducesHeaderAndRows() {

        User user = buildUser();

        Product product1 = buildProduct("Laptop", "LAP-001", 50000);
        Product product2 = buildProduct("Mouse", "MOU-001", 1000);

        Order order = buildOrder(user, LocalDateTime.of(2026, 3, 25, 10, 30));

        order.addOrderItem(buildItem(product1, 2));
        order.addOrderItem(buildItem(product2, 1));
        order.recalculateTotalAmount();

        when(userRepository.findByEmailIgnoreCase("user@test.com"))
                .thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user))
                .thenReturn(List.of(order));

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "user@test.com",
                false
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("orderId"));
        assertTrue(csv.contains("Laptop"));
        assertTrue(csv.contains("Mouse"));
        assertTrue(csv.contains("user@test.com"));
    }

    @Test
    void exportOrdersAsCsvWithNoOrdersReturnsHeaderOnly() {

        User user = buildUser();

        when(userRepository.findByEmailIgnoreCase("user@test.com"))
                .thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user))
                .thenReturn(List.of());

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "user@test.com",
                false
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("orderId"));
    }

    @Test
    void exportOrdersAsCsvSkipsOrdersBeforeFromDate() {

        User user = buildUser();

        Product product = buildProduct("Laptop", "LAP-002", 50000);

        Order order = buildOrder(user, LocalDateTime.of(2026, 3, 20, 10, 0));
        order.addOrderItem(buildItem(product, 1));

        when(userRepository.findByEmailIgnoreCase("user@test.com"))
                .thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user))
                .thenReturn(List.of(order));

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 21),
                LocalDate.of(2026, 3, 25),
                "user@test.com",
                false
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertFalse(csv.contains("Laptop"));
    }

    @Test
    void exportOrdersAsCsvSkipsOrdersAfterToDate() {

        User user = buildUser();

        Product product = buildProduct("Mouse", "MOU-002", 1000);

        Order order = buildOrder(user, LocalDateTime.of(2026, 3, 30, 10, 0));
        order.addOrderItem(buildItem(product, 1));

        when(userRepository.findByEmailIgnoreCase("user@test.com"))
                .thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user))
                .thenReturn(List.of(order));

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "user@test.com",
                false
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertFalse(csv.contains("Mouse"));
    }



    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    private Product buildProduct(String name, String sku, int price) {
        Category category = new Category();
        category.setName("Electronics");

        Product product = new Product();
        product.setName(name);
        product.setSku(sku);
        product.setPrice(BigDecimal.valueOf(price));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);
        return product;
    }

    private Order buildOrder(User user, LocalDateTime time) {
        Order order = new Order();
        order.setId(1001L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(time);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));
        return order;
    }

    private OrderItem buildItem(Product product, int qty) {
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();
        return item;
    }
}