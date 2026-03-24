package com.grootan.storeflow.dto;

import java.util.List;

public record ApiPageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}