package com.example.order.event;

import com.fast.cqrs.event.DomainEvent;

/**
 * Event published when an order is shipped.
 */
public class OrderShippedEvent extends DomainEvent {

    public OrderShippedEvent(String aggregateId) {
        super(aggregateId);
    }
}
