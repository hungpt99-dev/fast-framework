package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for event-sourced aggregates.
 * <p>
 * Aggregates are the consistency boundary in event sourcing.
 * All state changes happen through events.
 * <p>
 * Example:
 * <pre>{@code
 * public class OrderAggregate extends Aggregate {
 *     private String status;
 *     private BigDecimal total;
 *     
 *     public void create(String customerId, BigDecimal total) {
 *         apply(new OrderCreatedEvent(getId(), customerId, total));
 *     }
 *     
 *     public void ship() {
 *         if (!"PAID".equals(status)) {
 *             throw new IllegalStateException("Order must be paid first");
 *         }
 *         apply(new OrderShippedEvent(getId()));
 *     }
 *     
 *     @EventHandler
 *     private void on(OrderCreatedEvent event) {
 *         this.status = "CREATED";
 *         this.total = event.getTotal();
 *     }
 *     
 *     @EventHandler
 *     private void on(OrderShippedEvent event) {
 *         this.status = "SHIPPED";
 *     }
 * }
 * }</pre>
 */
public abstract class Aggregate {

    private String id;
    private long version = -1;
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();

    protected Aggregate() {
        this.id = UUID.randomUUID().toString();
    }

    protected Aggregate(String id) {
        this.id = id;
    }

    /**
     * Gets the aggregate ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the aggregate ID (used during loading).
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the current version.
     */
    public long getVersion() {
        return version;
    }

    /**
     * Sets the version (used during loading).
     */
    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Applies an event to this aggregate.
     * <p>
     * The event is added to uncommitted events and the state is updated.
     */
    protected void apply(DomainEvent event) {
        uncommittedEvents.add(event);
        applyEvent(event);
    }

    /**
     * Applies an event without recording it (for replay).
     */
    public void replayEvent(DomainEvent event) {
        applyEvent(event);
        version++;
    }

    /**
     * Gets uncommitted events.
     */
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    /**
     * Clears uncommitted events (after saving).
     */
    public void clearUncommittedEvents() {
        uncommittedEvents.clear();
    }

    /**
     * Applies the event to update internal state.
     * Override this to dispatch to specific handler methods.
     */
    protected void applyEvent(DomainEvent event) {
        // Default implementation uses reflection to find @EventHandler methods
        EventHandlerInvoker.invoke(this, event);
    }
}
