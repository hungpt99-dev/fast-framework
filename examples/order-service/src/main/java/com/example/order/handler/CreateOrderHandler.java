package com.example.order.handler;

import com.example.order.dto.CreateOrderCmd;
import com.example.order.entity.Order;
import com.example.order.repository.OrderRepository;

import com.fast.cqrs.concurrent.annotation.ConcurrentLimit;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.context.CommandContext;
import com.fast.cqrs.logging.annotation.TraceLog;
import com.fast.cqrs.logging.annotation.Loggable;
import com.fast.cqrs.util.IdGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Handler for CreateOrderCmd.
 * <p>
 * Demonstrates:
 * <ul>
 *   <li>Lifecycle hooks (preHandle, handle, postHandle, onError)</li>
 *   <li>@TraceLog for method timing</li>
 *   <li>@Loggable for business event logging</li>
 *   <li>@ConcurrentLimit for concurrency control</li>
 * </ul>
 */
@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {

    private static final Logger log = LoggerFactory.getLogger(CreateOrderHandler.class);

    private final OrderRepository orderRepository;

    public CreateOrderHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Pre-handle: Validation and authorization.
     */
    @Override
    public boolean preHandle(CreateOrderCmd cmd, CommandContext ctx) {
        log.debug("Checking permissions for user: {}", ctx.user());
        
        // Validation logic
        if (cmd.total() == null || cmd.total().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Order total must be positive");
        }
        
        if (cmd.customerId() == null || cmd.customerId().isBlank()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
        
        // Simulating security check
        if ("blocked-user".equals(ctx.user())) {
            throw new RuntimeException("User is blocked");
        }
        
        return true;
    }

    /**
     * Handle: Core business logic with tracing and concurrency control.
     */
    @Override
    @TraceLog(slowMs = 100)
    @Loggable("Creating order")
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
    
    /**
     * Post-handle: Audit logging and notifications.
     */
    @Override
    public void postHandle(CreateOrderCmd cmd, CommandContext ctx) {
        log.info("Audit: Order created by {} for customer {}", ctx.user(), cmd.customerId());
        // Could send email notification, publish event, etc.
    }
    
    /**
     * On error: Error handling and logging.
     */
    @Override
    public void onError(CreateOrderCmd cmd, Throwable error, CommandContext ctx) {
        log.error("Failed to create order for customer {}: {}", cmd.customerId(), error.getMessage());
        throw new RuntimeException("Order creation failed", error);
    }
}

