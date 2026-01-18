package com.fast.cqrs.concurrent.metrics;

import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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

    private static MeterRegistry meterRegistry;
    private static boolean available = false;

    static {
        // Since micrometer-core is implementation, we can just check availability or assume true if we want
        // But to be safe against classpath exclusions, we check:
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
    public static void configure(MeterRegistry registry) {
        meterRegistry = registry;
        available = true;
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
            return event -> {};
        }

        return event -> {
            try {
                recordMetric(event);
            } catch (Exception ignored) {
                // Don't fail if metrics recording fails
            }
        };
    }

    private static void recordMetric(TaskEvent event) {
        String taskName = event.taskName();

        switch (event) {
            case TaskEvent.Completed c -> {
                Timer.builder("task.duration")
                        .tag("task", taskName)
                        .register(meterRegistry)
                        .record(c.duration());

                Counter.builder("task.completed")
                        .tag("task", taskName)
                        .register(meterRegistry)
                        .increment();
            }
            case TaskEvent.Failed f -> {
                Counter.builder("task.failed")
                        .tag("task", taskName)
                        .tag("error", f.error().getClass().getSimpleName())
                        .register(meterRegistry)
                        .increment();
            }
            case TaskEvent.TimedOut t -> {
                Counter.builder("task.timeout")
                        .tag("task", taskName)
                        .register(meterRegistry)
                        .increment();
            }
            case TaskEvent.Retrying r -> {
                Counter.builder("task.retry")
                        .tag("task", taskName)
                        .tag("attempt", String.valueOf(r.attempt()))
                        .register(meterRegistry)
                        .increment();
            }
            default -> {
            }
        }
    }
}
