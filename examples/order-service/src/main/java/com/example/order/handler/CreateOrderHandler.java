package com.example.order.handler;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;

import com.fast.cqrs.concurrent.annotation.ConcurrentLimit;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.context.CommandContext;
import com.fast.cqrs.util.IdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Handler for CreateOrderCmd.
 */
@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderHandler.class);

    private final OrderRepository orderRepository;

    public CreateOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public boolean preHandle(CreateOrderCmd cmd, CommandContext ctx) {
        log.info("Checking permissions for user: {}", ctx.user());
        
        // Validation logic
        if (cmd.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
        
        // Simulating security check
        if ("blocked-user".equals(ctx.user())) {
            throw new RuntimeException("User is blocked");
        }
        
        return true;
    }

    @Override
    @ConcurrentLimit(permits = 5)
    public void handle(CreateOrderCmd cmd) {
        String orderId = cmd.orderId() != null ? cmd.orderId() : IdGenerator.prefixedId("ORD");

        Order order = new Order();
        order.setId(orderId);
        order.setCustomerId(cmd.customerId());
        order.setTotal(cmd.total());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now().toString());

        orderRepository.save(order);
        log.info("Order saved: {}", orderId);
    }
    
    @Override
    public void postHandle(CreateOrderCmd cmd, CommandContext ctx) {
        log.info("Audit: Order created successfully for customer {}", cmd.customerId());
        // Could send email notification here
    }
    
    @Override
    public void onError(CreateOrderCmd cmd, Throwable error, CommandContext ctx) {
        log.error("Failed to create order for customer {}: {}", cmd.customerId(), error.getMessage());
        throw new RuntimeException("Order creation failed", error);
    }
}
