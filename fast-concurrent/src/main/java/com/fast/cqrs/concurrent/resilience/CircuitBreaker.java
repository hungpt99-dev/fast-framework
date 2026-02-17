package com.fast.cqrs.concurrent.resilience;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lightweight circuit breaker for resilient concurrent execution.
 * <p>
 * States:
 * <ul>
 * <li>CLOSED - Normal operation, requests pass through</li>
 * <li>OPEN - Failing fast, requests rejected</li>
 * <li>HALF_OPEN - Testing recovery</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreaker.of("payment-service")
 *         .failureThreshold(5)
 *         .resetTimeout(Duration.ofSeconds(30))
 *         .build();
 * 
 * Result result = breaker.execute(() -> paymentService.call());
 * }</pre>
 */
public class CircuitBreaker {

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private final String name;
    private final int failureThreshold;
    private final Duration resetTimeout;
    private final int halfOpenSuccessThreshold;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
    private volatile long lastFailureTime = 0;

    private CircuitBreaker(Builder builder) {
        this.name = builder.name;
        this.failureThreshold = builder.failureThreshold;
        this.resetTimeout = builder.resetTimeout;
        this.halfOpenSuccessThreshold = builder.halfOpenSuccessThreshold;
    }

    /**
     * Creates a circuit breaker builder.
     */
    public static Builder of(String name) {
        return new Builder(name);
    }

    /**
     * Executes a supplier with circuit breaker protection.
     */
    public <T> T execute(Supplier<T> supplier) {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException(
                    "Circuit breaker '" + name + "' is OPEN");
        }

        try {
            T result = supplier.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Executes a runnable with circuit breaker protection.
     */
    public void execute(Runnable runnable) {
        execute(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Returns true if request is allowed.
     */
    public boolean allowRequest() {
        State currentState = state.get();

        if (currentState == State.CLOSED) {
            return true;
        }

        if (currentState == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastFailureTime;
            if (elapsed >= resetTimeout.toMillis()) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    halfOpenSuccessCount.set(0);
                }
                return true;
            }
            return false;
        }

        // HALF_OPEN
        return true;
    }

    /**
     * Records a successful execution.
     */
    public void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            int successes = halfOpenSuccessCount.incrementAndGet();
            if (successes >= halfOpenSuccessThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    failureCount.set(0);
                }
            }
        } else if (state.get() == State.CLOSED) {
            failureCount.set(0);
        }
    }

    /**
     * Records a failed execution.
     */
    public void recordFailure() {
        lastFailureTime = System.currentTimeMillis();

        if (state.get() == State.HALF_OPEN) {
            state.compareAndSet(State.HALF_OPEN, State.OPEN);
        } else if (state.get() == State.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                state.compareAndSet(State.CLOSED, State.OPEN);
            }
        }
    }

    /**
     * Gets current state.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Gets circuit breaker name.
     */
    public String getName() {
        return name;
    }

    /**
     * Resets the circuit breaker.
     */
    public void reset() {
        state.set(State.CLOSED);
        failureCount.set(0);
        halfOpenSuccessCount.set(0);
    }

    /**
     * Builder for CircuitBreaker.
     */
    public static class Builder {
        private final String name;
        private int failureThreshold = 5;
        private Duration resetTimeout = Duration.ofSeconds(30);
        private int halfOpenSuccessThreshold = 2;

        Builder(String name) {
            this.name = name;
        }

        public Builder failureThreshold(int threshold) {
            this.failureThreshold = threshold;
            return this;
        }

        public Builder resetTimeout(Duration timeout) {
            this.resetTimeout = timeout;
            return this;
        }

        public Builder resetTimeout(long seconds) {
            this.resetTimeout = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder halfOpenSuccessThreshold(int threshold) {
            this.halfOpenSuccessThreshold = threshold;
            return this;
        }

        public CircuitBreaker build() {
            return new CircuitBreaker(this);
        }
    }
}
