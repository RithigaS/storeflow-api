package com.grootan.storeflow.dto;

import com.grootan.storeflow.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderDto(
        Long id,
        String referenceNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        String customerEmail,
        List<OrderItemDto> items
) {}