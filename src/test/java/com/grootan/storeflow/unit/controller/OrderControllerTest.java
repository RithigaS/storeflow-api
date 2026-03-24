package com.grootan.storeflow.unit.controller;

import com.grootan.storeflow.controller.OrderController;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderStatusUpdateRequest;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private OrderDto orderDto;

    @BeforeEach
    void setUp() {
        orderDto = new OrderDto(
                1L,
                "ORD-123",
                OrderStatus.PENDING,
                java.math.BigDecimal.valueOf(1000),
                "user@test.com",
                List.of()
        );
    }

    @Test
    void placeOrderShouldUseFallbackEmailWhenPrincipalIsNull() {
        CreateOrderRequest request = new CreateOrderRequest();
        when(orderService.placeOrder(request, "user@test.com")).thenReturn(orderDto);

        OrderDto result = orderController.placeOrder(request, null);

        assertNotNull(result);
        assertEquals("user@test.com", result.customerEmail());
        verify(orderService).placeOrder(request, "user@test.com");
    }

    @Test
    void placeOrderShouldUsePrincipalNameWhenPrincipalExists() {
        CreateOrderRequest request = new CreateOrderRequest();
        Principal principal = () -> "realuser@test.com";

        when(orderService.placeOrder(request, "realuser@test.com")).thenReturn(orderDto);

        OrderDto result = orderController.placeOrder(request, principal);

        assertNotNull(result);
        verify(orderService).placeOrder(request, "realuser@test.com");
    }

    @Test
    void getOrdersShouldUseFallbackEmailAndAdminFalseWhenPrincipalIsNull() {
        when(orderService.getOrders("user@test.com", false)).thenReturn(List.of(orderDto));

        List<OrderDto> result = orderController.getOrders(false, null);

        assertEquals(1, result.size());
        verify(orderService).getOrders("user@test.com", false);
    }

    @Test
    void getOrdersShouldPassPrincipalEmailAndAdminTrue() {
        Principal principal = () -> "admin@test.com";
        when(orderService.getOrders("admin@test.com", true)).thenReturn(List.of(orderDto));

        List<OrderDto> result = orderController.getOrders(true, principal);

        assertEquals(1, result.size());
        verify(orderService).getOrders("admin@test.com", true);
    }

    @Test
    void getByIdShouldUseFallbackEmailWhenPrincipalIsNull() {
        when(orderService.getOrderById(1L, "user@test.com", false)).thenReturn(orderDto);

        OrderDto result = orderController.getById(1L, false, null);

        assertNotNull(result);
        verify(orderService).getOrderById(1L, "user@test.com", false);
    }

    @Test
    void getByIdShouldUsePrincipalEmailAndAdminTrue() {
        Principal principal = () -> "admin@test.com";
        when(orderService.getOrderById(1L, "admin@test.com", true)).thenReturn(orderDto);

        OrderDto result = orderController.getById(1L, true, principal);

        assertNotNull(result);
        verify(orderService).getOrderById(1L, "admin@test.com", true);
    }

    @Test
    void updateStatusShouldDelegateToService() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        OrderDto confirmedOrder = new OrderDto(
                1L,
                "ORD-123",
                OrderStatus.CONFIRMED,
                java.math.BigDecimal.valueOf(1000),
                "user@test.com",
                List.of()
        );

        when(orderService.updateStatus(1L, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);

        OrderDto result = orderController.updateStatus(1L, request);

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.status());
        verify(orderService).updateStatus(1L, OrderStatus.CONFIRMED);
    }
}