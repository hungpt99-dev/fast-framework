package com.example.order.dto;

/**
 * Query for fetching orders by customer.
 */
public record GetOrdersByCustomerQuery(String customerId) {}
