package com.fast.cqrs.eventsourcing.saga;

/**
 * State of a saga instance.
 */
public enum SagaState {

    /**
     * Saga has started.
     */
    STARTED,

    /**
     * Saga is in progress.
     */
    IN_PROGRESS,

    /**
     * Saga completed successfully.
     */
    COMPLETED,

    /**
     * Saga failed and is compensating.
     */
    COMPENSATING,

    /**
     * Saga compensation completed.
     */
    COMPENSATED,

    /**
     * Saga failed permanently.
     */
    FAILED
}
