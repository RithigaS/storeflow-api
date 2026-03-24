package com.grootan.storeflow.mapper;

import com.grootan.storeflow.dto.CategoryDto;
import com.grootan.storeflow.dto.ProductDto;
import com.grootan.storeflow.entity.Product;

public class ProductMapper {
    public static ProductDto toDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getStatus(),
                new CategoryDto(
                        product.getCategory().getId(),
                        product.getCategory().getName(),
                        product.getCategory().getDescription()
                )
        );
    }
}