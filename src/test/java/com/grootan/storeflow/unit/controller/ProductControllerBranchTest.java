package com.grootan.storeflow.unit.controller;

import com.grootan.storeflow.controller.ProductController;
import com.grootan.storeflow.dto.CursorPageResponse;

import com.grootan.storeflow.dto.ProductDto;

import com.grootan.storeflow.service.FileStorageService;
import com.grootan.storeflow.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerBranchTest {

    private final ProductService productService = Mockito.mock(ProductService.class);
    private final FileStorageService fileStorageService = Mockito.mock(FileStorageService.class);

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProductController(productService, fileStorageService))
            .build();

    @Test
    void shouldHandlePaginatedWithAllParams() throws Exception {
        Mockito.when(productService.getAllWithPagination(
                        any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        mockMvc.perform(get("/api/products/paginated")
                        .param("name", "lap")
                        .param("category", "electronics")
                        .param("status", "ACTIVE")
                        .param("minPrice", "100")
                        .param("maxPrice", "1000")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "price,asc"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandlePaginatedWithNoParams() throws Exception {
        Mockito.when(productService.getAllWithPagination(
                        any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/products/paginated"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleCursorWithoutCursorParam() throws Exception {
        Mockito.when(productService.getAllWithCursor(
                        any(), any(), any(), any(), any(), isNull(), anyInt(), any()))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false, 0));

        mockMvc.perform(get("/api/products/cursor"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleCursorWithCursorParam() throws Exception {
        Mockito.when(productService.getAllWithCursor(
                        any(), any(), any(), any(), any(), anyLong(), anyInt(), any()))
                .thenReturn(new CursorPageResponse<>(List.of(), 2L, true, 1));

        mockMvc.perform(get("/api/products/cursor")
                        .param("cursor", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldHandleLowStockEndpoint() throws Exception {
        Mockito.when(productService.getLowStockProducts(anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/products/low-stock")
                        .param("threshold", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn404WhenImageNotFound() throws Exception {
        ProductDto dto = Mockito.mock(ProductDto.class);
        Mockito.when(dto.imageUrl()).thenReturn(null);

        Mockito.when(productService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/1/image"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUploadImageSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "data".getBytes()
        );

        ProductDto dto = Mockito.mock(ProductDto.class);
        Mockito.when(productService.uploadProductImage(eq(1L), any())).thenReturn(dto);

        mockMvc.perform(multipart("/api/products/1/image")
                        .file(file))
                .andExpect(status().isOk());
    }
}