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
 * Each method specifies an explicit handler for zero-reflection dispatch.
 */
@HttpController
@RequestMapping("/api/orders")
public interface OrderController {

    @PostMapping
    @Command(handler = CreateOrderHandler.class)
    void createOrder(@RequestBody CreateOrderCmd cmd);

    @GetMapping("/{id}")
    @Query(handler = GetOrderHandler.class)
    OrderDto getOrder(@ModelAttribute GetOrderQuery query);

    @GetMapping
    @Query(handler = GetOrdersByCustomerHandler.class)
    List<OrderDto> listOrders(@ModelAttribute GetOrdersByCustomerQuery query);
}
