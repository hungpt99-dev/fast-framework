package com.fast.cqrs.concurrent.task;

import java.util.function.Supplier;

/**
 * Factory for creating tasks.
 * <p>
 * Entry point for the Task API:
 * 
 * <pre>{@code
 * // Supply with result
 * User user = Tasks.supply("load-user", () -> userService.load(id))
 *         .timeout(2, TimeUnit.SECONDS)
 *         .retry(3)
 *         .execute();
 * 
 * // Run without result
 * Tasks.run("send-email", () -> emailService.send(msg))
 *         .execute();
 * 
 * // Async execution
 * Future<User> future = Tasks.supply("load-user", () -> userService.load(id))
 *         .executeAsync();
 * }</pre>
 */
public final class Tasks {

    private Tasks() {
    }

    /**
     * Creates a task that supplies a value.
     *
     * @param name     task name for logging/tracing
     * @param supplier the supplier function
     * @return task builder
     */
    public static <T> TaskBuilder<T> supply(String name, Supplier<T> supplier) {
        return new TaskBuilder<>(name, supplier);
    }

    /**
     * Creates a task that runs without returning a value.
     *
     * @param name     task name for logging/tracing
     * @param runnable the runnable function
     * @return task builder
     */
    public static TaskBuilder<Void> run(String name, Runnable runnable) {
        return new TaskBuilder<>(name, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Creates a task from a callable.
     *
     * @param name     task name for logging/tracing
     * @param callable the callable function
     * @return task builder
     */
    public static <T> TaskBuilder<T> call(String name, java.util.concurrent.Callable<T> callable) {
        return new TaskBuilder<>(name, () -> {
            try {
                return callable.call();
            } catch (Exception e) {
                throw new TaskExecutionException("Task '" + name + "' failed", e);
            }
        });
    }
}
