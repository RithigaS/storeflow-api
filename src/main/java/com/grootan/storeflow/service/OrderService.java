package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.CreateOrderRequest;
import com.grootan.storeflow.dto.OrderDto;
import com.grootan.storeflow.entity.enums.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderDto placeOrder(CreateOrderRequest request, String userEmail);
    List<OrderDto> getOrders(String userEmail, boolean isAdmin);
    OrderDto getOrderById(Long id, String userEmail, boolean isAdmin);
    OrderDto updateStatus(Long id, OrderStatus status);

    byte[] generateOrderReport(Long id, String userEmail, boolean isAdmin);

    byte[] exportOrdersAsCsv(LocalDate from, LocalDate to, String userEmail, boolean isAdmin);
}