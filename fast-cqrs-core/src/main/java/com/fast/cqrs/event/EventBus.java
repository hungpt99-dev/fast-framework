package com.fast.cqrs.event;

/**
 * Event bus for publishing and subscribing to domain events.
 * <p>
 * Example:
 * <pre>{@code
 * @Autowired
 * private EventBus eventBus;
 * 
 * public void createOrder(CreateOrderCmd cmd) {
 *     Order order = orderRepository.save(toOrder(cmd));
 *     eventBus.publish(new OrderCreatedEvent(order.getId()));
 * }
 * }</pre>
 */
public interface EventBus {
    
    /**
     * Publishes an event to all registered handlers.
     *
     * @param event the event to publish
     * @param <E>   the event type
     */
    <E extends DomainEvent> void publish(E event);
    
    /**
     * Registers an event handler.
     *
     * @param eventType the event class
     * @param handler   the handler
     * @param <E>       the event type
     */
    <E extends DomainEvent> void subscribe(Class<E> eventType, DomainEventHandler<E> handler);
}
