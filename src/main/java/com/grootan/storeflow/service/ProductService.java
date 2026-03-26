package com.grootan.storeflow.service;

import com.grootan.storeflow.dto.CreateProductRequest;
import com.grootan.storeflow.dto.ProductDto;
import com.grootan.storeflow.dto.UpdateProductRequest;
import com.grootan.storeflow.dto.CursorPageResponse; // ✅ NEW
import com.grootan.storeflow.entity.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    ProductDto create(CreateProductRequest request);

    // Existing method (keep for backward compatibility)
    Page<ProductDto> getAll(String category, ProductStatus status, BigDecimal minPrice, BigDecimal maxPrice, int page, int size);

    // Offset pagination with sorting
    Page<ProductDto> getAllWithPagination(
            String name,
            String category,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sort
    );

    //  Cursor-based pagination
    CursorPageResponse<ProductDto> getAllWithCursor(
            String name,
            String category,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Long cursor,
            int size,
            String sort
    );

    // Low stock report
    List<ProductDto> getLowStockProducts(int threshold);

    ProductDto getById(Long id);

    ProductDto update(Long id, UpdateProductRequest request);

    ProductDto adjustStock(Long id, int delta);

    void delete(Long id);

    ProductDto uploadProductImage(Long id, MultipartFile file);
}