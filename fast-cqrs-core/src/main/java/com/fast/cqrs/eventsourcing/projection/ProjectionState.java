package com.fast.cqrs.eventsourcing.projection;

import java.time.Instant;

/**
 * Tracks the state of a projection.
 * <p>
 * Used to resume projections from the last processed position.
 */
public record ProjectionState(
        String projectionName,
        long lastProcessedPosition,
        Instant lastProcessedAt,
        ProjectionStatus status,
        String errorMessage) {

    public ProjectionState {
        if (projectionName == null)
            throw new IllegalArgumentException("projectionName required");
        if (lastProcessedAt == null)
            lastProcessedAt = Instant.now();
        if (status == null)
            status = ProjectionStatus.RUNNING;
    }

    /**
     * Creates initial state.
     */
    public static ProjectionState initial(String projectionName) {
        return new ProjectionState(projectionName, 0, Instant.now(), ProjectionStatus.RUNNING, null);
    }

    /**
     * Updates position.
     */
    public ProjectionState withPosition(long position) {
        return new ProjectionState(projectionName, position, Instant.now(), status, null);
    }

    /**
     * Marks as error.
     */
    public ProjectionState withError(String message) {
        return new ProjectionState(projectionName, lastProcessedPosition, Instant.now(),
                ProjectionStatus.ERROR, message);
    }
}
