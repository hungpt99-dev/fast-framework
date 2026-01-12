package com.fast.cqrs.eventsourcing.snapshot;

import java.util.Optional;

/**
 * Store for aggregate snapshots.
 * <p>
 * Provides persistence for aggregate state snapshots to optimize
 * aggregate loading performance.
 */
public interface SnapshotStore {

    /**
     * Saves a snapshot.
     */
    <T> void save(Snapshot<T> snapshot);

    /**
     * Loads the latest snapshot for an aggregate.
     */
    <T> Optional<Snapshot<T>> load(String aggregateId, Class<T> stateType);

    /**
     * Loads a snapshot at a specific version.
     */
    <T> Optional<Snapshot<T>> loadAt(String aggregateId, long version, Class<T> stateType);

    /**
     * Deletes all snapshots for an aggregate.
     */
    void delete(String aggregateId);

    /**
     * Deletes snapshots older than a specific version.
     */
    void deleteOlderThan(String aggregateId, long version);
}
