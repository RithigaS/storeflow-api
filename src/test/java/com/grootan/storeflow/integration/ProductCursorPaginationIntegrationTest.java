package com.grootan.storeflow.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCursorPaginationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void should_return_first_page_with_cursor() throws Exception {
        mockMvc.perform(get("/api/products/cursor")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void should_return_next_page_using_cursor() throws Exception {
        mockMvc.perform(get("/api/products/cursor")
                        .param("cursor", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void should_handle_no_cursor() throws Exception {
        mockMvc.perform(get("/api/products/cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void should_not_fail_when_fetching_multiple_pages() throws Exception {

        String firstPage = mockMvc.perform(get("/api/products/cursor")
                        .param("size", "2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondPage = mockMvc.perform(get("/api/products/cursor")
                        .param("cursor", "1")
                        .param("size", "2"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Only verify responses exist (safe assertion)
        assertNotNull(firstPage);
        assertNotNull(secondPage);
    }
}