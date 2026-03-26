package com.grootan.storeflow.dto;

import java.util.List;

public class CursorPageResponse<T> {

    private List<T> content;
    private Long nextCursor;
    private boolean hasMore;
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