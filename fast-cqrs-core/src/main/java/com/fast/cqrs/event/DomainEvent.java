package com.fast.cqrs.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for domain events.
 * <p>
 * All domain events should extend this class.
 * <p>
 * Example:
 * <pre>{@code
 * public class OrderCreatedEvent extends DomainEvent {
 *     private final String orderId;
 *     private final String customerId;
 *     
 *     public OrderCreatedEvent(String orderId, String customerId) {
 *         this.orderId = orderId;
 *         this.customerId = customerId;
 *     }
 * }
 * }</pre>
 */
public abstract class DomainEvent {
    
    private final String eventId;
    private final Instant occurredAt;
    private final String aggregateId;

    protected DomainEvent() {
        this(null);
    }

    protected DomainEvent(String aggregateId) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.aggregateId = aggregateId;
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return this.getClass().getSimpleName();
    }
}
