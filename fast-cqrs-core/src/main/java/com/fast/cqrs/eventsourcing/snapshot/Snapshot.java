package com.fast.cqrs.eventsourcing.snapshot;

import java.time.Instant;

/**
 * Snapshot of an aggregate's state at a specific version.
 * <p>
 * Snapshots improve performance by reducing the number of events
 * that need to be replayed when loading an aggregate.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * Snapshot<OrderState> snapshot = Snapshot.of(
 *         "order-123",
 *         "Order",
 *         100,
 *         orderState);
 * }</pre>
 */
public record Snapshot<T>(
        String aggregateId,
        String aggregateType,
        long version,
        T state,
        Instant createdAt) {

    public Snapshot {
        if (aggregateId == null)
            throw new IllegalArgumentException("aggregateId required");
        if (aggregateType == null)
            throw new IllegalArgumentException("aggregateType required");
        if (version < 0)
            throw new IllegalArgumentException("version must be >= 0");
        if (createdAt == null)
            createdAt = Instant.now();
    }

    /**
     * Creates a snapshot with current timestamp.
     */
    public static <T> Snapshot<T> of(String aggregateId, String aggregateType, long version, T state) {
        return new Snapshot<>(aggregateId, aggregateType, version, state, Instant.now());
    }
}
