package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.*;
import com.grootan.storeflow.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface ProductService {
    ProductDto create(CreateProductRequest request);
    Page<ProductDto> getAll(String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice, int page, int size);
    ProductDto getById(Long id);
    ProductDto update(Long id, UpdateProductRequest request);
    ProductDto adjustStock(Long id, int delta);
    void delete(Long id);
}