package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;

import java.util.List;
import java.util.Optional;

/**
 * Event store for persisting and retrieving domain events.
 * <p>
 * This is the core interface for event sourcing - instead of storing
 * current state, we store all events and rebuild state by replaying them.
 */
public interface EventStore {

    /**
     * Appends events to the event stream for an aggregate.
     *
     * @param aggregateId   the aggregate ID
     * @param events        the events to append
     * @param expectedVersion expected version for optimistic locking (-1 for new)
     */
    void append(String aggregateId, List<DomainEvent> events, long expectedVersion);

    /**
     * Loads all events for an aggregate.
     *
     * @param aggregateId the aggregate ID
     * @return list of events in order
     */
    List<DomainEvent> load(String aggregateId);

    /**
     * Loads events for an aggregate from a specific version.
     *
     * @param aggregateId the aggregate ID
     * @param fromVersion starting version (exclusive)
     * @return list of events after the version
     */
    List<DomainEvent> loadFrom(String aggregateId, long fromVersion);

    /**
     * Gets the current version of an aggregate.
     *
     * @param aggregateId the aggregate ID
     * @return current version, or empty if aggregate doesn't exist
     */
    Optional<Long> getVersion(String aggregateId);
}
