package com.fast.cqrs.concurrent.task;

import java.util.function.Supplier;

/**
 * Unified task abstraction for async/parallel execution.
 * <p>
 * Provides fluent API with timeout, retry, fallback, and execution strategy.
 * <p>
 * Example:
 * 
 * <pre>{@code
 * Task<User> task = Tasks.supply("load-user", () -> userService.load(id))
 *         .timeout(2, TimeUnit.SECONDS)
 *         .retry(3)
 *         .fallback(() -> User.EMPTY)
 *         .trace()
 *         .build();
 * 
 * User user = task.execute();
 * }</pre>
 *
 * @param <T> the result type
 */
public interface Task<T> {

    /**
     * Executes the task and returns the result.
     */
    T execute();

    /**
     * Executes the task asynchronously.
     */
    java.util.concurrent.Future<T> executeAsync();

    /**
     * Gets the task name.
     */
    String name();

    /**
     * Creates a new builder for this task.
     */
    static <T> TaskBuilder<T> builder(String name, Supplier<T> supplier) {
        return new TaskBuilder<>(name, supplier);
    }

    /**
     * Creates a builder for a runnable task (no return value).
     */
    static TaskBuilder<Void> builder(String name, Runnable runnable) {
        return new TaskBuilder<>(name, () -> {
            runnable.run();
            return null;
        });
    }
}
