package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.GetOrdersByCustomerQuery;
import com.example.order.dto.OrderDto;
import com.fast.cqrs.annotation.Command;
import com.fast.cqrs.annotation.HttpController;
import com.fast.cqrs.annotation.Query;

import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order API Controller.
 * 
 * IMPORTANT: For CQRS to work correctly:
 * - @Query methods should accept a query object as @RequestBody
 * - @Command methods should accept a command object as @RequestBody
 * 
 * This ensures the dispatcher can route to the correct handler based on type.
 */
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    /**
     * Get order by ID.
     * POST because we send query as request body.
     */
    @Query
    @PostMapping("/get")
    OrderDto getOrder(@RequestBody GetOrderQuery query);

    /**
     * Get orders by customer.
     * POST because we send query as request body.
     */
    @Query
    @PostMapping("/by-customer")
    List<OrderDto> getOrdersByCustomer(@RequestBody GetOrdersByCustomerQuery query);

    /**
     * Create a new order.
     */
    @Command
    @PostMapping
    void createOrder(@RequestBody CreateOrderCmd cmd);
}
