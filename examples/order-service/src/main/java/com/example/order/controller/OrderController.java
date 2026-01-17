package com.example.order.controller;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.dto.GetOrderQuery;
import com.example.order.dto.OrderDto;
import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.HttpController;
import com.fast.cqrs.cqrs.annotation.Query;
import org.springframework.web.bind.annotation.*;

@HttpController
@RequestMapping("/orders")
public interface OrderController {

    @PostMapping
    @Command
    void createOrder(@RequestBody CreateOrderCmd cmd);

    @GetMapping("/{id}")
    @Query(cache = "10s")
    OrderDto getOrder(@PathVariable String id);
}
