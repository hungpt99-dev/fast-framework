package com.fast.cqrs.eventsourcing.replay;

/**
 * Mode for event replay.
 */
public enum ReplayMode {

    /**
     * Replay all events from the beginning.
     */
    FULL,

    /**
     * Replay events from a specific position.
     */
    PARTIAL,

    /**
     * Replay events within a time window.
     */
    TIME_WINDOW,

    /**
     * Replay only specific event types.
     */
    EVENT_TYPE,

    /**
     * Replay events for a specific aggregate.
     */
    AGGREGATE
}
