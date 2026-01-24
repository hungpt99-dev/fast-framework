package com.fast.cqrs.sql.repository;

/**
 * Pagination request parameters.
 * <p>
 * Parameters are validated to ensure safe values:
 * <ul>
 *   <li>page must be >= 0</li>
 *   <li>size must be between 1 and 1000</li>
 * </ul>
 *
 * @param page page number (0-indexed)
 * @param size page size
 * @param sort optional sorting
 */
public record Pageable(int page, int size, Sort sort) {

    /**
     * Maximum allowed page size to prevent DoS attacks.
     */
    public static final int MAX_PAGE_SIZE = 1000;

    /**
     * Compact constructor with validation.
     */
    public Pageable {
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be >= 0, got: " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be >= 1, got: " + size);
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size must be <= " + MAX_PAGE_SIZE + ", got: " + size);
        }
    }

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
