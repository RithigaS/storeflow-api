package com.grootan.storeflow.unit.entity;

import com.grootan.storeflow.entity.Category;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CategoryTest {

    @Test
    void shouldSetAndGetAllFields() {
        Category category = new Category();

        category.setId(1L);
        category.setName("Books");
        category.setDescription("Books category");

        assertEquals(1L, category.getId());
        assertEquals("Books", category.getName());
        assertEquals("Books category", category.getDescription());
    }
}