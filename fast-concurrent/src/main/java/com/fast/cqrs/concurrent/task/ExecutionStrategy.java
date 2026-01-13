package com.fast.cqrs.concurrent.task;

/**
 * Execution strategy for tasks.
 */
public enum ExecutionStrategy {

    /**
     * Virtual threads (Java 21+) - best for I/O-bound tasks.
     */
    VIRTUAL_THREAD,

    /**
     * ForkJoinPool - best for CPU-bound parallel tasks.
     */
    FORK_JOIN,

    /**
     * Platform thread pool - traditional executor.
     */
    PLATFORM_THREAD,

    /**
     * Execute on caller thread (synchronous).
     */
    CALLER,

    /**
     * Auto-select based on task characteristics.
     */
    AUTO
}
