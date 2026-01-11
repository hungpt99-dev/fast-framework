package com.example.order.event;

import com.fast.cqrs.event.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handles OrderCreatedEvent - sends notifications, updates analytics, etc.
 */
@Component
public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedHandler.class);

    @Override
    public void handle(OrderCreatedEvent event) {
        log.info("Order created: id={}, customer={}, total={}", 
                 event.getAggregateId(), 
                 event.getCustomerId(), 
                 event.getTotal());
        
        // Send confirmation email
        // Update analytics
        // Notify external systems
    }
}
