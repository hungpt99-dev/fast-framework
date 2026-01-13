package com.fast.cqrs.sql.repository;

/**
 * Pagination request parameters.
 *
 * @param page page number (0-indexed)
 * @param size page size
 * @param sort optional sorting
 */
public record Pageable(int page, int size, Sort sort) {

    /**
     * Creates a pageable with default sorting.
     */
    public Pageable(int page, int size) {
        this(page, size, Sort.unsorted());
    }

    /**
     * Creates a pageable for the first page.
     */
    public static Pageable of(int page, int size) {
        return new Pageable(page, size);
    }

    /**
     * Creates a pageable with sorting.
     */
    public static Pageable of(int page, int size, Sort sort) {
        return new Pageable(page, size, sort);
    }

    /**
     * Returns the offset for SQL OFFSET clause.
     */
    public int getOffset() {
        return page * size;
    }
}
