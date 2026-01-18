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

    private static final TaskEventListener NO_OP = event -> {};
    private static volatile TaskEventListener listener;
    
    private OpenTelemetryTracing() {
    }

    /**
     * Configures with an OpenTelemetry instance.
     */
    public static void configure(Object otel) {
        try {
            // Check availability through linkage
            Class.forName("io.opentelemetry.api.OpenTelemetry");
            if (otel instanceof io.opentelemetry.api.OpenTelemetry) {
                 listener = new RealOtelListener((io.opentelemetry.api.OpenTelemetry) otel);
            } else {
                 throw new IllegalArgumentException("Provided object is not an OpenTelemetry instance");
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            throw new IllegalStateException("OpenTelemetry API not found on classpath", e);
        }
    }

    /**
     * Returns a task event listener that creates OpenTelemetry spans.
     */
    public static TaskEventListener listener() {
        return listener != null ? listener : NO_OP;
    }

    // Inner class loaded only when OpenTelemetry is present and used
    private static class RealOtelListener implements TaskEventListener {
        private final io.opentelemetry.api.trace.Tracer tracer;
        private final java.util.Map<String, io.opentelemetry.api.trace.Span> activeSpans = new java.util.concurrent.ConcurrentHashMap<>();

        RealOtelListener(io.opentelemetry.api.OpenTelemetry otel) {
            this.tracer = otel.getTracer("fast-concurrent");
        }

        @Override
        public void onEvent(TaskEvent event) {
            try {
                handleEvent(event);
            } catch (Exception ignored) {
                // Don't fail if tracing fails
            }
        }

        private void handleEvent(TaskEvent event) {
            String taskName = event.taskName();
            String spanKey = taskName + "-" + Thread.currentThread().threadId();

            switch (event) {
                case TaskEvent.Started s -> {
                    io.opentelemetry.api.trace.Span span = tracer.spanBuilder(taskName).startSpan();
                    activeSpans.put(spanKey, span);
                }
                case TaskEvent.Completed c -> {
                    io.opentelemetry.api.trace.Span span = activeSpans.remove(spanKey);
                    if (span != null) {
                        span.setAttribute("duration_ms", c.durationNanos() / 1_000_000);
                        span.end();
                    }
                }
                case TaskEvent.Failed f -> {
                    io.opentelemetry.api.trace.Span span = activeSpans.remove(spanKey);
                    if (span != null) {
                        span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, f.error().getMessage());
                        span.recordException(f.error());
                        span.end();
                    }
                }
                case TaskEvent.Retrying r -> {
                    io.opentelemetry.api.trace.Span span = activeSpans.get(spanKey);
                    if (span != null) {
                        span.addEvent("retry-" + r.attempt());
                    }
                }
                default -> {
                }
            }
        }
    }
}
