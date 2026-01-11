package com.fast.cqrs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory implementation of {@link EventBus}.
 * <p>
 * Synchronously dispatches events to all registered handlers.
 * For production use with high throughput, consider an async implementation.
 */
public class SimpleEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(SimpleEventBus.class);

    private final Map<Class<?>, List<DomainEventHandler<?>>> handlers = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <E extends DomainEvent> void publish(E event) {
        log.debug("Publishing event: {} (id={})", event.getEventType(), event.getEventId());

        List<DomainEventHandler<?>> eventHandlers = handlers.get(event.getClass());
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            log.debug("No handlers registered for event: {}", event.getEventType());
            return;
        }

        for (DomainEventHandler<?> handler : eventHandlers) {
            try {
                ((DomainEventHandler<E>) handler).handle(event);
                log.debug("Handler {} processed event {}", 
                          handler.getClass().getSimpleName(), event.getEventId());
            } catch (Exception e) {
                log.error("Error handling event {} in handler {}", 
                          event.getEventId(), handler.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public <E extends DomainEvent> void subscribe(Class<E> eventType, DomainEventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        log.info("Registered handler {} for event {}", 
                 handler.getClass().getSimpleName(), eventType.getSimpleName());
    }
}
