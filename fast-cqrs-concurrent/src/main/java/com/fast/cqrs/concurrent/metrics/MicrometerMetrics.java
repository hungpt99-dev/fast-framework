package com.fast.cqrs.concurrent.metrics;

import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

/**
 * Integration with Micrometer metrics (optional dependency).
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * // Requires io.micrometer:micrometer-core dependency
 * MeterRegistry registry = new SimpleMeterRegistry();
 * MicrometerMetrics.configure(registry);
 * 
 * Tasks.supply("my-task", () -> doWork())
 *         .listener(MicrometerMetrics.listener())
 *         .execute();
 * }</pre>
 */
public final class MicrometerMetrics {

    private static Object meterRegistry;
    private static boolean available = false;

    static {
        try {
            Class.forName("io.micrometer.core.instrument.MeterRegistry");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
    }

    private MicrometerMetrics() {
    }

    /**
     * Configures with a MeterRegistry.
     */
    public static void configure(Object registry) {
        if (!available) {
            throw new IllegalStateException(
                    "Micrometer not available. Add io.micrometer:micrometer-core dependency.");
        }
        meterRegistry = registry;
    }

    /**
     * Returns true if Micrometer is available.
     */
    public static boolean isAvailable() {
        return available && meterRegistry != null;
    }

    /**
     * Returns a task event listener that publishes Micrometer metrics.
     */
    public static TaskEventListener listener() {
        if (!isAvailable()) {
            return event -> {
            }; // No-op if not configured
        }

        return event -> {
            try {
                recordMetric(event);
            } catch (Exception ignored) {
                // Don't fail if metrics recording fails
            }
        };
    }

    private static void recordMetric(TaskEvent event) throws Exception {
        Class<?> meterClass = Class.forName("io.micrometer.core.instrument.MeterRegistry");
        Class<?> timerClass = Class.forName("io.micrometer.core.instrument.Timer");
        Class<?> counterClass = Class.forName("io.micrometer.core.instrument.Counter");

        String taskName = event.taskName();

        switch (event) {
            case TaskEvent.Completed c -> {
                // Record duration
                Object timer = meterClass.getMethod("timer", String.class, String[].class)
                        .invoke(meterRegistry, "task.duration", new String[] { "task", taskName });
                timerClass.getMethod("record", java.time.Duration.class)
                        .invoke(timer, java.time.Duration.ofNanos(c.durationNanos()));

                // Increment counter
                Object counter = meterClass.getMethod("counter", String.class, String[].class)
                        .invoke(meterRegistry, "task.completed", new String[] { "task", taskName });
                counterClass.getMethod("increment").invoke(counter);
            }
            case TaskEvent.Failed f -> {
                Object counter = meterClass.getMethod("counter", String.class, String[].class)
                        .invoke(meterRegistry, "task.failed", new String[] { "task", taskName });
                counterClass.getMethod("increment").invoke(counter);
            }
            case TaskEvent.TimedOut t -> {
                Object counter = meterClass.getMethod("counter", String.class, String[].class)
                        .invoke(meterRegistry, "task.timeout", new String[] { "task", taskName });
                counterClass.getMethod("increment").invoke(counter);
            }
            case TaskEvent.Retrying r -> {
                Object counter = meterClass.getMethod("counter", String.class, String[].class)
                        .invoke(meterRegistry, "task.retry",
                                new String[] { "task", taskName, "attempt", String.valueOf(r.attempt()) });
                counterClass.getMethod("increment").invoke(counter);
            }
            default -> {
            }
        }
    }
}
