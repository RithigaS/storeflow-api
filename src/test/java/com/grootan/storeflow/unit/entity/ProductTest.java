package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void shouldSetAndGetAllFields() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Electronics");

        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setDescription("Gaming laptop");
        product.setSku("LAP-123");
        product.setPrice(BigDecimal.valueOf(75000));
        product.setStockQuantity(5);
        product.setCategory(category);

        assertEquals(1L, product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals("Gaming laptop", product.getDescription());
        assertEquals("LAP-123", product.getSku());
        assertEquals(BigDecimal.valueOf(75000), product.getPrice());
        assertEquals(5, product.getStockQuantity());
        assertSame(category, product.getCategory());
    }
}