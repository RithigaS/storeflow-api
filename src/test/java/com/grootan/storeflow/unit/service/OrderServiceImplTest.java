package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.entity.enums.Role;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.EmailService;
import com.grootan.storeflow.service.NotificationService;
import com.grootan.storeflow.service.OrderReportPdfService;
import com.grootan.storeflow.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    private OrderRepository orderRepository;
    private ProductRepository productRepository;
    private UserRepository userRepository;
    private OrderReportPdfService orderReportPdfService;
    private NotificationService notificationService;
    private EmailService emailService;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        productRepository = mock(ProductRepository.class);
        userRepository = mock(UserRepository.class);
        orderReportPdfService = mock(OrderReportPdfService.class);
        notificationService = mock(NotificationService.class);
        emailService = mock(EmailService.class);

        orderService = new OrderServiceImpl(
                orderRepository,
                productRepository,
                userRepository,
                orderReportPdfService,
                notificationService,
                emailService
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

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrders("missing@test.com", false));
    }

    @Test
    void getOrderByIdAsAdminReturnsOrder() {
        Order order = buildOrder(1002L, "someone@test.com", "Someone", OrderStatus.PENDING);
        when(orderRepository.findWithDetailsById(1002L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById(1002L, "admin@test.com", true);

        assertNotNull(result);
        assertEquals(1002L, result.id());
    }

    @Test
    void updateStatusAllowsTransitionAndTriggersNotification() {
        Order order = buildOrder(1010L, "user@test.com", "User", OrderStatus.CONFIRMED);

        when(orderRepository.findWithDetailsById(1010L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1010L, OrderStatus.SHIPPED);

        assertEquals("SHIPPED", result.status().name());

        verify(notificationService, times(1))
                .sendOrderStatusUpdate(
                        eq(1010L),
                        eq(order.getCustomer().getId()),
                        any()
                );
    }

    @Test
    void updateStatusRejectsInvalidTransition() {
        Order order = buildOrder(1012L, "user@test.com", "User", OrderStatus.CANCELLED);
        when(orderRepository.findWithDetailsById(1012L)).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(1012L, OrderStatus.CONFIRMED));
    }

    @Test
    void shouldTriggerLowStockAlert() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setStockQuantity(4);

        emailService.sendLowStockAlert(product.getName(), product.getStockQuantity());

        verify(emailService, times(1))
                .sendLowStockAlert("Test Product", 4);
    }

    @Test
    void shouldSendOrderConfirmationEmail() {
        Order order = buildOrder(2001L, "user@test.com", "User", OrderStatus.PENDING);

        when(orderRepository.findWithDetailsById(2001L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.updateStatus(2001L, OrderStatus.CONFIRMED);

        verify(notificationService, times(1))
                .sendOrderStatusUpdate(
                        eq(2001L),
                        eq(order.getCustomer().getId()),
                        any()
                );

        verify(emailService, times(1))
                .sendOrderConfirmationEmail(eq("user@test.com"), anyString());
    }

    @Test
    void shouldGeneratePdfReport() {
        Order order = buildOrder(3001L, "user@test.com", "User", OrderStatus.PENDING);

        when(orderRepository.findWithDetailsById(3001L)).thenReturn(Optional.of(order));
        when(orderReportPdfService.generateOrderReport(order)).thenReturn(new byte[]{1, 2, 3});

        byte[] result = orderService.generateOrderReport(3001L, "user@test.com", true);

        assertNotNull(result);
        assertTrue(result.length > 0);
        verify(orderReportPdfService, times(1)).generateOrderReport(order);
    }

    @Test
    void shouldExportOrdersAsCsv() {
        Order order = buildOrder(4001L, "user@test.com", "User", OrderStatus.PENDING);

        when(orderRepository.findAll()).thenReturn(List.of(order));

        byte[] csv = orderService.exportOrdersAsCsv(null, null, "admin@test.com", true);

        assertNotNull(csv);
        assertTrue(new String(csv).contains("orderId"));
    }

    private Order buildOrder(Long orderId, String email, String fullName, OrderStatus status) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(Role.USER);

        Category category = new Category();
        category.setName("Electronics");

        Product product = new Product();
        product.setId(101L);
        product.setName("Laptop");
        product.setPrice(BigDecimal.valueOf(50000));
        product.setStockQuantity(10);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        Order order = new Order();
        order.setId(orderId);
        order.setCustomer(user);
        order.setStatus(status);
        order.setShippingAddress(new ShippingAddress("Street", "City", "India", "641001"));
        order.setCreatedAt(LocalDateTime.now());

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();

        return order;
    }
}