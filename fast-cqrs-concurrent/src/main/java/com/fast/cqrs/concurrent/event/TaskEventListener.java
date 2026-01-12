package com.fast.cqrs.concurrent.event;

/**
 * Listener for task lifecycle events.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * TaskEventListener listener = event -> {
 *     switch (event) {
 *         case TaskEvent.Started s -> log.info("Task {} started", s.taskName());
 *         case TaskEvent.Completed c -> log.info("Task {} completed in {}ms",
 *                 c.taskName(), c.durationNanos() / 1_000_000);
 *         case TaskEvent.Failed f -> log.error("Task {} failed", f.taskName(), f.error());
 *         case TaskEvent.TimedOut t -> log.warn("Task {} timed out after {}",
 *                 t.taskName(), t.timeout());
 *         case TaskEvent.Retrying r -> log.info("Task {} retrying {}/{}",
 *                 r.taskName(), r.attempt(), r.maxAttempts());
 *         case TaskEvent.Cancelled c -> log.info("Task {} cancelled", c.taskName());
 *     }
 * };
 * 
 * Tasks.supply("my-task", () -> doWork())
 *         .listener(listener)
 *         .execute();
 * }</pre>
 */
@FunctionalInterface
public interface TaskEventListener {

    void onEvent(TaskEvent event);

    /**
     * Creates a listener that logs events.
     */
    static TaskEventListener logging() {
        return event -> {
            switch (event) {
                case TaskEvent.Started s ->
                    System.out.println("[TASK] " + s.taskName() + " started");
                case TaskEvent.Completed c ->
                    System.out.println("[TASK] " + c.taskName() + " completed in " +
                            (c.durationNanos() / 1_000_000) + "ms");
                case TaskEvent.Failed f ->
                    System.err.println("[TASK] " + f.taskName() + " failed: " + f.error().getMessage());
                case TaskEvent.TimedOut t ->
                    System.out.println("[TASK] " + t.taskName() + " timed out after " + t.timeout());
                case TaskEvent.Retrying r ->
                    System.out.println("[TASK] " + r.taskName() + " retrying " +
                            r.attempt() + "/" + r.maxAttempts());
                case TaskEvent.Cancelled c ->
                    System.out.println("[TASK] " + c.taskName() + " cancelled");
            }
        };
    }

    /**
     * Combines multiple listeners.
     */
    static TaskEventListener composite(TaskEventListener... listeners) {
        return event -> {
            for (TaskEventListener listener : listeners) {
                listener.onEvent(event);
            }
        };
    }
}
