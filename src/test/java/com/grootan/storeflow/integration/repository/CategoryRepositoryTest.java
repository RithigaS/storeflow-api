package com.grootan.storeflow.integration.repository;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest

class CategoryRepositoryTest extends TestContainerConfig {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldSaveCategorySuccessfully() {
        Category category = new Category();
        category.setName("Electronics");
        category.setDescription("Electronic items");

        Category saved = categoryRepository.saveAndFlush(category);

        assertNotNull(saved.getId());
        assertEquals("Electronics", saved.getName());
    }

    @Test
    void shouldFindCategoryByNameIgnoreCase() {
        Category category = new Category();
        category.setName("Books");
        category.setDescription("Books category");
        categoryRepository.saveAndFlush(category);

        Optional<Category> found = categoryRepository.findByNameIgnoreCase("BOOKS");

        assertTrue(found.isPresent());
        assertEquals("Books", found.get().getName());
    }

    @Test
    void shouldPersistParentChildRelationship() {
        Category parent = new Category();
        parent.setName("Electronics");
        parent.setDescription("Parent category");
        categoryRepository.saveAndFlush(parent);

        Category child = new Category();
        child.setName("Mobiles");
        child.setDescription("Child category");
        child.setParent(parent);
        categoryRepository.saveAndFlush(child);

        Category loadedChild = categoryRepository.findById(child.getId()).orElseThrow();

        assertNotNull(loadedChild.getParent());
        assertEquals("Electronics", loadedChild.getParent().getName());
    }
}