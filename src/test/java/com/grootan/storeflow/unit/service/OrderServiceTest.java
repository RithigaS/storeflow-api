package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.CreateOrderItemRequest;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.entity.*;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.exception.InsufficientStockException;
import com.grootan.storeflow.exception.InvalidStatusTransitionException;
import com.grootan.storeflow.repository.OrderRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.repository.UserRepository;
import com.grootan.storeflow.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private OrderServiceImpl orderService;

    private User user;
    private Product product;
    private CreateOrderRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("user@grootan.com");

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
    void placeOrderShouldThrowWhenInsufficientStock() {
        product.setStockQuantity(1);
        when(userRepository.findByEmailIgnoreCase("user@grootan.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThrows(InsufficientStockException.class,
                () -> orderService.placeOrder(request, "user@grootan.com"));
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

        orderService.placeOrder(request, "user@grootan.com");

        assertEquals(8, product.getStockQuantity());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateStatusShouldThrowForIllegalStateChanges() {
        Order order = new Order();
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findWithDetailsById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidStatusTransitionException.class,
                () -> orderService.updateStatus(1L, OrderStatus.PENDING));
    }
}