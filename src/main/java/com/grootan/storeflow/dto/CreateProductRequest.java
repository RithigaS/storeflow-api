package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Request payload for creating a new product")
public class CreateProductRequest {

    @Schema(
            description = "Product name",
            example = "iPhone 15 Pro",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Product name is required")
    @Size(min = 3, max = 150, message = "Product name must be between 3 and 150 characters")
    private String name;

    @Schema(
            description = "Product description",
            example = "Latest Apple iPhone with advanced features"
    )
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Schema(
            description = "Stock Keeping Unit (uppercase, numbers, hyphen)",
            example = "IPHONE-15-PRO",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "SKU is required")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase letters, numbers, and hyphens")
    private String sku;

    @Schema(
            description = "Product price",
            example = "999.99",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be a positive value")
    private BigDecimal price;

    @Schema(
            description = "Available stock quantity",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @Schema(
            description = "Category ID of the product",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Category ID is required")
    @com.grootan.storeflow.validation.ExistsCategory
    private Long categoryId;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSku() {
        return sku;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}