package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import com.fast.cqrs.annotation.CacheableQuery;
import com.fast.cqrs.annotation.Command;
import com.fast.cqrs.annotation.HttpController;
import com.fast.cqrs.annotation.Metrics;
import com.fast.cqrs.annotation.Query;
import com.fast.cqrs.annotation.RetryCommand;

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

/**
 * Order API Controller demonstrating CQRS features.
 * <p>
 * REST conventions:
 * - @Query + @GetMapping for reads (handler is optional)
 * - @Command + @PostMapping for writes
 * <p>
 * Query endpoints use @ModelAttribute to bind query parameters.
 * The framework auto-detects @ModelAttribute and dispatches to QueryBus.
 */
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    /**
     * Get order by ID.
     * <p>
     * Handler is optional - QueryBus finds handler by query type (GetOrderQuery).
     */
    @CacheableQuery(ttl = "5m")
    @Metrics(name = "orders.get")
    @Query
    @GetMapping("/{id}")
    OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);

    /**
     * Get orders by customer with pagination.
     * <p>
     * Uses @ModelAttribute to bind query parameters from URL.
     */
    @CacheableQuery(ttl = "1m")
    @Query
    @GetMapping
    List<OrderDto> getOrdersByCustomer(@ModelAttribute GetOrdersByCustomerQuery query);

    /**
     * Create a new order.
     */
    @RetryCommand(maxAttempts = 3, backoff = "100ms")
    @Metrics(name = "orders.create")
    @Command
    @PostMapping
    void createOrder(@Valid @RequestBody CreateOrderCmd cmd);
}
