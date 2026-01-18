package com.fast.cqrs.concurrent.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Logger log = LoggerFactory.getLogger(TaskEventListener.class);
        return event -> {
            switch (event) {
                case TaskEvent.Started s ->
                    log.info("[TASK] {} started", s.taskName());
                case TaskEvent.Completed c ->
                    log.info("[TASK] {} completed in {}ms", c.taskName(), c.durationNanos() / 1_000_000);
                case TaskEvent.Failed f ->
                    log.error("[TASK] {} failed: {}", f.taskName(), f.error().getMessage());
                case TaskEvent.TimedOut t ->
                    log.warn("[TASK] {} timed out after {}", t.taskName(), t.timeout());
                case TaskEvent.Retrying r ->
                    log.info("[TASK] {} retrying {}/{}", r.taskName(), r.attempt(), r.maxAttempts());
                case TaskEvent.Cancelled c ->
                    log.info("[TASK] {} cancelled", c.taskName());
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
