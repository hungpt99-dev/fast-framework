package com.fast.cqrs.concurrent.flow;

import java.util.Map;
import java.util.Optional;

/**
 * Result of a parallel flow execution.
 * <p>
 * Contains results and errors for each named task.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * FlowResult result = ParallelFlow.of()
 *         .task("user", () -> loadUser())
 *         .task("orders", () -> loadOrders())
 *         .execute();
 * 
 * User user = result.get("user", User.class);
 * List<Order> orders = result.get("orders");
 * 
 * if (result.hasErrors()) {
 *     result.errors().forEach((name, error) -> log.error("Task {} failed: {}", name, error.getMessage()));
 * }
 * }</pre>
 */
public class FlowResult {

    private final Map<String, Object> results;
    private final Map<String, Throwable> errors;
    private final long durationNanos;
    private final boolean allSuccessful;

    public FlowResult(Map<String, Object> results, Map<String, Throwable> errors, long durationNanos) {
        this.results = Map.copyOf(results);
        this.errors = Map.copyOf(errors);
        this.durationNanos = durationNanos;
        this.allSuccessful = errors.isEmpty();
    }

    /**
     * Gets a result by task name.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String taskName) {
        return (T) results.get(taskName);
    }

    /**
     * Gets a result by task name with type check.
     */
    public <T> T get(String taskName, Class<T> type) {
        Object result = results.get(taskName);
        if (result == null)
            return null;
        return type.cast(result);
    }

    /**
     * Gets a result as Optional.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOptional(String taskName) {
        return Optional.ofNullable((T) results.get(taskName));
    }

    /**
     * Returns true if all tasks succeeded.
     */
    public boolean isAllSuccessful() {
        return allSuccessful;
    }

    /**
     * Returns true if the flow has any errors.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Gets error for a specific task.
     */
    public Optional<Throwable> getError(String taskName) {
        return Optional.ofNullable(errors.get(taskName));
    }

    /**
     * Gets all errors.
     */
    public Map<String, Throwable> errors() {
        return errors;
    }

    /**
     * Gets all successful results.
     */
    public Map<String, Object> results() {
        return results;
    }

    /**
     * Gets execution duration in milliseconds.
     */
    public long durationMillis() {
        return durationNanos / 1_000_000;
    }

    /**
     * Gets execution duration in nanoseconds.
     */
    public long durationNanos() {
        return durationNanos;
    }

    /**
     * Gets the number of successful tasks.
     */
    public int successCount() {
        return results.size();
    }

    /**
     * Gets the number of failed tasks.
     */
    public int errorCount() {
        return errors.size();
    }

    @Override
    public String toString() {
        return "FlowResult{success=" + results.size() +
                ", errors=" + errors.size() +
                ", duration=" + durationMillis() + "ms}";
    }
}
