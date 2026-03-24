package com.grootan.storeflow.unit.controller;

import com.grootan.storeflow.controller.ProductController;
import com.grootan.storeflow.dto.AdjustStockRequest;
import com.grootan.storeflow.dto.ApiPageResponse;
import com.grootan.storeflow.dto.CreateProductRequest;
import com.grootan.storeflow.dto.ProductDto;
import com.grootan.storeflow.dto.UpdateProductRequest;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        productDto = mock(ProductDto.class);
    }

    @Test
    void createShouldCallServiceAndReturnProduct() {
        CreateProductRequest request = new CreateProductRequest();
        when(productService.create(request)).thenReturn(productDto);

        ProductDto result = productController.create(request);

        assertNotNull(result);
        verify(productService).create(request);
    }

    @Test
    void getAllShouldReturnApiPageResponse() {
        Page<ProductDto> pageResult = new PageImpl<>(List.of(productDto));

        when(productService.getAll(
                "Electronics",
                ProductStatus.ACTIVE,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                0,
                10
        )).thenReturn(pageResult);

        ApiPageResponse<ProductDto> result = productController.getAll(
                "Electronics",
                ProductStatus.ACTIVE,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                0,
                10
        );

        assertNotNull(result);
        assertNotNull(result.content());
        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());

        verify(productService).getAll(
                "Electronics",
                ProductStatus.ACTIVE,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(1000),
                0,
                10
        );
    }

    @Test
    void getByIdShouldReturnProduct() {
        when(productService.getById(1L)).thenReturn(productDto);

        ProductDto result = productController.getById(1L);

        assertNotNull(result);
        verify(productService).getById(1L);
    }

    @Test
    void updateShouldCallServiceAndReturnUpdatedProduct() {
        UpdateProductRequest request = new UpdateProductRequest();
        when(productService.update(1L, request)).thenReturn(productDto);

        ProductDto result = productController.update(1L, request);

        assertNotNull(result);
        verify(productService).update(1L, request);
    }

    @Test
    void adjustStockShouldCallServiceWithDeltaAndReturnUpdatedProduct() {
        AdjustStockRequest request = new AdjustStockRequest();
        request.setDelta(5);

        when(productService.adjustStock(1L, 5)).thenReturn(productDto);

        ProductDto result = productController.adjustStock(1L, request);

        assertNotNull(result);
        verify(productService).adjustStock(1L, 5);
    }

    @Test
    void deleteShouldCallService() {
        doNothing().when(productService).delete(1L);

        productController.delete(1L);

        verify(productService).delete(1L);
    }
}