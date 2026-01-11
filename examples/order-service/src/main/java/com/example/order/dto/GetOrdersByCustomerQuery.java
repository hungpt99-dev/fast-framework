package com.example.order.dto;

/**
 * Query for fetching orders by customer with pagination.
 * <p>
 * Used with @ModelAttribute for query parameter binding.
 * Example: GET /api/orders?customerId=123&page=0&size=20&status=PENDING
 */
public record GetOrdersByCustomerQuery(
        String customerId,
        String status,
        Integer page,
        Integer size,
        String sort) {
    /**
     * Compact constructor with defaults.
     */
    public GetOrdersByCustomerQuery {
        if (page == null)
            page = 0;
        if (size == null)
            size = 20;
        if (sort == null)
            sort = "createdAt,desc";
    }
}
