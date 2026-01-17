package com.example.order.handler;

import com.example.order.dto.PerformanceCmd;
import com.fast.cqrs.cqrs.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class PerformanceHandler implements CommandHandler<PerformanceCmd> {

    private final com.example.order.repository.OrderRepository repository;

    public PerformanceHandler(@org.springframework.beans.factory.annotation.Qualifier("orderRepository_FastImpl") com.example.order.repository.OrderRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public void handle(PerformanceCmd command) {
        // Benchmark with DB: Save a minimal order
        // Order(id, customerId, total, status, createdAt)
        com.example.order.entity.Order order = new com.example.order.entity.Order(
            command.id(), 
            "BENCH", 
            java.math.BigDecimal.ONE, 
            "NEW", 
            java.time.Instant.now().toString()
        );
        repository.save(order);
    }
}
