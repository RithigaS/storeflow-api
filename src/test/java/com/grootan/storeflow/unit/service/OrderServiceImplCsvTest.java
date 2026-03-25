package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.OrderReportPdfService;
import com.grootan.storeflow.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OrderServiceImplCsvTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private OrderReportPdfService orderReportPdfService;
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = Mockito.mock(OrderRepository.class);
        productRepository = Mockito.mock(ProductRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        orderReportPdfService = Mockito.mock(OrderReportPdfService.class);

        orderService = new OrderServiceImpl(
                orderRepository,
                productRepository,
                userRepository,
                orderReportPdfService
        );
    }

    @Test
    void exportOrdersAsCsvProducesHeaderAndOneRowPerOrderItem() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setEnabled(true);

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic items");

        Product product1 = new Product();
        product1.setId(101L);
        product1.setName("Laptop");
        product1.setSku("LAP-001");
        product1.setPrice(BigDecimal.valueOf(50000));
        product1.setStockQuantity(10);
        product1.setStatus(ProductStatus.ACTIVE);
        product1.setCategory(category);

        Product product2 = new Product();
        product2.setId(102L);
        product2.setName("Mouse");
        product2.setSku("MOU-001");
        product2.setPrice(BigDecimal.valueOf(1000));
        product2.setStockQuantity(20);
        product2.setStatus(ProductStatus.ACTIVE);
        product2.setCategory(category);

        Order order = new Order();
        order.setId(1001L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(new ShippingAddress(
                "Street 1",
                "Coimbatore",
                "India",
                "641001"
        ));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 30));

        OrderItem item1 = new OrderItem();
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(product1.getPrice());
        item1.calculateSubtotal();

        OrderItem item2 = new OrderItem();
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(product2.getPrice());
        item2.calculateSubtotal();

        order.addOrderItem(item1);
        order.addOrderItem(item2);
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

        assertTrue(csv.contains("orderId,orderDate,customerName,customerEmail,status,productName,quantity,unitPrice,subtotal,totalAmount"));
        assertTrue(csv.contains("\"Test User\""));
        assertTrue(csv.contains("\"user@test.com\""));
        assertTrue(csv.contains("\"Laptop\""));
        assertTrue(csv.contains("\"Mouse\""));
    }
    @Test
    void exportOrdersAsCsvWithNoOrdersReturnsHeaderOnly() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setEnabled(true);

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

        assertTrue(csv.startsWith("orderId,orderDate,customerName,customerEmail,status,productName,quantity,unitPrice,subtotal,totalAmount"));
    }
    @Test
    void exportOrdersAsCsvSkipsOrdersBeforeFromDate() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);
        user.setEnabled(true);

        Category category = new Category();
        category.setName("Electronics");

        Product product = new Product();
        product.setName("Laptop");
        product.setSku("LAP-002");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Order order = new Order();
        order.setId(1001L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 20, 10, 0));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

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

        assertTrue(csv.startsWith("orderId,orderDate,customerName,customerEmail,status,productName,quantity,unitPrice,subtotal,totalAmount"));
        assertFalse(csv.contains("\"Laptop\""));
    }

    @Test
    void exportOrdersAsCsvSkipsOrdersAfterToDate() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);
        user.setEnabled(true);

        Category category = new Category();
        category.setName("Electronics");

        Product product = new Product();
        product.setName("Mouse");
        product.setSku("MOU-002");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Order order = new Order();
        order.setId(1002L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 30, 10, 0));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
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

        assertFalse(csv.contains("\"Mouse\""));
    }
}