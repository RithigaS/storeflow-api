package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response DTO representing an item inside an order")
public record OrderItemDto(

        @Schema(description = "Unique order item ID", example = "1")
        Long id,

        @Schema(description = "Product ID of the ordered item", example = "101")
        Long productId,

        @Schema(description = "Name of the product", example = "iPhone 15 Pro")
        String productName,

        @Schema(description = "Quantity ordered", example = "2")
        Integer quantity,

        @Schema(description = "Price per unit", example = "999.99")
        BigDecimal unitPrice,

        @Schema(description = "Subtotal for this item", example = "1999.98")
        BigDecimal subtotal

) {}