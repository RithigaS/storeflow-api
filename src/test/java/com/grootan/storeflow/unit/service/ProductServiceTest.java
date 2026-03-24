package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.CreateProductRequest;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private CreateProductRequest request;
    private Category category;

    @BeforeEach
    void setUp() {
        request = new CreateProductRequest();
        request.setName("Laptop");
        request.setDescription("Gaming laptop");
        request.setSku("LAP-001");
        request.setPrice(BigDecimal.valueOf(1000));
        request.setStockQuantity(5);
        request.setCategoryId(1L);

        category = new Category();
        category.setId(1L);
        category.setName("Electronics");
    }

    @Test
    void createShouldFlowDataToRepository() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        var result = productService.create(request);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();

        assertEquals("Laptop", saved.getName());
        assertEquals("Gaming laptop", saved.getDescription());
        assertEquals("LAP-001", saved.getSku());
        assertEquals(BigDecimal.valueOf(1000), saved.getPrice());
        assertEquals(5, saved.getStockQuantity());
        assertEquals(category, saved.getCategory());
        assertEquals(ProductStatus.ACTIVE, saved.getStatus());
        assertEquals(100L, result.id());
    }

    @Test
    void createShouldThrowWhenCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void deleteShouldSoftDeleteInsteadOfRemovingRow() {
        Product product = new Product();
        product.setId(5L);
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        productService.delete(5L);

        assertEquals(ProductStatus.DISCONTINUED, product.getStatus());
        assertNotNull(product.getDeletedAt());

        verify(productRepository).save(product);
        verify(productRepository, never()).delete(any(Product.class));
    }
}