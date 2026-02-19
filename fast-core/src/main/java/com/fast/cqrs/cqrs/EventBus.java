package com.fast.cqrs.cqrs;

/**
 * Bus for publishing events to handlers.
 */
public interface EventBus {

    /**
     * Publishes the given event to all registered handlers.
     *
     * @param event the event to publish
     * @param <E>   the event type
     */
    <E> void publish(E event);
}
