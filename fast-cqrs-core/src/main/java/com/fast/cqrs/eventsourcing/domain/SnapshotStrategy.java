package com.fast.cqrs.eventsourcing.domain;

/**
 * Strategy for when to create snapshots.
 */
public enum SnapshotStrategy {

    /**
     * No automatic snapshotting.
     */
    NONE,

    /**
     * Snapshot every N events.
     */
    EVENT_COUNT,

    /**
     * Snapshot based on time intervals.
     */
    TIME_BASED,

    /**
     * Adaptive snapshotting based on aggregate size and load time.
     */
    ADAPTIVE
}
