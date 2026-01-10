package com.example.order.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Command for creating a new order.
 */
public record CreateOrderCmd(
    String customerId,
    BigDecimal total
) {}
