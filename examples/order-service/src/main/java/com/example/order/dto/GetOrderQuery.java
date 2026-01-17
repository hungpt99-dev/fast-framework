package com.example.order.dto;

/**
 * Query for fetching an order by ID.
 * <p>
 * Used with @ModelAttribute for query parameter binding.
 * Additional filter parameters can be added here.
 */
public record GetOrderQuery(
        String id,
        Boolean includeItems,
        Boolean includeCustomer) {
    /**
     * Compact constructor with defaults.
     */
    public GetOrderQuery {
        if (includeItems == null)
            includeItems = false;
        if (includeCustomer == null)
            includeCustomer = false;
    }
    public GetOrderQuery(String id) {
        this(id, false, false);
    }
}
