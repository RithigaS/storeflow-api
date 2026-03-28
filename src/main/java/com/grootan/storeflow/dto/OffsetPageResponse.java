package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Offset-based paginated response with metadata")
public class OffsetPageResponse<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Total number of elements", example = "120")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "6")
    private int totalPages;

    @Schema(description = "Current page number", example = "0")
    private int page;

    @Schema(description = "Number of items per page", example = "20")
    private int size;

    @Schema(description = "Whether this is the first page", example = "true")
    private boolean first;

    @Schema(description = "Whether this is the last page", example = "false")
    private boolean last;

    @Schema(description = "Indicates if there is a next page", example = "true")
    private boolean hasNext;

    public OffsetPageResponse(Page<T> pageData) {
        this.content = pageData.getContent();
        this.totalElements = pageData.getTotalElements();
        this.totalPages = pageData.getTotalPages();
        this.page = pageData.getNumber();
        this.size = pageData.getSize();
        this.first = pageData.isFirst();
        this.last = pageData.isLast();
        this.hasNext = pageData.hasNext();
    }

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}