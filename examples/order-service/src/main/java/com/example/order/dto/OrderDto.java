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
) {
    /**
     * Creates DTO from entity.
     */
    public static OrderDto fromEntity(com.example.order.entity.Order order) {
        return new OrderDto(
            order.getId(),
            order.getCustomerId(),
            order.getTotal(),
            order.getStatus(),
            order.getCreatedAt()
        );
    }
}
