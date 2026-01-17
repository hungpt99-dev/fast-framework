package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.HttpController;
import com.fast.cqrs.cqrs.annotation.Query;
import com.fast.cqrs.cqrs.gateway.CommandGateway;
import com.fast.cqrs.cqrs.gateway.QueryGateway;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * REST Controller for Orders.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public OrderController(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @PostMapping
    @Command
    public void createOrder(@RequestBody CreateOrderCmd cmd) {
        // Use fluent API for robust command dispatch
        commandGateway.with(cmd)
                .timeout(Duration.ofSeconds(5))
                .retry(3)
                .send();
    }

    @GetMapping("/{id}")
    @Query(cache = "10s")  // Annotation-based cache control
    public OrderDto getOrder(@PathVariable String id) {
        // Use fluent API for query dispatch
        return queryGateway.with(new GetOrderQuery(id))
                .timeout(Duration.ofSeconds(2))
                .execute();
    }
}
