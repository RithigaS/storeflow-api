package com.grootan.storeflow.integration.repository;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryRepositoryTest extends TestContainerConfig {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void shouldSaveCategorySuccessfully() {
        String name = "Electronics-" + System.nanoTime();

        Category category = new Category();
        category.setName(name);
        category.setDescription("Electronic items");

        Category saved = categoryRepository.saveAndFlush(category);

        assertNotNull(saved.getId());
        assertEquals(name, saved.getName());
    }

    @Test
    void shouldFindCategoryByNameIgnoreCase() {
        String name = "Books-" + System.nanoTime();

        Category category = new Category();
        category.setName(name);
        category.setDescription("Books category");
        categoryRepository.saveAndFlush(category);

        Optional<Category> found = categoryRepository.findByNameIgnoreCase(name.toUpperCase());

        assertTrue(found.isPresent());
        assertEquals(name, found.get().getName());
    }

    @Test
    void shouldPersistParentChildRelationship() {
        String parentName = "Electronics-" + System.nanoTime();
        String childName = "Mobiles-" + System.nanoTime();

        Category parent = new Category();
        parent.setName(parentName);
        parent.setDescription("Parent category");
        parent = categoryRepository.saveAndFlush(parent);

        Category child = new Category();
        child.setName(childName);
        child.setDescription("Child category");
        child.setParent(parent);
        child = categoryRepository.saveAndFlush(child);

        Category loadedChild = categoryRepository.findById(child.getId()).orElseThrow();

        assertNotNull(loadedChild.getParent());
        assertEquals(parentName, loadedChild.getParent().getName());
    }
}