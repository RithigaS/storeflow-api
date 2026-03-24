package com.grootan.storeflow.integration;

import com.grootan.storeflow.entity.Category;
import com.grootan.storeflow.entity.Product;
import com.grootan.storeflow.entity.enums.ProductStatus;
import com.grootan.storeflow.integration.config.TestContainerConfig;
import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest extends TestContainerConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Category electronicsCategory;
    private Category booksCategory;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronicsCategory = new Category();
        electronicsCategory.setName("Electronics");
        electronicsCategory.setDescription("Electronic items");
        electronicsCategory = categoryRepository.saveAndFlush(electronicsCategory);

        booksCategory = new Category();
        booksCategory.setName("Books");
        booksCategory.setDescription("Books category");
        booksCategory = categoryRepository.saveAndFlush(booksCategory);
    }

    @Test
    void postProductsWithValidDataReturns201AndCreatedProductDto() throws Exception {
        String requestBody = """
                {
                  "name": "Laptop",
                  "description": "Gaming laptop",
                  "sku": "LAP-001",
                  "price": 1200.00,
                  "stockQuantity": 5,
                  "categoryId": %d
                }
                """.formatted(electronicsCategory.getId());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.description").value("Gaming laptop"))
                .andExpect(jsonPath("$.sku").value("LAP-001"))
                .andExpect(jsonPath("$.price").value(1200.00))
                .andExpect(jsonPath("$.stockQuantity").value(5))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.category.id").value(electronicsCategory.getId()))
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    void postProductsWithMissingRequiredFieldsReturns400WithErrorDetails() throws Exception {
        String requestBody = """
                {
                  "description": "Missing required fields"
                }
                """;

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getProductsReturnsPaginatedListWithCorrectPaginationMetadata() throws Exception {
        Product p1 = createProduct("Laptop", "LAP-100", BigDecimal.valueOf(1000), 5, ProductStatus.ACTIVE, electronicsCategory);
        Product p2 = createProduct("Mouse", "MOU-100", BigDecimal.valueOf(50), 20, ProductStatus.ACTIVE, electronicsCategory);

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getProductsFiltersResultsByCategoryCorrectly() throws Exception {
        createProduct("Laptop", "LAP-200", BigDecimal.valueOf(1500), 5, ProductStatus.ACTIVE, electronicsCategory);
        createProduct("Spring Book", "BOO-200", BigDecimal.valueOf(300), 10, ProductStatus.ACTIVE, booksCategory);

        mockMvc.perform(get("/api/products")
                        .param("category", "Electronics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].category.name").value("Electronics"))
                .andExpect(jsonPath("$.content[0].name").value("Laptop"));
    }

    @Test
    void getProductsFiltersResultsByStatusCorrectly() throws Exception {
        createProduct("Laptop", "LAP-300", BigDecimal.valueOf(1500), 5, ProductStatus.ACTIVE, electronicsCategory);
        createProduct("Old Mouse", "MOU-300", BigDecimal.valueOf(200), 0, ProductStatus.OUT_OF_STOCK, electronicsCategory);

        mockMvc.perform(get("/api/products")
                        .param("status", "OUT_OF_STOCK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("OUT_OF_STOCK"))
                .andExpect(jsonPath("$.content[0].name").value("Old Mouse"));
    }

    @Test
    void getProductsFiltersResultsByMinAndMaxPriceCorrectly() throws Exception {
        createProduct("Budget Mouse", "MOU-400", BigDecimal.valueOf(50), 15, ProductStatus.ACTIVE, electronicsCategory);
        createProduct("Premium Laptop", "LAP-400", BigDecimal.valueOf(2000), 3, ProductStatus.ACTIVE, electronicsCategory);
        createProduct("Keyboard", "KEY-400", BigDecimal.valueOf(150), 8, ProductStatus.ACTIVE, electronicsCategory);

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "100")
                        .param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Keyboard"));
    }

    @Test
    void getProductByIdWithValidIdReturnsProductWithCategoryPopulated() throws Exception {
        Product product = createProduct("Keyboard", "KEY-001", BigDecimal.valueOf(150), 4, ProductStatus.ACTIVE, electronicsCategory);

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Keyboard"))
                .andExpect(jsonPath("$.category.id").value(electronicsCategory.getId()))
                .andExpect(jsonPath("$.category.name").value("Electronics"));
    }

    @Test
    void getProductByIdWithNonExistentIdReturns404() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999999L))
                .andExpect(status().isNotFound());
    }

    @Test
    void putProductUpdatesAllFieldsCorrectlyAndReturnsUpdatedDto() throws Exception {
        Product product = createProduct("Mouse", "MOU-500", BigDecimal.valueOf(75), 10, ProductStatus.ACTIVE, electronicsCategory);

        String requestBody = """
                {
                  "name": "Updated Mouse",
                  "description": "Wireless mouse",
                  "sku": "MOU-500-UPD",
                  "price": 120.00,
                  "stockQuantity": 8,
                  "categoryId": %d
                }
                """.formatted(booksCategory.getId());

        mockMvc.perform(put("/api/products/{id}", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("Updated Mouse"))
                .andExpect(jsonPath("$.description").value("Wireless mouse"))
                .andExpect(jsonPath("$.sku").value("MOU-500-UPD"))
                .andExpect(jsonPath("$.price").value(120.00))
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.category.id").value(booksCategory.getId()))
                .andExpect(jsonPath("$.category.name").value("Books"));
    }

    @Test
    void patchProductStockCorrectlyAdjustsQuantity() throws Exception {
        Product product = createProduct("USB Cable", "USB-001", BigDecimal.valueOf(20), 5, ProductStatus.ACTIVE, electronicsCategory);

        String requestBody = """
                {
                  "delta": 3
                }
                """;

        mockMvc.perform(patch("/api/products/{id}/stock", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(8))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void patchProductStockRejectsNegativeResults() throws Exception {
        Product product = createProduct("USB Cable", "USB-002", BigDecimal.valueOf(20), 5, ProductStatus.ACTIVE, electronicsCategory);

        String requestBody = """
                {
                  "delta": -10
                }
                """;

        mockMvc.perform(patch("/api/products/{id}/stock", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchProductStockSetsStatusToOutOfStockWhenQuantityBecomesZero() throws Exception {
        Product product = createProduct("Headset", "HEA-001", BigDecimal.valueOf(300), 5, ProductStatus.ACTIVE, electronicsCategory);

        String requestBody = """
                {
                  "delta": -5
                }
                """;

        mockMvc.perform(patch("/api/products/{id}/stock", product.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity").value(0))
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));
    }

    @Test
    void deleteProductSoftDeletesProductBySettingDiscontinuedAndDeletedAt() throws Exception {
        Product product = createProduct("Monitor", "MON-001", BigDecimal.valueOf(800), 6, ProductStatus.ACTIVE, electronicsCategory);

        mockMvc.perform(delete("/api/products/{id}", product.getId()))
                .andExpect(status().isNoContent());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(ProductStatus.DISCONTINUED, updated.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getDeletedAt());
    }

    private Product createProduct(String name,
                                  String sku,
                                  BigDecimal price,
                                  int stockQuantity,
                                  ProductStatus status,
                                  Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setSku(sku);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setStatus(status);
        product.setCategory(category);
        return productRepository.saveAndFlush(product);
    }
}