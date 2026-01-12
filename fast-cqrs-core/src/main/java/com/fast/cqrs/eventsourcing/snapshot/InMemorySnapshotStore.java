package com.fast.cqrs.eventsourcing.snapshot;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of SnapshotStore for testing.
 */
public class InMemorySnapshotStore implements SnapshotStore {

    private final Map<String, Snapshot<?>> snapshots = new ConcurrentHashMap<>();

    @Override
    public <T> void save(Snapshot<T> snapshot) {
        snapshots.put(snapshot.aggregateId(), snapshot);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Snapshot<T>> load(String aggregateId, Class<T> stateType) {
        return Optional.ofNullable((Snapshot<T>) snapshots.get(aggregateId));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Snapshot<T>> loadAt(String aggregateId, long version, Class<T> stateType) {
        Snapshot<?> snapshot = snapshots.get(aggregateId);
        if (snapshot != null && snapshot.version() == version) {
            return Optional.of((Snapshot<T>) snapshot);
        }
        return Optional.empty();
    }

    @Override
    public void delete(String aggregateId) {
        snapshots.remove(aggregateId);
    }

    @Override
    public void deleteOlderThan(String aggregateId, long version) {
        Snapshot<?> snapshot = snapshots.get(aggregateId);
        if (snapshot != null && snapshot.version() < version) {
            snapshots.remove(aggregateId);
        }
    }

    /**
     * Clears all snapshots.
     */
    public void clear() {
        snapshots.clear();
    }
}
