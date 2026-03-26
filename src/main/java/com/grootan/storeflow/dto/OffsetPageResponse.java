package com.grootan.storeflow.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class OffsetPageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
    private boolean first;
    private boolean last;
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