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
class ProductLowStockIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_low_stock_products() throws Exception {
        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void should_handle_zero_threshold() throws Exception {
        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "0"))
                .andExpect(status().isOk());
    }

    @Test
    void should_handle_high_threshold() throws Exception {
        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "1000"))
                .andExpect(status().isOk());
    }
}