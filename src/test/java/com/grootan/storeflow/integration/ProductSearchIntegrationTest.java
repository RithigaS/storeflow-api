package com.grootan.storeflow.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_filter_by_name() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("name", "lap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void should_filter_by_price_range() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("minPrice", "100")
                        .param("maxPrice", "100000"))
                .andExpect(status().isOk());
    }

    @Test
    void should_filter_by_category_and_status() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("category", "electronics")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    void should_handle_empty_filters() throws Exception {
        mockMvc.perform(get("/api/products/paginated"))
                .andExpect(status().isOk());
    }
}