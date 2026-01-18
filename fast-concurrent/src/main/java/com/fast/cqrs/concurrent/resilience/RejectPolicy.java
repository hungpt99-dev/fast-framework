package com.fast.cqrs.concurrent.resilience;

/**
 * Defines behavior when a concurrency limit is reached.
 */
public enum RejectPolicy {
    /**
     * Throw {@link ConcurrencyRejectedException} immediately.
     */
    FAIL_FAST,

    /**
     * Wait for a permit up to the configured timeout, then throw exception.
     * This is the default behavior to handle temporary bursts.
     */
    WAIT_TIMEOUT,

    /**
     * Invoke a fallback method.
     * Access to the fallback method is NOT limited by the semaphore.
     */
    FALLBACK,

    /**
     * Wait indefinitely for a permit.
     * WARNING: Can cause thread starvation. Logged as dangerous.
     */
    WAIT // Dangerous
}
