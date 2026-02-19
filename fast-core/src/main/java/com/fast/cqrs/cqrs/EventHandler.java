package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.EventContext;

/**
 * Interface for event handlers with lifecycle hooks.
 * @param <E> the event type
 */
public interface EventHandler<E> {

    // ==================== LIFECYCLE HOOKS ====================

    default boolean preHandle(E event, EventContext ctx) {
        return true;
    }

    void handle(E event);

    default void postHandle(E event, EventContext ctx) {
    }

    default void onError(E event, Throwable error, EventContext ctx) {
        if (error instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(error);
    }

    // ==================== TYPE RESOLUTION ====================

    @SuppressWarnings("unchecked")
    default Class<E> getEventType() {
        return (Class<E>) GenericTypeResolver.resolveTypeArgument(getClass(), EventHandler.class, 0);
    }
}
