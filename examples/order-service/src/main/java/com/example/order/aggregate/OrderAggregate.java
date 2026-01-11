package com.example.order.aggregate;

import com.example.order.event.OrderCreatedEvent;
import com.example.order.event.OrderShippedEvent;
import com.fast.cqrs.eventsourcing.Aggregate;
import com.fast.cqrs.eventsourcing.ApplyEvent;
import com.fast.cqrs.eventsourcing.EventSourced;

import java.math.BigDecimal;

/**
 * Order aggregate demonstrating event sourcing.
 * 
 * All state changes happen through events.
 */
@EventSourced
public class OrderAggregate extends Aggregate {

    private String status;
    private String customerId;
    private BigDecimal total;

    public OrderAggregate() {
        super();
    }

    public OrderAggregate(String id) {
        super(id);
    }

    /**
     * Creates a new order.
     */
    public void create(String customerId, BigDecimal total) {
        if (this.status != null) {
            throw new IllegalStateException("Order already created");
        }
        apply(new OrderCreatedEvent(getId(), customerId, total));
    }

    /**
     * Ships the order.
     */
    public void ship() {
        if (!"PAID".equals(status)) {
            throw new IllegalStateException("Order must be paid before shipping");
        }
        apply(new OrderShippedEvent(getId()));
    }

    /**
     * Marks order as paid.
     */
    public void pay() {
        if (!"CREATED".equals(status)) {
            throw new IllegalStateException("Invalid state for payment");
        }
        this.status = "PAID";
    }

    // Event handlers - update state based on events
    
    @ApplyEvent
    private void on(OrderCreatedEvent event) {
        this.status = "CREATED";
        this.customerId = event.getCustomerId();
        this.total = event.getTotal();
    }

    @ApplyEvent
    private void on(OrderShippedEvent event) {
        this.status = "SHIPPED";
    }

    // Getters
    public String getStatus() { return status; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getTotal() { return total; }
}
