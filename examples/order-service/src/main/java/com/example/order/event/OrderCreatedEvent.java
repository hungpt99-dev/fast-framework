package com.example.order.event;

import com.fast.cqrs.event.DomainEvent;

import java.math.BigDecimal;

/**
 * Event published when an order is created.
 */
public class OrderCreatedEvent extends DomainEvent {
    
    private final String customerId;
    private final BigDecimal total;

    public OrderCreatedEvent(String aggregateId, String customerId, BigDecimal total) {
        super(aggregateId);
        this.customerId = customerId;
        this.total = total;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
