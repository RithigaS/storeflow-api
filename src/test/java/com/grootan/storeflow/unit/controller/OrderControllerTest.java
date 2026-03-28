package com.grootan.storeflow.unit.controller;

import com.grootan.storeflow.controller.OrderController;
import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.dto.OrderStatusUpdateRequest;
import com.grootan.storeflow.entity.enums.OrderStatus;
import com.grootan.storeflow.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    private OrderService orderService;
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        orderController = new OrderController(orderService);
    }

    @Test
    void placeOrderShouldUsePrincipalEmailWhenPrincipalPresent() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        OrderDto orderDto = mock(OrderDto.class);
        Principal principal = () -> "user@test.com";

        when(orderService.placeOrder(request, "user@test.com")).thenReturn(orderDto);

        OrderDto result = orderController.placeOrder(request, principal);

        assertSame(orderDto, result);
        verify(orderService).placeOrder(request, "user@test.com");
    }

    @Test
    void placeOrderShouldUseFallbackEmailWhenPrincipalIsNull() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        OrderDto orderDto = mock(OrderDto.class);

        when(orderService.placeOrder(request, "user@test.com")).thenReturn(orderDto);

        OrderDto result = orderController.placeOrder(request, null);

        assertSame(orderDto, result);
        verify(orderService).placeOrder(request, "user@test.com");
    }

    @Test
    void getOrdersShouldUsePrincipalEmailWhenPrincipalPresent() {
        OrderDto order1 = mock(OrderDto.class);
        OrderDto order2 = mock(OrderDto.class);
        Principal principal = () -> "admin@test.com";

        when(orderService.getOrders("admin@test.com", true))
                .thenReturn(List.of(order1, order2));

        List<OrderDto> result = orderController.getOrders(true, principal);

        assertEquals(2, result.size());
        verify(orderService).getOrders("admin@test.com", true);
    }

    @Test
    void getOrdersShouldUseFallbackEmailWhenPrincipalIsNull() {
        OrderDto orderDto = mock(OrderDto.class);

        when(orderService.getOrders("user@test.com", false))
                .thenReturn(List.of(orderDto));

        List<OrderDto> result = orderController.getOrders(false, null);

        assertEquals(1, result.size());
        verify(orderService).getOrders("user@test.com", false);
    }

    @Test
    void getByIdShouldUsePrincipalEmailWhenPrincipalPresent() {
        OrderDto orderDto = mock(OrderDto.class);
        Principal principal = () -> "user@test.com";

        when(orderService.getOrderById(10L, "user@test.com", false)).thenReturn(orderDto);

        OrderDto result = orderController.getById(10L, false, principal);

        assertSame(orderDto, result);
        verify(orderService).getOrderById(10L, "user@test.com", false);
    }

    @Test
    void getByIdShouldUseFallbackEmailWhenPrincipalIsNull() {
        OrderDto orderDto = mock(OrderDto.class);

        when(orderService.getOrderById(11L, "user@test.com", true)).thenReturn(orderDto);

        OrderDto result = orderController.getById(11L, true, null);

        assertSame(orderDto, result);
        verify(orderService).getOrderById(11L, "user@test.com", true);
    }

    @Test
    void updateStatusShouldDelegateToService() {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        OrderDto orderDto = mock(OrderDto.class);
        when(orderService.updateStatus(15L, OrderStatus.CONFIRMED)).thenReturn(orderDto);

        OrderDto result = orderController.updateStatus(15L, request);

        assertSame(orderDto, result);
        verify(orderService).updateStatus(15L, OrderStatus.CONFIRMED);
    }

    @Test
    void downloadOrderReportShouldReturnPdfResponseWhenPrincipalPresent() {
        byte[] pdfBytes = new byte[]{1, 2, 3, 4};
        Principal principal = () -> "user@test.com";

        when(orderService.generateOrderReport(20L, "user@test.com", false)).thenReturn(pdfBytes);

        ResponseEntity<ByteArrayResource> response =
                orderController.downloadOrderReport(20L, false, principal);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertArrayEquals(pdfBytes, response.getBody().getByteArray());

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("order-20.pdf"));

        verify(orderService).generateOrderReport(20L, "user@test.com", false);
    }

    @Test
    void downloadOrderReportShouldUseFallbackEmailWhenPrincipalIsNull() {
        byte[] pdfBytes = new byte[]{9, 8, 7};

        when(orderService.generateOrderReport(21L, "user@test.com", true)).thenReturn(pdfBytes);

        ResponseEntity<ByteArrayResource> response =
                orderController.downloadOrderReport(21L, true, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
        assertArrayEquals(pdfBytes, response.getBody().getByteArray());

        verify(orderService).generateOrderReport(21L, "user@test.com", true);
    }

    @Test
    void exportOrdersShouldReturnCsvResponseWhenPrincipalPresent() {
        byte[] csvBytes = "id,name\n1,Order".getBytes();
        Principal principal = () -> "admin@test.com";

        when(orderService.exportOrdersAsCsv(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "admin@test.com",
                true
        )).thenReturn(csvBytes);

        ResponseEntity<ByteArrayResource> response = orderController.exportOrders(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                true,
                principal
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertArrayEquals(csvBytes, response.getBody().getByteArray());

        String disposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(disposition);
        assertTrue(disposition.contains("orders.csv"));

        verify(orderService).exportOrdersAsCsv(
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                "admin@test.com",
                true
        );
    }

    @Test
    void exportOrdersShouldUseFallbackEmailWhenPrincipalIsNull() {
        byte[] csvBytes = "id,name\n2,Order".getBytes();

        when(orderService.exportOrdersAsCsv(null, null, "user@test.com", false))
                .thenReturn(csvBytes);

        ResponseEntity<ByteArrayResource> response =
                orderController.exportOrders(null, null, false, null);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
        assertNotNull(response.getBody());
        assertArrayEquals(csvBytes, response.getBody().getByteArray());

        verify(orderService).exportOrdersAsCsv(null, null, "user@test.com", false);
    }
}