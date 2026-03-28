package com.grootan.storeflow.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Cursor-based paginated response for efficient large dataset handling")
public class CursorPageResponse<T> {

    @Schema(description = "List of items returned in the current response")
    private List<T> content;

    @Schema(description = "Cursor value for fetching the next set of results", example = "100")
    private Long nextCursor;

    @Schema(description = "Indicates if more data is available", example = "true")
    private boolean hasMore;

    @Schema(description = "Number of items returned", example = "20")
    private int size;

    public CursorPageResponse(List<T> content, Long nextCursor, boolean hasMore, int size) {
        this.content = content;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.size = size;
    }

    public List<T> getContent() {
        return content;
    }

    public Long getNextCursor() {
        return nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getSize() {
        return size;
    }
}