package com.fast.cqrs.eventsourcing.outbox;

/**
 * Status of an outbox entry.
 */
public enum OutboxStatus {

    /**
     * Entry is waiting to be published.
     */
    PENDING,

    /**
     * Entry has been published successfully.
     */
    SENT,

    /**
     * Entry failed to publish.
     */
    FAILED
}
