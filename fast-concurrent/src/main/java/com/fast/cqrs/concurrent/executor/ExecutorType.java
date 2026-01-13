package com.fast.cqrs.concurrent.executor;

/**
 * Type of executor for workload classification.
 */
public enum ExecutorType {

    /**
     * Virtual threads - best for I/O-bound with high concurrency.
     */
    VIRTUAL,

    /**
     * CPU-bound tasks - fixed pool based on cores.
     */
    CPU,

    /**
     * I/O-bound tasks - larger pool for blocking operations.
     */
    IO,

    /**
     * Long-running blocking tasks.
     */
    BLOCKING,

    /**
     * Custom user-provided executor.
     */
    CUSTOM
}
