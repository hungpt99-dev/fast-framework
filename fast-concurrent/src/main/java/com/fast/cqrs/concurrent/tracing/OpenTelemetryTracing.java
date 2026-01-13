package com.fast.cqrs.concurrent.tracing;

import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration with OpenTelemetry tracing (optional dependency).
 * <p>
 * Creates spans for task executions with proper parent-child relationships.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * // Requires io.opentelemetry:opentelemetry-api dependency
 * OpenTelemetryTracing.configure(GlobalOpenTelemetry.get());
 * 
 * Tasks.supply("my-task", () -> doWork())
 *         .listener(OpenTelemetryTracing.listener())
 *         .execute();
 * }</pre>
 */
public final class OpenTelemetryTracing {

    private static Object openTelemetry;
    private static Object tracer;
    private static boolean available = false;
    private static final Map<String, Object> activeSpans = new ConcurrentHashMap<>();

    static {
        try {
            Class.forName("io.opentelemetry.api.OpenTelemetry");
            available = true;
        } catch (ClassNotFoundException e) {
            available = false;
        }
    }

    private OpenTelemetryTracing() {
    }

    /**
     * Configures with an OpenTelemetry instance.
     */
    public static void configure(Object otel) {
        if (!available) {
            throw new IllegalStateException(
                    "OpenTelemetry not available. Add io.opentelemetry:opentelemetry-api dependency.");
        }
        openTelemetry = otel;
        try {
            Class<?> otelClass = Class.forName("io.opentelemetry.api.OpenTelemetry");
            tracer = otelClass.getMethod("getTracer", String.class)
                    .invoke(otel, "fast-concurrent");
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure OpenTelemetry", e);
        }
    }

    /**
     * Returns true if OpenTelemetry is available and configured.
     */
    public static boolean isAvailable() {
        return available && tracer != null;
    }

    /**
     * Returns a task event listener that creates OpenTelemetry spans.
     */
    public static TaskEventListener listener() {
        if (!isAvailable()) {
            return event -> {
            }; // No-op if not configured
        }

        return event -> {
            try {
                handleEvent(event);
            } catch (Exception ignored) {
                // Don't fail if tracing fails
            }
        };
    }

    private static void handleEvent(TaskEvent event) throws Exception {
        Class<?> tracerClass = Class.forName("io.opentelemetry.api.trace.Tracer");
        Class<?> spanBuilderClass = Class.forName("io.opentelemetry.api.trace.SpanBuilder");
        Class<?> spanClass = Class.forName("io.opentelemetry.api.trace.Span");
        Class<?> statusCodeClass = Class.forName("io.opentelemetry.api.trace.StatusCode");

        String taskName = event.taskName();

        switch (event) {
            case TaskEvent.Started s -> {
                Object spanBuilder = tracerClass.getMethod("spanBuilder", String.class)
                        .invoke(tracer, taskName);
                Object span = spanBuilderClass.getMethod("startSpan").invoke(spanBuilder);
                activeSpans.put(taskName + "-" + Thread.currentThread().threadId(), span);
            }
            case TaskEvent.Completed c -> {
                Object span = activeSpans.remove(taskName + "-" + Thread.currentThread().threadId());
                if (span != null) {
                    spanClass.getMethod("setAttribute", String.class, long.class)
                            .invoke(span, "duration_ms", c.durationNanos() / 1_000_000);
                    spanClass.getMethod("end").invoke(span);
                }
            }
            case TaskEvent.Failed f -> {
                Object span = activeSpans.remove(taskName + "-" + Thread.currentThread().threadId());
                if (span != null) {
                    Object errorStatus = statusCodeClass.getField("ERROR").get(null);
                    spanClass.getMethod("setStatus", statusCodeClass, String.class)
                            .invoke(span, errorStatus, f.error().getMessage());
                    spanClass.getMethod("recordException", Throwable.class)
                            .invoke(span, f.error());
                    spanClass.getMethod("end").invoke(span);
                }
            }
            case TaskEvent.Retrying r -> {
                Object span = activeSpans.get(taskName + "-" + Thread.currentThread().threadId());
                if (span != null) {
                    spanClass.getMethod("addEvent", String.class)
                            .invoke(span, "retry-" + r.attempt());
                }
            }
            default -> {
            }
        }
    }
}
