package com.fast.cqrs.concurrent.resilience;

/**
 * Exception thrown when circuit breaker is open.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
