package com.fast.cqrs.concurrent.event;

import java.time.Duration;

/**
 * Event representing a task lifecycle event.
 */
public sealed interface TaskEvent {

    String taskName();

    // Event types
    record Started(String taskName, long timestamp) implements TaskEvent {
    }

    record Completed(String taskName, long durationNanos, long timestamp) implements TaskEvent {
    }

    record Failed(String taskName, Throwable error, long durationNanos, long timestamp) implements TaskEvent {
    }

    record TimedOut(String taskName, Duration timeout, long timestamp) implements TaskEvent {
    }

    record Retrying(String taskName, int attempt, int maxAttempts, long timestamp) implements TaskEvent {
    }

    record Cancelled(String taskName, long timestamp) implements TaskEvent {
    }

    // Factory methods
    static TaskEvent started(String name) {
        return new Started(name, System.currentTimeMillis());
    }

    static TaskEvent completed(String name, long durationNanos) {
        return new Completed(name, durationNanos, System.currentTimeMillis());
    }

    static TaskEvent failed(String name, Throwable error, long durationNanos) {
        return new Failed(name, error, durationNanos, System.currentTimeMillis());
    }

    static TaskEvent timedOut(String name, Duration timeout) {
        return new TimedOut(name, timeout, System.currentTimeMillis());
    }

    static TaskEvent retrying(String name, int attempt, int maxAttempts) {
        return new Retrying(name, attempt, maxAttempts, System.currentTimeMillis());
    }

    static TaskEvent cancelled(String name) {
        return new Cancelled(name, System.currentTimeMillis());
    }
}
