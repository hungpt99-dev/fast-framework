package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import java.util.List;
import com.example.order.handler.CreateOrderHandler;
import com.example.order.handler.GetOrderHandler;
import com.example.order.handler.GetOrdersByCustomerHandler;
import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.HttpController;
import com.fast.cqrs.cqrs.annotation.Query;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller demonstrating GraalVM-compatible direct handler invocation.
 * <p>
 * Features demonstrated:
 * <ul>
 *   <li>Explicit handler binding (GraalVM compatible)</li>
 *   <li>Command idempotency with TTL</li>
 *   <li>Query result caching with TTL</li>
 * </ul>
 */
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    /**
     * Create a new order.
     * <p>
     * Idempotent: Same requestId within 24h will return cached result.
     */
    @PostMapping
    @Command(
        handler = CreateOrderHandler.class,
        idempotencyKey = "#cmd.requestId",
        idempotencyTtl = "24h"
    )
    void createOrder(@jakarta.validation.Valid @RequestBody CreateOrderCmd cmd);

    /**
     * Get order by ID.
     * <p>
     * Cached for 5 minutes to reduce database load.
     */
    @GetMapping("/{id}")
    @Query(
        handler = GetOrderHandler.class,
        cache = "5m",
        cacheKey = "#query.id"
    )
    OrderDto getOrder(@ModelAttribute GetOrderQuery query);

    /**
     * List orders by customer.
     * <p>
     * Cached for 1 minute with customer-specific cache key.
     */
    @GetMapping
    @Query(
        handler = GetOrdersByCustomerHandler.class,
        cache = "1m",
        cacheKey = "#query.customerId"
    )
    List<OrderDto> listOrders(@ModelAttribute GetOrdersByCustomerQuery query);
}

