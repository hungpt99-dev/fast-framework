package com.fast.cqrs.sql.repository;

import java.util.List;

/**
 * A page of results with pagination metadata.
 *
 * @param <T> the content type
 */
public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    /**
     * Creates a page from content and pagination info.
     */
    public static <T> Page<T> of(List<T> content, Pageable pageable, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.size());
        return new Page<>(content, pageable.page(), pageable.size(), totalElements, totalPages);
    }

    /**
     * Returns true if this is the first page.
     */
    public boolean isFirst() {
        return pageNumber == 0;
    }

    /**
     * Returns true if this is the last page.
     */
    public boolean isLast() {
        return pageNumber >= totalPages - 1;
    }

    /**
     * Returns true if there is a next page.
     */
    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    /**
     * Returns true if there is a previous page.
     */
    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    /**
     * Returns the number of elements in this page.
     */
    public int getNumberOfElements() {
        return content.size();
    }
}
