package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplBranchCoverageTest {

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
    void getOrdersAsAdminUsesFindAllBranch() {
        Order order = buildOrder(1001L, "admin@test.com", "Admin User", OrderStatus.PENDING);
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getOrders("admin@test.com", true);

        assertEquals(1, result.size());
        verify(orderRepository).findAll();
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void getOrdersAsUserThrowsWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("missing@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrders("missing@test.com", false)
        );

        verify(orderRepository, never()).findByCustomer(any());
    }

    @Test
    void getOrderByIdAsAdminReturnsOrderWithoutOwnershipCheckFailure() {
        Order order = buildOrder(1002L, "someone@test.com", "Someone", OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1002L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById(1002L, "admin@test.com", true);

        assertNotNull(result);
        assertEquals(1002L, result.id());
    }

    @Test
    void getOrderByIdAsUserThrowsWhenOrderBelongsToAnotherUser() {
        Order order = buildOrder(1003L, "owner@test.com", "Owner", OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1003L)).thenReturn(Optional.of(order));

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getOrderById(1003L, "other@test.com", false)
        );
    }

    @Test
    void generateOrderReportDelegatesToPdfServiceForAccessibleOrder() {
        Order order = buildOrder(1004L, "user@test.com", "Test User", OrderStatus.PENDING);
        byte[] expected = "pdf".getBytes(StandardCharsets.UTF_8);

        when(orderRepository.findWithDetailsById(1004L)).thenReturn(Optional.of(order));
        when(orderReportPdfService.generateOrderReport(order)).thenReturn(expected);

        byte[] actual = orderService.generateOrderReport(1004L, "user@test.com", false);

        assertArrayEquals(expected, actual);
        verify(orderReportPdfService).generateOrderReport(order);
    }

    @Test
    void generateOrderReportThrowsWhenOrderBelongsToAnotherUser() {
        Order order = buildOrder(1005L, "owner@test.com", "Owner", OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1005L)).thenReturn(Optional.of(order));

        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.generateOrderReport(1005L, "other@test.com", false)
        );

        verify(orderReportPdfService, never()).generateOrderReport(any());
    }

    @Test
    void exportOrdersAsCsvAsAdminUsesFindAllBranchAndIncludesData() {
        Order order = buildOrder(1006L, "user@test.com", "Test User", OrderStatus.PENDING);
        when(orderRepository.findAll()).thenReturn(List.of(order));

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "admin@test.com",
                true
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("orderId,orderDate,customerName,customerEmail,status,productName,quantity,unitPrice,subtotal,totalAmount"));
        assertTrue(csv.contains("\"Laptop\""));
        verify(orderRepository).findAll();
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void exportOrdersAsCsvIncludesOrderWhenCreatedAtIsNull() {
        User user = buildUser("user@test.com", "Test User");

        Order order = buildOrder(1007L, "user@test.com", "Test User", OrderStatus.PENDING);
        order.setCreatedAt(null);

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

        assertTrue(csv.contains("\"Laptop\""));
    }

    @Test
    void exportOrdersAsCsvEscapesQuotedValues() {
        User user = buildUser("quoted@test.com", "Test \"Quoted\" User");

        Category category = new Category();
        category.setName("Electronics");

        Product product = new Product();
        product.setId(2001L);
        product.setName("Laptop \"Pro\"");
        product.setSku("LAP-QUOTE-001");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Order order = new Order();
        order.setId(1008L);
        order.setCustomer(user);
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 0));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        when(userRepository.findByEmailIgnoreCase("quoted@test.com"))
                .thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user))
                .thenReturn(List.of(order));

        byte[] csvBytes = orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 24),
                LocalDate.of(2026, 3, 26),
                "quoted@test.com",
                false
        );

        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"Test \"\"Quoted\"\" User\""));
        assertTrue(csv.contains("\"Laptop \"\"Pro\"\"\""));
    }

    @Test
    void updateStatusAllowsPendingToCancelledBranch() {
        Order order = buildOrder(1009L, "user@test.com", "Test User", OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1009L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1009L, OrderStatus.CANCELLED);

        assertEquals(OrderStatus.CANCELLED.name(), result.status().name());
    }

    @Test
    void updateStatusAllowsConfirmedToShippedBranch() {
        Order order = buildOrder(1010L, "user@test.com", "Test User", OrderStatus.CONFIRMED);
        when(orderRepository.findWithDetailsById(1010L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1010L, OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED.name(), result.status().name());
    }

    @Test
    void updateStatusAllowsShippedToDeliveredBranch() {
        Order order = buildOrder(1011L, "user@test.com", "Test User", OrderStatus.SHIPPED);
        when(orderRepository.findWithDetailsById(1011L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1011L, OrderStatus.DELIVERED);

        assertEquals(OrderStatus.DELIVERED.name(), result.status().name());
    }

    @Test
    void updateStatusRejectsCancelledToAnythingBranch() {
        Order order = buildOrder(1012L, "user@test.com", "Test User", OrderStatus.CANCELLED);
        when(orderRepository.findWithDetailsById(1012L)).thenReturn(Optional.of(order));

        assertThrows(
                InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(1012L, OrderStatus.CONFIRMED)
        );

        verify(orderRepository, never()).save(any());
    }

    private Order buildOrder(Long orderId, String email, String fullName, OrderStatus status) {
        User user = buildUser(email, fullName);

        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic items");

        Product product = new Product();
        product.setId(101L);
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setSku("LAP-001");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(user);
        order.setStatus(status);
        order.setShippingAddress(new ShippingAddress("Street 1", "Coimbatore", "India", "641001"));
        order.setCreatedAt(LocalDateTime.of(2026, 3, 25, 10, 30));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        return order;
    }

    private User buildUser(String email, String fullName) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }
}