package com.fast.cqrs.eventsourcing.replay;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks replay progress.
 */
public class ReplayProgress {

    private final String projectionName;
    private final long totalEvents;
    private final AtomicLong processedEvents = new AtomicLong(0);
    private final Instant startedAt;
    private volatile Instant completedAt;
    private volatile boolean cancelled;
    private volatile boolean paused;

    public ReplayProgress(String projectionName, long totalEvents) {
        this.projectionName = projectionName;
        this.totalEvents = totalEvents;
        this.startedAt = Instant.now();
    }

    public void increment() {
        processedEvents.incrementAndGet();
    }

    public void complete() {
        completedAt = Instant.now();
    }

    public void cancel() {
        cancelled = true;
        completedAt = Instant.now();
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public String getProjectionName() {
        return projectionName;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public long getProcessedEvents() {
        return processedEvents.get();
    }

    public double getPercentComplete() {
        if (totalEvents == 0)
            return 100.0;
        return (double) processedEvents.get() / totalEvents * 100;
    }

    public boolean isComplete() {
        return completedAt != null;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isPaused() {
        return paused;
    }

    public Duration getElapsed() {
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(startedAt, end);
    }

    public long getEventsPerSecond() {
        long elapsed = getElapsed().toSeconds();
        if (elapsed == 0)
            return 0;
        return processedEvents.get() / elapsed;
    }
}
