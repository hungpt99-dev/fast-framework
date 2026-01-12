package com.example.order.event;

import com.fast.cqrs.event.DomainEvent;

public class OrderPaidEvent extends DomainEvent {

    public OrderPaidEvent(String aggregateId) {
        super(aggregateId);
    }

    public OrderPaidEvent() {
        // for serialization
    }
}
