package com.grootan.storeflow.dto;

import com.grootan.storeflow.entity.enums.ProductStatus;

import java.math.BigDecimal;

public record ProductDto(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        Integer stockQuantity,
        ProductStatus status,
        String imageUrl,
        CategoryDto category
) {}