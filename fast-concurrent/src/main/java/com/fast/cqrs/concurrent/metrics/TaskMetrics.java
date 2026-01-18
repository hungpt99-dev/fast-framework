package com.fast.cqrs.concurrent.metrics;

import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Collects metrics from task executions.
 * <p>
 * Metrics collected:
 * <ul>
 * <li>Task count (started, completed, failed)</li>
 * <li>Duration histograms</li>
 * <li>Retry counts</li>
 * <li>Timeout counts</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * Tasks.supply("my-task", () -> doWork())
 *         .listener(TaskMetrics.listener())
 *         .execute();
 * 
 * // Get metrics
 * TaskMetrics.Stats stats = TaskMetrics.stats("my-task");
 * }</pre>
 */
public final class TaskMetrics {

    private static final Map<String, TaskStats> taskStats = new ConcurrentHashMap<>();
    private static final LongAdder totalTasks = new LongAdder();
    private static final LongAdder totalCompleted = new LongAdder();
    private static final LongAdder totalFailed = new LongAdder();
    private static final LongAdder totalTimeouts = new LongAdder();
    private static final LongAdder totalRetries = new LongAdder();

    private TaskMetrics() {
    }

    /**
     * Returns a task event listener that collects metrics.
     */
    public static TaskEventListener listener() {
        return event -> {
            String name = event.taskName();
            TaskStats stats = taskStats.computeIfAbsent(name, TaskStats::new);

            switch (event) {
                case TaskEvent.Started s -> {
                    stats.started.increment();
                    totalTasks.increment();
                }
                case TaskEvent.Completed c -> {
                    stats.completed.increment();
                    stats.recordDuration(c.durationNanos());
                    totalCompleted.increment();
                }
                case TaskEvent.Failed f -> {
                    stats.failed.increment();
                    totalFailed.increment();
                }
                case TaskEvent.TimedOut t -> {
                    stats.timedOut.increment();
                    totalTimeouts.increment();
                }
                case TaskEvent.Retrying r -> {
                    stats.retried.increment();
                    totalRetries.increment();
                }
                case TaskEvent.Cancelled c -> {
                    stats.cancelled.increment();
                }
            }
        };
    }

    /**
     * Gets stats for a specific task.
     */
    public static Stats stats(String taskName) {
        TaskStats stats = taskStats.get(taskName);
        if (stats == null)
            return null;
        return stats.toStats();
    }

    /**
     * Gets global stats.
     */
    public static GlobalStats globalStats() {
        return new GlobalStats(
                totalTasks.sum(),
                totalCompleted.sum(),
                totalFailed.sum(),
                totalTimeouts.sum(),
                totalRetries.sum());
    }

    /**
     * Resets all metrics.
     */
    public static void reset() {
        taskStats.clear();
        totalTasks.reset();
        totalCompleted.reset();
        totalFailed.reset();
        totalTimeouts.reset();
        totalRetries.reset();
    }

    /**
     * Gets all task names with metrics.
     */
    public static Set<String> taskNames() {
        return taskStats.keySet();
    }

    /**
     * Per-task statistics.
     */
    public record Stats(
            String taskName,
            long started,
            long completed,
            long failed,
            long timedOut,
            long retried,
            long cancelled,
            double avgDurationMs,
            long minDurationMs,
            long maxDurationMs) {
        public double successRate() {
            long total = completed + failed;
            return total == 0 ? 0 : (double) completed / total * 100;
        }
    }

    /**
     * Global statistics.
     */
    public record GlobalStats(
            long totalTasks,
            long totalCompleted,
            long totalFailed,
            long totalTimeouts,
            long totalRetries) {
        public double successRate() {
            long total = totalCompleted + totalFailed;
            return total == 0 ? 0 : (double) totalCompleted / total * 100;
        }
    }

    private static class TaskStats {
        final String name;
        final LongAdder started = new LongAdder();
        final LongAdder completed = new LongAdder();
        final LongAdder failed = new LongAdder();
        final LongAdder timedOut = new LongAdder();
        final LongAdder retried = new LongAdder();
        final LongAdder cancelled = new LongAdder();
        final LongAdder totalDurationNanos = new LongAdder();
        final AtomicLong minDurationNanos = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong maxDurationNanos = new AtomicLong(0);

        TaskStats(String name) {
            this.name = name;
        }

        void recordDuration(long nanos) {
            totalDurationNanos.add(nanos);
            minDurationNanos.updateAndGet(current -> Math.min(current, nanos));
            maxDurationNanos.updateAndGet(current -> Math.max(current, nanos));
        }

        Stats toStats() {
            long completedCount = completed.sum();
            double avgMs = completedCount == 0 ? 0 : (double) totalDurationNanos.sum() / completedCount / 1_000_000;
            long minMs = minDurationNanos.get() == Long.MAX_VALUE ? 0 : minDurationNanos.get() / 1_000_000;
            long maxMs = maxDurationNanos.get() / 1_000_000;

            return new Stats(
                    name,
                    started.sum(),
                    completedCount,
                    failed.sum(),
                    timedOut.sum(),
                    retried.sum(),
                    cancelled.sum(),
                    avgMs,
                    minMs,
                    maxMs);
        }
    }
}
