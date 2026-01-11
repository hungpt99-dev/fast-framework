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
 * 
 * Features used:
 * - @Query / @Command for CQRS routing
 * - @CacheableQuery for caching
 * - @RetryCommand for retry
 * - @Metrics for observability
 * - @Valid for validation
 * 
 * Entity: Order (database)
 * DTO: OrderDto (API response)
 */
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    /**
     * Get order by ID with caching.
     */
    @CacheableQuery(ttl = "5m")
    @Metrics(name = "orders.get")
    @Query
    @PostMapping("/get")
    OrderDto getOrder(@RequestBody GetOrderQuery query);

    /**
     * Get orders by customer.
     */
    @CacheableQuery(ttl = "1m")
    @Query
    @PostMapping("/by-customer")
    List<OrderDto> getOrdersByCustomer(@RequestBody GetOrdersByCustomerQuery query);

    /**
     * Create a new order with retry and validation.
     */
    @RetryCommand(maxAttempts = 3, backoff = "100ms")
    @Metrics(name = "orders.create")
    @Command
    @PostMapping
    void createOrder(@Valid @RequestBody CreateOrderCmd cmd);
}
