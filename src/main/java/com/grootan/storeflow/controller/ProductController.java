package com.grootan.storeflow.controller;

import com.grootan.storeflow.dto.*;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Controller", description = "APIs for managing products, filtering, pagination, stock, and images")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;

    public ProductController(ProductService productService, FileStorageService fileStorageService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
    }

    @Operation(summary = "Create product", description = "Creates a new product (Admin only)", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully",
                    content = @Content(schema = @Schema(implementation = ProductDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Product creation request",
                    required = true,
                    content = @Content(schema = @Schema(implementation = CreateProductRequest.class))
            )
            @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @Operation(summary = "Get all products", description = "Fetch products with filters and pagination")
    @GetMapping
    public ApiPageResponse<ProductDto> getAll(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Filter by status") @RequestParam(required = false) ProductStatus status,
            @Parameter(description = "Minimum price") @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
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

    @Operation(summary = "Offset pagination", description = "Advanced pagination with sorting")
    @GetMapping("/paginated")
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

    @Operation(summary = "Cursor pagination", description = "Efficient pagination for large datasets")
    @GetMapping("/cursor")
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

    @Operation(summary = "Get low stock products", description = "Returns products below stock threshold")
    @GetMapping("/low-stock")
    public List<ProductDto> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold
    ) {
        return productService.getLowStockProducts(threshold);
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ProductDto getById(
            @Parameter(description = "Product ID", example = "1")
            @PathVariable Long id) {
        return productService.getById(id);
    }

    @Operation(summary = "Update product", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ProductDto update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @Operation(summary = "Adjust product stock", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/stock")
    public ProductDto adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody AdjustStockRequest request) {
        return productService.adjustStock(id, request.getDelta());
    }

    @Operation(summary = "Delete product", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @Operation(summary = "Upload product image", security = @SecurityRequirement(name = "BearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDto uploadProductImage(
            @PathVariable Long id,
            @Parameter(description = "Image file (JPEG/PNG/WEBP)", required = true)
            @RequestParam("file") MultipartFile file) {
        return productService.uploadProductImage(id, file);
    }

    @Operation(summary = "Download product image")
    @GetMapping("/{id}/image")
    public ResponseEntity<InputStreamResource> downloadProductImage(@PathVariable Long id) throws IOException {
        ProductDto product = productService.getById(id);

        if (product.imageUrl() == null || product.imageUrl().isBlank()) {
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
}