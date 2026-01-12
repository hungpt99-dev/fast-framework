package com.fast.cqrs.eventsourcing.projection;

/**
 * Status of a projection.
 */
public enum ProjectionStatus {

    /**
     * Projection is running normally.
     */
    RUNNING,

    /**
     * Projection is paused.
     */
    PAUSED,

    /**
     * Projection is replaying events.
     */
    REPLAYING,

    /**
     * Projection encountered an error.
     */
    ERROR,

    /**
     * Projection is rebuilding from scratch.
     */
    REBUILDING
}
