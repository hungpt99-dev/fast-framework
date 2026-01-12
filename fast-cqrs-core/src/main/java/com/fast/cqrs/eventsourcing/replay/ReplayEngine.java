package com.fast.cqrs.eventsourcing.replay;

import com.fast.cqrs.event.DomainEvent;
import com.fast.cqrs.eventsourcing.EventStore;
import com.fast.cqrs.eventsourcing.projection.Projection;
import com.fast.cqrs.eventsourcing.projection.ProjectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Engine for replaying events to projections.
 * <p>
 * Supports multiple replay modes with progress tracking and throttling.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * ReplayEngine engine = new ReplayEngine(eventStore, projectionManager);
 * 
 * // Full replay
 * ReplayProgress progress = engine.replay("order-summary");
 * 
 * // Monitor progress
 * while (!progress.isComplete()) {
 *     System.out.println(progress.getPercentComplete() + "% complete");
 *     Thread.sleep(1000);
 * }
 * }</pre>
 */
public class ReplayEngine {

    private static final Logger log = LoggerFactory.getLogger(ReplayEngine.class);

    private final EventStore eventStore;
    private final ProjectionManager projectionManager;
    private final Map<String, ReplayProgress> activeReplays = new ConcurrentHashMap<>();
    private int batchSize = 1000;
    private long throttleDelayMs = 0;

    public ReplayEngine(EventStore eventStore, ProjectionManager projectionManager) {
        this.eventStore = eventStore;
        this.projectionManager = projectionManager;
    }

    /**
     * Sets the batch size for replay.
     */
    public ReplayEngine batchSize(int size) {
        this.batchSize = size;
        return this;
    }

    /**
     * Sets the throttle delay between batches.
     */
    public ReplayEngine throttle(long delayMs) {
        this.throttleDelayMs = delayMs;
        return this;
    }

    /**
     * Replays all events to a projection.
     */
    public ReplayProgress replay(String projectionName) {
        return replay(projectionName, ReplayMode.FULL, null);
    }

    /**
     * Replays events to a projection for a specific aggregate.
     */
    public ReplayProgress replayAggregate(String projectionName, String aggregateId) {
        return replay(projectionName, ReplayMode.AGGREGATE, aggregateId);
    }

    /**
     * Replays events with specified mode.
     */
    public ReplayProgress replay(String projectionName, ReplayMode mode, String aggregateId) {
        log.info("Starting {} replay for projection: {}", mode, projectionName);

        projectionManager.reset(projectionName);

        List<DomainEvent> events = aggregateId != null
                ? eventStore.load(aggregateId)
                : List.of(); // Would need stream API for full replay

        ReplayProgress progress = new ReplayProgress(projectionName, events.size());
        activeReplays.put(projectionName, progress);

        // Get projection and notify replay start
        for (Projection p : projectionManager.getProjections()) {
            if (p.getName().equals(projectionName)) {
                p.onReplayStart();
                break;
            }
        }

        // Process events
        for (DomainEvent event : events) {
            if (progress.isCancelled()) {
                log.info("Replay cancelled for {}", projectionName);
                break;
            }

            while (progress.isPaused()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            projectionManager.process(event, true);
            progress.increment();

            if (throttleDelayMs > 0 && progress.getProcessedEvents() % batchSize == 0) {
                try {
                    Thread.sleep(throttleDelayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Notify replay complete
        for (Projection p : projectionManager.getProjections()) {
            if (p.getName().equals(projectionName)) {
                p.onReplayComplete();
                break;
            }
        }

        progress.complete();
        activeReplays.remove(projectionName);

        log.info("Replay complete for {}: {} events in {}",
                projectionName, progress.getProcessedEvents(), progress.getElapsed());

        return progress;
    }

    /**
     * Cancels an active replay.
     */
    public void cancel(String projectionName) {
        ReplayProgress progress = activeReplays.get(projectionName);
        if (progress != null) {
            progress.cancel();
        }
    }

    /**
     * Pauses an active replay.
     */
    public void pause(String projectionName) {
        ReplayProgress progress = activeReplays.get(projectionName);
        if (progress != null) {
            progress.pause();
        }
    }

    /**
     * Resumes a paused replay.
     */
    public void resume(String projectionName) {
        ReplayProgress progress = activeReplays.get(projectionName);
        if (progress != null) {
            progress.resume();
        }
    }

    /**
     * Gets the progress of an active replay.
     */
    public ReplayProgress getProgress(String projectionName) {
        return activeReplays.get(projectionName);
    }
}
