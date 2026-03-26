package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.*;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.grootan.storeflow.dto.OffsetPageResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    public ProductController(ProductService productService, FileStorageService fileStorageService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public ApiPageResponse<ProductDto> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ProductDto> result = productService.getAll(category, status, minPrice, maxPrice, page, size);
        return new ApiPageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @GetMapping("/{id}")
    public ProductDto getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductDto update(@PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/stock")
    public ProductDto adjustStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request) {
        return productService.adjustStock(id, request.getDelta());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/image")
    public ProductDto uploadProductImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return productService.uploadProductImage(id, file);
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> downloadProductImage(@PathVariable Long id) throws IOException {
        ProductDto product = productService.getById(id);

        if (product.imageUrl()== null || product.imageUrl().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path filePath = fileStorageService.loadAsPath(product.imageUrl());
        String contentType = fileStorageService.detectContentType(filePath);

        InputStreamResource resource = new InputStreamResource(Files.newInputStream(filePath));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(filePath.getFileName().toString())
                                .build()
                                .toString()
                )
                .body(resource);
    }
    @GetMapping("/products/paginated")
    public OffsetPageResponse<ProductDto> getProductsWithPagination(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var result = productService.getAllWithPagination(
                name, category, status, minPrice, maxPrice, page, size, sort
        );

        return new OffsetPageResponse<>(result);
    }
    @GetMapping("/products/cursor")
    public CursorPageResponse<ProductDto> getProductsWithCursor(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return productService.getAllWithCursor(
                name, category, status, minPrice, maxPrice, cursor, size, sort
        );
    }

    @GetMapping("/admin/products/low-stock")
    public List<ProductDto> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold
    ) {
        return productService.getLowStockProducts(threshold);
    }
}