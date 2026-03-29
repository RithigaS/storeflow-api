package com.grootan.storeflow.dto;

import com.grootan.storeflow.entity.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Response DTO representing a product")
public record ProductDto(

        @Schema(description = "Unique product ID", example = "1")
        Long id,

        @Schema(description = "Product name", example = "iPhone 15 Pro")
        String name,

        @Schema(description = "Product description", example = "Latest Apple iPhone with advanced features")
        String description,

        @Schema(description = "Stock Keeping Unit", example = "IPHONE-15-PRO")
        String sku,

        @Schema(description = "Product price", example = "999.99")
        BigDecimal price,

        @Schema(description = "Available stock quantity", example = "50")
        Integer stockQuantity,

        @Schema(description = "Product status", example = "ACTIVE")
        ProductStatus status,

        @Schema(description = "URL of product image", example = "/uploads/products/iphone.png")
        String imageUrl,

        @Schema(description = "Category details of the product")
        CategoryDto category

) {}