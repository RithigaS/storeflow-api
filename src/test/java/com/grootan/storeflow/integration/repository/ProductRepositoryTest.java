package com.grootan.storeflow.integration.repository;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ProductRepositoryTest extends TestContainerConfig {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Category createCategory() {
        Category category = new Category();
        category.setName("Electronics-" + UUID.randomUUID());
        category.setDescription("Electronic products");
        return categoryRepository.saveAndFlush(category);
    }

    @Test
    void shouldSaveValidProduct() {
        Category category = createCategory();

        Product product = new Product();
        product.setName("Phone");
        product.setDescription("A good smartphone");
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(BigDecimal.valueOf(500));
        product.setStockQuantity(10);
        product.setCategory(category);

        Product saved = productRepository.saveAndFlush(product);

        assertNotNull(saved.getId());
    }

    @Test
    void shouldFailValidationForNegativePrice() {
        Category category = createCategory();

        Product product = new Product();
        product.setName("Phone");
        product.setDescription("A good smartphone");
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(BigDecimal.valueOf(-1));
        product.setStockQuantity(10);
        product.setCategory(category);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldFailValidationForNegativeStockQuantity() {
        Category category = createCategory();

        Product product = new Product();
        product.setName("Laptop");
        product.setDescription("A good laptop");
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStockQuantity(-5);
        product.setCategory(category);

        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldEnforceUniqueSkuConstraint() {
        Category category = createCategory();
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8);

        Product first = new Product();
        first.setName("Phone");
        first.setDescription("First phone");
        first.setSku(sku);
        first.setPrice(BigDecimal.valueOf(500));
        first.setStockQuantity(10);
        first.setCategory(category);
        productRepository.saveAndFlush(first);

        Product second = new Product();
        second.setName("Tablet");
        second.setDescription("Second product");
        second.setSku(sku);
        second.setPrice(BigDecimal.valueOf(300));
        second.setStockQuantity(5);
        second.setCategory(category);

        assertThrows(DataIntegrityViolationException.class, () -> productRepository.saveAndFlush(second));
    }

    @Test
    void shouldFindBySkuIgnoreCase() {
        Category category = createCategory();
        String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8);

        Product product = new Product();
        product.setName("Camera");
        product.setDescription("Nice camera");
        product.setSku(sku);
        product.setPrice(BigDecimal.valueOf(700));
        product.setStockQuantity(7);
        product.setCategory(category);
        productRepository.saveAndFlush(product);

        Optional<Product> found = productRepository.findBySkuIgnoreCase(sku.toLowerCase());

        assertTrue(found.isPresent());

        assertTrue(found.get().getSku().equalsIgnoreCase(sku));
    }

    @Test
    void shouldFindByCategory() {
        Category category = createCategory();

        Product product = new Product();
        product.setName("Speaker");
        product.setDescription("Bluetooth speaker");
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setPrice(BigDecimal.valueOf(200));
        product.setStockQuantity(8);
        product.setCategory(category);
        productRepository.saveAndFlush(product);

        List<Product> products = productRepository.findByCategory(category);

        assertEquals(1, products.size());
    }

    @Test
    void shouldFindLowStockProducts() {
        Category category = createCategory();

        Product lowStockProduct = new Product();
        lowStockProduct.setName("Mouse");
        lowStockProduct.setDescription("Wireless mouse");
        lowStockProduct.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        lowStockProduct.setPrice(BigDecimal.valueOf(50));
        lowStockProduct.setStockQuantity(2);
        lowStockProduct.setCategory(category);
        productRepository.saveAndFlush(lowStockProduct);

        Product enoughStockProduct = new Product();
        enoughStockProduct.setName("Keyboard");
        enoughStockProduct.setDescription("Mechanical keyboard");
        enoughStockProduct.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        enoughStockProduct.setPrice(BigDecimal.valueOf(80));
        enoughStockProduct.setStockQuantity(20);
        enoughStockProduct.setCategory(category);
        productRepository.saveAndFlush(enoughStockProduct);

        List<Product> lowStockProducts = productRepository.findAllByStockQuantityLessThan(5);

        assertEquals(1, lowStockProducts.size());
        assertEquals(2, lowStockProducts.get(0).getStockQuantity());
    }
}