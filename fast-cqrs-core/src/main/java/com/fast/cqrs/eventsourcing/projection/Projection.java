package com.fast.cqrs.eventsourcing.projection;

import com.fast.cqrs.event.DomainEvent;

/**
 * Base interface for projections (read models).
 * <p>
 * Projections build query-optimized views from domain events.
 * <p>
 * Usage:
 * 
 * <pre>
 * {
 *     &#64;code
 *     &#64;Component
 *     public class OrderSummaryProjection implements Projection {
 * 
 *         &#64;Override
 *         public String getName() {
 *             return "order-summary";
 *         }
 * 
 *         &#64;ProjectionHandler
 *         public void on(OrderCreatedEvent event) {
 *             // Insert or update read model
 *         }
 * 
 *         @ProjectionHandler
 *         public void on(OrderShippedEvent event) {
 *             // Update status
 *         }
 *     }
 * }
 * </pre>
 */
public interface Projection {

    /**
     * Gets the unique name of this projection.
     */
    String getName();

    /**
     * Returns true if this projection can handle the given event type.
     */
    default boolean canHandle(Class<? extends DomainEvent> eventType) {
        return true;
    }

    /**
     * Called before replay starts.
     */
    default void onReplayStart() {
    }

    /**
     * Called after replay completes.
     */
    default void onReplayComplete() {
    }

    /**
     * Resets the projection state (for rebuild).
     */
    default void reset() {
    }
}
