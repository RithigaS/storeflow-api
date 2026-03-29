package com.grootan.storeflow.unit.service;

import com.grootan.storeflow.dto.CreateProductRequest;
import com.grootan.storeflow.dto.UpdateProductRequest;
import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.exception.AppException;
import com.grootan.storeflow.exception.ResourceNotFoundException;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
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

    @Mock
    private FileStorageService fileStorageService;

    private ProductServiceImpl productService;

    private CreateProductRequest request;
    private UpdateProductRequest updateRequest;
    private Category category;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(
                productRepository,
                categoryRepository,
                fileStorageService
        );

        request = new CreateProductRequest();
        request.setName("Laptop");
        request.setDescription("Gaming laptop");
        request.setSku("LAP-001");
        request.setPrice(BigDecimal.valueOf(1000));
        request.setStockQuantity(5);
        request.setCategoryId(1L);

        updateRequest = new UpdateProductRequest();
        updateRequest.setName("Updated Laptop");
        updateRequest.setDescription("Updated description");
        updateRequest.setSku("LAP-002");
        updateRequest.setPrice(BigDecimal.valueOf(1500));
        updateRequest.setStockQuantity(8);
        updateRequest.setCategoryId(1L);

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
    void createShouldSetOutOfStockWhenStockIsZero() {
        request.setStockQuantity(0);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(101L);
            return p;
        });

        var result = productService.create(request);

        assertNotNull(result);
        verify(productRepository).save(any(Product.class));

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals(ProductStatus.OUT_OF_STOCK, captor.getValue().getStatus());
    }

    @Test
    void createShouldThrowWhenCategoryNotFound() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.create(request));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getByIdShouldReturnProduct() {
        Product product = new Product();
        product.setId(5L);
        product.setName("Phone");
        product.setCategory(category);
        product.setPrice(BigDecimal.valueOf(500));
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        var result = productService.getById(5L);

        assertNotNull(result);
        assertEquals(5L, result.id());
    }

    @Test
    void getByIdShouldThrowWhenProductNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getById(5L));
    }

    @Test
    void updateShouldUpdateProductFields() {
        Product product = new Product();
        product.setId(5L);
        product.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = productService.update(5L, updateRequest);

        assertNotNull(result);
        assertEquals("Updated Laptop", product.getName());
        assertEquals("Updated description", product.getDescription());
        assertEquals("LAP-002", product.getSku());
        assertEquals(BigDecimal.valueOf(1500), product.getPrice());
        assertEquals(8, product.getStockQuantity());
        assertEquals(category, product.getCategory());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    void updateShouldSetOutOfStockWhenUpdatedStockIsZero() {
        Product product = new Product();
        product.setId(5L);

        updateRequest.setStockQuantity(0);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.update(5L, updateRequest);

        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    void updateShouldThrowWhenProductNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(5L, updateRequest));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateShouldThrowWhenCategoryNotFound() {
        Product product = new Product();
        product.setId(5L);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.update(5L, updateRequest));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void adjustStockShouldIncreaseStock() {
        Product product = new Product();
        product.setId(5L);
        product.setStockQuantity(5);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = productService.adjustStock(5L, 3);

        assertNotNull(result);
        assertEquals(8, product.getStockQuantity());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    void adjustStockShouldSetOutOfStockWhenStockBecomesZero() {
        Product product = new Product();
        product.setId(5L);
        product.setStockQuantity(5);
        product.setStatus(ProductStatus.ACTIVE);
        product.setCategory(category);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.adjustStock(5L, -5);

        assertEquals(0, product.getStockQuantity());
        assertEquals(ProductStatus.OUT_OF_STOCK, product.getStatus());
    }

    @Test
    void adjustStockShouldThrowWhenStockBecomesNegative() {
        Product product = new Product();
        product.setId(5L);
        product.setStockQuantity(2);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        assertThrows(AppException.class, () -> productService.adjustStock(5L, -3));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void adjustStockShouldThrowWhenProductNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.adjustStock(5L, 2));
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

    @Test
    void deleteShouldThrowWhenProductNotFound() {
        when(productRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.delete(5L));

        verify(productRepository, never()).save(any(Product.class));
        verify(productRepository, never()).delete(any(Product.class));
    }

    @Test
    void getAllWithPaginationShouldHandleSizeEdgeCasesAndDefaultSort() {
        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        productService.getAllWithPagination(null, null, null, null, null, 0, 0, null);
        productService.getAllWithPagination(null, null, null, null, null, 0, 200, null);

        verify(productRepository, times(2)).findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllWithPaginationShouldHandleSortAscAndDesc() {
        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        productService.getAllWithPagination(null, null, null, null, null, 0, 10, "price,asc");
        productService.getAllWithPagination(null, null, null, null, null, 0, 10, "name,desc");

        verify(productRepository, times(2)).findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        );
    }

    @Test
    void getAllWithCursorShouldHandleNullCursor() {
        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var result = productService.getAllWithCursor(
                null, null, null, null, null,
                null, 5, null
        );

        assertNotNull(result);
        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(0, result.getSize());
    }

    @Test
    void getAllWithCursorShouldHandleNonNullCursorAndHasMore() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("P1");
        p1.setCategory(category);
        p1.setPrice(BigDecimal.valueOf(100));
        p1.setStockQuantity(5);
        p1.setStatus(ProductStatus.ACTIVE);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("P2");
        p2.setCategory(category);
        p2.setPrice(BigDecimal.valueOf(200));
        p2.setStockQuantity(5);
        p2.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(p1, p2)));

        var result = productService.getAllWithCursor(
                null, null, null, null, null,
                1L, 1, null
        );

        assertTrue(result.isHasMore());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getNextCursor());
    }

    @Test
    void getAllWithCursorShouldHandleNoMoreData() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("P1");
        p1.setCategory(category);
        p1.setPrice(BigDecimal.valueOf(100));
        p1.setStockQuantity(5);
        p1.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(p1)));

        var result = productService.getAllWithCursor(
                null, null, null, null, null,
                null, 5, null
        );

        assertFalse(result.isHasMore());
        assertEquals(1, result.getSize());
        assertEquals(1L, result.getNextCursor());
    }

    @Test
    void getAllWithCursorShouldHandleEmptyList() {
        when(productRepository.findAll(
                Mockito.<Specification<Product>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of()));

        var result = productService.getAllWithCursor(
                null, null, null, null, null,
                null, 5, null
        );

        assertFalse(result.isHasMore());
        assertNull(result.getNextCursor());
        assertEquals(0, result.getSize());
    }

    @Test
    void getLowStockProductsShouldReturnList() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Low Stock");
        p.setCategory(category);
        p.setPrice(BigDecimal.valueOf(100));
        p.setStockQuantity(2);
        p.setStatus(ProductStatus.ACTIVE);

        when(productRepository.findLowStockProducts(5)).thenReturn(List.of(p));

        var result = productService.getLowStockProducts(5);

        assertEquals(1, result.size());
    }

    @Test
    void getLowStockProductsShouldReturnEmptyList() {
        when(productRepository.findLowStockProducts(5)).thenReturn(List.of());

        var result = productService.getLowStockProducts(5);

        assertTrue(result.isEmpty());
    }
}