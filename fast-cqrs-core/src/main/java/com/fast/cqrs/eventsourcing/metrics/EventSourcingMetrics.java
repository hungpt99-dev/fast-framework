package com.fast.cqrs.eventsourcing.metrics;

import java.util.concurrent.atomic.LongAdder;

/**
 * Metrics for event sourcing.
 * <p>
 * Collects:
 * <ul>
 * <li>Commands processed</li>
 * <li>Events appended</li>
 * <li>Events replayed</li>
 * <li>Snapshots created/loaded</li>
 * <li>Handler latencies</li>
 * </ul>
 */
public final class EventSourcingMetrics {

    private static final LongAdder commandsProcessed = new LongAdder();
    private static final LongAdder eventsAppended = new LongAdder();
    private static final LongAdder eventsReplayed = new LongAdder();
    private static final LongAdder snapshotsCreated = new LongAdder();
    private static final LongAdder snapshotsLoaded = new LongAdder();
    private static final LongAdder aggregatesLoaded = new LongAdder();
    private static final LongAdder projectionEventsProcessed = new LongAdder();

    private EventSourcingMetrics() {
    }

    public static void recordCommandProcessed() {
        commandsProcessed.increment();
    }

    public static void recordEventsAppended(int count) {
        eventsAppended.add(count);
    }

    public static void recordEventReplayed() {
        eventsReplayed.increment();
    }

    public static void recordSnapshotCreated() {
        snapshotsCreated.increment();
    }

    public static void recordSnapshotLoaded() {
        snapshotsLoaded.increment();
    }

    public static void recordAggregateLoaded() {
        aggregatesLoaded.increment();
    }

    public static void recordProjectionEventProcessed() {
        projectionEventsProcessed.increment();
    }

    public static Stats getStats() {
        return new Stats(
                commandsProcessed.sum(),
                eventsAppended.sum(),
                eventsReplayed.sum(),
                snapshotsCreated.sum(),
                snapshotsLoaded.sum(),
                aggregatesLoaded.sum(),
                projectionEventsProcessed.sum());
    }

    public static void reset() {
        commandsProcessed.reset();
        eventsAppended.reset();
        eventsReplayed.reset();
        snapshotsCreated.reset();
        snapshotsLoaded.reset();
        aggregatesLoaded.reset();
        projectionEventsProcessed.reset();
    }

    public record Stats(
            long commandsProcessed,
            long eventsAppended,
            long eventsReplayed,
            long snapshotsCreated,
            long snapshotsLoaded,
            long aggregatesLoaded,
            long projectionEventsProcessed) {
        public double snapshotHitRate() {
            long total = snapshotsLoaded + aggregatesLoaded;
            if (total == 0)
                return 0;
            return (double) snapshotsLoaded / total * 100;
        }
    }
}
