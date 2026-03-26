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
class ProductPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_paginated_products() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void should_handle_large_size_limit() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("size", "500"))
                .andExpect(status().isOk());
    }

    @Test
    void should_handle_zero_size_default() throws Exception {
        mockMvc.perform(get("/api/products/paginated")
                        .param("size", "0"))
                .andExpect(status().isOk());
    }
}