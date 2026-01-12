package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link EventStore}.
 * <p>
 * Suitable for testing and development. For production, use a
 * persistent implementation (database, Kafka, EventStoreDB, etc.)
 */
public class InMemoryEventStore implements EventStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventStore.class);

    private final Map<String, List<DomainEvent>> eventStreams = new ConcurrentHashMap<>();
    private final Map<String, Long> versions = new ConcurrentHashMap<>();

    @Override
    public void append(String aggregateId, List<DomainEvent> events, long expectedVersion) {
        synchronized (getLock(aggregateId)) {
            long currentVersion = versions.getOrDefault(aggregateId, -1L);
            
            if (expectedVersion != -1 && currentVersion != expectedVersion) {
                throw new ConcurrencyException(
                    "Concurrency conflict for aggregate " + aggregateId +
                    ": expected version " + expectedVersion + ", but was " + currentVersion
                );
            }

            List<DomainEvent> stream = eventStreams.computeIfAbsent(aggregateId, k -> new ArrayList<>());
            stream.addAll(events);
            versions.put(aggregateId, currentVersion + events.size());

            log.debug("Appended {} events to aggregate {}, new version: {}", 
                      events.size(), aggregateId, currentVersion + events.size());
        }
    }

    @Override
    public List<DomainEvent> load(String aggregateId) {
        return new ArrayList<>(eventStreams.getOrDefault(aggregateId, List.of()));
    }

    @Override
    public List<DomainEvent> loadFrom(String aggregateId, long fromVersion) {
        List<DomainEvent> stream = eventStreams.getOrDefault(aggregateId, List.of());
        if (fromVersion >= stream.size()) {
            return List.of();
        }
        return new ArrayList<>(stream.subList((int) fromVersion + 1, stream.size()));
    }

    @Override
    public Optional<Long> getVersion(String aggregateId) {
        return Optional.ofNullable(versions.get(aggregateId));
    }

    private Object getLock(String aggregateId) {
        return aggregateId.intern();
    }

    /**
     * Clears all events (for testing).
     */
    public void clear() {
        eventStreams.clear();
        versions.clear();
    }

    /**
     * Exception thrown on concurrency conflict.
     */
    public static class ConcurrencyException extends RuntimeException {
        public ConcurrencyException(String message) {
            super(message);
        }
    }
}
