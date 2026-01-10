package com.example.order.dto;

import java.math.BigDecimal;

/**
 * Order DTO for API responses.
 */
public record OrderDto(
    String id,
    String customerId,
    BigDecimal total,
    String status,
    String createdAt
) {}
