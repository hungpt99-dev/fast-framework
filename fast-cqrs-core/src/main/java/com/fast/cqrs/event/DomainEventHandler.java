package com.fast.cqrs.event;

/**
 * Interface for handling domain events.
 * <p>
 * Implement this interface to create event handlers.
 * <p>
 * Example:
 * <pre>{@code
 * @Component
 * public class OrderCreatedHandler implements DomainEventHandler<OrderCreatedEvent> {
 *     @Override
 *     public void handle(OrderCreatedEvent event) {
 *         // Send notification, update read model, etc.
 *     }
 * }
 * }</pre>
 *
 * @param <E> the event type
 */
public interface DomainEventHandler<E extends DomainEvent> {
    
    /**
     * Handles the event.
     *
     * @param event the event to handle
     */
    void handle(E event);
}
