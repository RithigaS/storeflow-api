package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.CreateOrderItemRequest;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.Order;
import com.grootan.storeflow.entity.OrderItem;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.ShippingAddress;
import com.grootan.storeflow.entity.User;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.exception.InsufficientStockException;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User user;
    private User anotherUser;
    private Product product;
    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@grootan.com");

        anotherUser = new User();
        anotherUser.setId(2L);
        anotherUser.setEmail("other@grootan.com");

        product = new Product();
        product.setId(10L);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(500));
        product.setStockQuantity(10);

        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductId(10L);
        item.setQuantity(2);

        request = new CreateOrderRequest();
        request.setStreet("Street 1");
        request.setCity("Coimbatore");
        request.setCountry("India");
        request.setPostalCode("641001");
        request.setItems(List.of(item));
    }

    @Test
    void placeOrderShouldThrowWhenUserNotFound() {
        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(request, "user@grootan.com"));

        verify(productRepository, never()).findById(anyLong());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrderShouldThrowWhenProductNotFound() {
        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.placeOrder(request, "user@grootan.com"));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrderShouldThrowWhenInsufficientStock() {
        product.setStockQuantity(1);

        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(request, "user@grootan.com"));

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrderShouldDeductStockForEachItem() {
        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        OrderDto result = orderService.placeOrder(request, "user@grootan.com");

        assertNotNull(result);
        assertEquals(8, product.getStockQuantity());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getOrdersShouldReturnAllWhenAdminTrue() {
        Order order1 = createOrder(user, OrderStatus.PENDING);
        order1.setId(1L);

        Order order2 = createOrder(anotherUser, OrderStatus.CONFIRMED);
        order2.setId(2L);

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<OrderDto> result = orderService.getOrders("user@grootan.com", true);

        assertEquals(2, result.size());
        verify(orderRepository).findAll();
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void getOrdersShouldReturnOnlyUserOrdersWhenAdminFalse() {
        Order order = createOrder(user, OrderStatus.PENDING);
        order.setId(1L);

        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.of(user));
        when(orderRepository.findByCustomer(user)).thenReturn(List.of(order));

        List<OrderDto> result = orderService.getOrders("user@grootan.com", false);

        assertEquals(1, result.size());
        assertEquals("user@grootan.com", result.get(0).customerEmail());
        verify(orderRepository).findByCustomer(user);
    }

    @Test
    void getOrdersShouldThrowWhenUserNotFoundForNonAdmin() {
        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrders("user@grootan.com", false));

        verify(orderRepository, never()).findByCustomer(any(User.class));
    }

    @Test
    void getOrderByIdShouldReturnOrderForAdmin() {
        Order order = createOrder(anotherUser, OrderStatus.PENDING);
        order.setId(101L);

        when(orderRepository.findWithDetailsById(101L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById(101L, "user@grootan.com", true);

        assertNotNull(result);
        assertEquals(101L, result.id());
    }

    @Test
    void getOrderByIdShouldReturnOrderForOwner() {
        Order order = createOrder(user, OrderStatus.PENDING);
        order.setId(102L);

        when(orderRepository.findWithDetailsById(102L)).thenReturn(Optional.of(order));

        OrderDto result = orderService.getOrderById(102L, "user@grootan.com", false);

        assertNotNull(result);
        assertEquals(102L, result.id());
        assertEquals("user@grootan.com", result.customerEmail());
    }

    @Test
    void getOrderByIdShouldThrowWhenOrderNotFound() {
        when(orderRepository.findWithDetailsById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(999L, "user@grootan.com", false));
    }

    @Test
    void getOrderByIdShouldThrowWhenNonAdminAccessesAnotherUsersOrder() {
        Order order = createOrder(anotherUser, OrderStatus.PENDING);
        order.setId(103L);

        when(orderRepository.findWithDetailsById(103L)).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.getOrderById(103L, "user@grootan.com", false));
    }

    @Test
    void updateStatusShouldAllowPendingToConfirmed() {
        Order order = createOrder(user, OrderStatus.PENDING);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1L, OrderStatus.CONFIRMED);

        assertEquals("CONFIRMED", result.status().name());
    }

    @Test
    void updateStatusShouldAllowPendingToCancelled() {
        Order order = createOrder(user, OrderStatus.PENDING);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertEquals("CANCELLED", result.status().name());
    }

    @Test
    void updateStatusShouldAllowConfirmedToShipped() {
        Order order = createOrder(user, OrderStatus.CONFIRMED);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1L, OrderStatus.SHIPPED);

        assertEquals("SHIPPED", result.status().name());
    }

    @Test
    void updateStatusShouldAllowConfirmedToCancelled() {
        Order order = createOrder(user, OrderStatus.CONFIRMED);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1L, OrderStatus.CANCELLED);

        assertEquals("CANCELLED", result.status().name());
    }

    @Test
    void updateStatusShouldAllowShippedToDelivered() {
        Order order = createOrder(user, OrderStatus.SHIPPED);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderDto result = orderService.updateStatus(1L, OrderStatus.DELIVERED);

        assertEquals("DELIVERED", result.status().name());
    }

    @Test
    void updateStatusShouldThrowWhenOrderNotFound() {
        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> orderService.updateStatus(1L, OrderStatus.CONFIRMED));
    }

    @Test
    void updateStatusShouldThrowForIllegalStateChanges() {
        Order order = createOrder(user, OrderStatus.DELIVERED);
        order.setId(1L);

        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(1L, OrderStatus.PENDING));
    }

    private Order createOrder(User customer, OrderStatus status) {
        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(status);
        order.setShippingAddress(new ShippingAddress("Street 1", "Coimbatore", "India", "641001"));

        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getPrice());
        item.calculateSubtotal();

        order.addOrderItem(item);
        order.recalculateTotalAmount();
        return order;
    }
}