package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Generic paginated API response")
public record ApiPageResponse<T>(

        @Schema(description = "List of items in the current page")
        List<T> content,

        @Schema(description = "Current page number", example = "0")
        int page,

        @Schema(description = "Number of items per page", example = "10")
        int size,

        @Schema(description = "Total number of elements", example = "125")
        long totalElements,

        @Schema(description = "Total number of pages", example = "13")
        int totalPages

) {}