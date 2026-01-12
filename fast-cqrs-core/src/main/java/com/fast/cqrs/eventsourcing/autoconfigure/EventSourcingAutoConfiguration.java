package com.fast.cqrs.eventsourcing.autoconfigure;

import com.fast.cqrs.event.EventBus;
import com.fast.cqrs.event.SimpleEventBus;
import com.fast.cqrs.eventsourcing.EventStore;
import com.fast.cqrs.eventsourcing.InMemoryEventStore;
import com.fast.cqrs.eventsourcing.JdbcEventStore;
import com.fast.cqrs.eventsourcing.projection.ProjectionManager;
import com.fast.cqrs.eventsourcing.replay.ReplayEngine;
import com.fast.cqrs.eventsourcing.snapshot.InMemorySnapshotStore;
import com.fast.cqrs.eventsourcing.snapshot.SnapshotStore;

/**
 * Auto-configuration for Event Sourcing.
 * <p>
 * Provides default beans:
 * <ul>
 * <li>EventStore (InMemory or JDBC)</li>
 * <li>SnapshotStore</li>
 * <li>ProjectionManager</li>
 * <li>ReplayEngine</li>
 * </ul>
 * <p>
 * Configuration:
 * 
 * <pre>{@code
 * fast.eventsourcing:
 *   store: jdbc  # or memory
 *   snapshot:
 *     enabled: true
 *     strategy: event-count
 *     every-n-events: 100
 * }</pre>
 */
public class EventSourcingAutoConfiguration {

    /**
     * Creates default event store (in-memory for development).
     */
    public EventStore eventStore() {
        return new InMemoryEventStore();
    }

    /**
     * Creates default snapshot store.
     */
    public SnapshotStore snapshotStore() {
        return new InMemorySnapshotStore();
    }

    /**
     * Creates default event bus.
     */
    public EventBus eventBus() {
        return new SimpleEventBus();
    }

    /**
     * Creates projection manager.
     */
    public ProjectionManager projectionManager() {
        return new ProjectionManager();
    }

    /**
     * Creates replay engine.
     */
    public ReplayEngine replayEngine(EventStore eventStore, ProjectionManager projectionManager) {
        return new ReplayEngine(eventStore, projectionManager);
    }

    /**
     * Configuration properties.
     */
    public record EventSourcingProperties(
            String store,
            SnapshotProperties snapshot) {
        public EventSourcingProperties {
            if (store == null)
                store = "memory";
        }
    }

    public record SnapshotProperties(
            boolean enabled,
            String strategy,
            int everyNEvents) {
        public SnapshotProperties {
            if (strategy == null)
                strategy = "event-count";
            if (everyNEvents <= 0)
                everyNEvents = 100;
        }
    }
}
