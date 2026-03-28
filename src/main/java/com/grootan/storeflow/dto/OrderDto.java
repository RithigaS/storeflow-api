package com.grootan.storeflow.dto;

import com.grootan.storeflow.entity.enums.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Response DTO representing an order")
public record OrderDto(

        @Schema(description = "Unique order ID", example = "1")
        Long id,

        @Schema(description = "Order reference number", example = "ORD-20260328-001")
        String referenceNumber,

        @Schema(description = "Current status of the order", example = "CONFIRMED")
        OrderStatus status,

        @Schema(description = "Total amount of the order", example = "1499.99")
        BigDecimal totalAmount,

        @Schema(description = "Email of the customer who placed the order", example = "rithi@example.com")
        String customerEmail,

        @Schema(description = "List of items included in the order")
        List<OrderItemDto> items

) {}