package com.fast.cqrs.concurrent.resilience;

/**
 * Thrown when a task is rejected due to concurrency limits.
 */
public class ConcurrencyRejectedException extends RuntimeException {
    
    public ConcurrencyRejectedException(String message) {
        super(message);
    }

    public ConcurrencyRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
