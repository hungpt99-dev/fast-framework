package com.example.order.handler;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.handler.CommandHandler;
import com.fast.cqrs.logging.annotation.Loggable;
import com.fast.cqrs.util.IdGenerator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Handler for CreateOrderCmd.
 */
@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {

    private final OrderRepository orderRepository;

    public CreateOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Loggable("Creating new order")
    @Override
    public void handle(CreateOrderCmd cmd) {
        String orderId = IdGenerator.prefixedId("ORD");
        
        orderRepository.insert(
            orderId,
            cmd.customerId(),
            cmd.total(),
            "PENDING",
            LocalDateTime.now()
        );
    }
}
