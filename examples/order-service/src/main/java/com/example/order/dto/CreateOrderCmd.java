package com.example.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * Command for creating a new order.
 * <p>
 * Includes:
 * <ul>
 *   <li>{@code requestId} for idempotency - same requestId executes once within TTL</li>
 *   <li>Bean Validation for input validation</li>
 * </ul>
 */
public record CreateOrderCmd(
    String requestId,   // Idempotency key (optional)
    
    String orderId,     // Optional, auto-generated if not provided
    
    @NotBlank(message = "Customer ID is required")
    String customerId,
    
    @NotNull(message = "Order total is required")
    @DecimalMin(value = "0.01", message = "Order total must be positive")
    BigDecimal total
) {}


