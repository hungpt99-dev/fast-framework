package com.example.order.handler;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.event.OrderCreatedEvent;
import com.example.order.repository.OrderRepository;
import com.fast.cqrs.event.EventBus;
import com.fast.cqrs.handler.CommandHandler;
import com.fast.cqrs.logging.annotation.Loggable;
import com.fast.cqrs.util.IdGenerator;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

import static com.fast.cqrs.concurrent.VirtualThread.*;

/**
 * Handler for CreateOrderCmd.
 * 
 * Demonstrates:
 * - EventBus publishing
 * - VirtualThread parallel execution
 */
@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {

    private final OrderRepository orderRepository;
    private final EventBus eventBus;

    public CreateOrderHandler(OrderRepository orderRepository, EventBus eventBus) {
        this.orderRepository = orderRepository;
        this.eventBus = eventBus;
    }

    @Loggable("Creating new order")
    @Override
    public void handle(CreateOrderCmd cmd) {
        String orderId = IdGenerator.prefixedId("ORD");
        
        // Insert order
        orderRepository.insert(
            orderId,
            cmd.customerId(),
            cmd.total(),
            "PENDING",
            LocalDateTime.now()
        );
        
        // Publish event (async, non-blocking)
        eventBus.publish(new OrderCreatedEvent(orderId, cmd.customerId(), cmd.total()));
        
        // Example: VirtualThread parallel execution
        execute(() -> {
            // Background task: notify external systems
        });
    }
}
