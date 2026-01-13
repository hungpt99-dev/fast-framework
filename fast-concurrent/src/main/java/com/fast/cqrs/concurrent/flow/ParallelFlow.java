package com.fast.cqrs.concurrent.flow;

import com.fast.cqrs.concurrent.context.ContextSnapshot;
import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Declarative parallel flow execution engine.
 * <p>
 * Features:
 * <ul>
 * <li>Fan-out / Fan-in pattern</li>
 * <li>Fail-fast vs wait-all modes</li>
 * <li>Partial success handling</li>
 * <li>Timeout at flow level</li>
 * <li>Context propagation</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * FlowResult result = ParallelFlow.of()
 *         .task("user", () -> loadUser())
 *         .task("orders", () -> loadOrders())
 *         .task("balance", () -> loadBalance())
 *         .timeout(3, TimeUnit.SECONDS)
 *         .failFast()
 *         .execute();
 * 
 * User user = result.get("user");
 * List<Order> orders = result.get("orders");
 * }</pre>
 */
public class ParallelFlow {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, Supplier<?>> tasks = new LinkedHashMap<>();
    private Duration timeout;
    private boolean failFast = false;
    private boolean propagateContext = true;
    private int maxConcurrency = Integer.MAX_VALUE;
    private final List<TaskEventListener> listeners = new ArrayList<>();

    private ParallelFlow() {
    }

    /**
     * Creates a new parallel flow.
     */
    public static ParallelFlow of() {
        return new ParallelFlow();
    }

    /**
     * Adds a named task to the flow.
     */
    public <T> ParallelFlow task(String name, Supplier<T> supplier) {
        tasks.put(name, supplier);
        return this;
    }

    /**
     * Adds a named runnable task to the flow.
     */
    public ParallelFlow task(String name, Runnable runnable) {
        tasks.put(name, () -> {
            runnable.run();
            return null;
        });
        return this;
    }

    /**
     * Sets flow-level timeout.
     */
    public ParallelFlow timeout(long amount, TimeUnit unit) {
        this.timeout = Duration.of(amount, unit.toChronoUnit());
        return this;
    }

    /**
     * Sets flow-level timeout.
     */
    public ParallelFlow timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    /**
     * Enables fail-fast mode - cancel remaining tasks on first failure.
     */
    public ParallelFlow failFast() {
        this.failFast = true;
        return this;
    }

    /**
     * Wait for all tasks regardless of failures.
     */
    public ParallelFlow waitAll() {
        this.failFast = false;
        return this;
    }

    /**
     * Sets maximum concurrent tasks.
     */
    public ParallelFlow maxConcurrency(int max) {
        this.maxConcurrency = max;
        return this;
    }

    /**
     * Enables/disables context propagation.
     */
    public ParallelFlow propagateContext(boolean propagate) {
        this.propagateContext = propagate;
        return this;
    }

    /**
     * Adds event listener.
     */
    public ParallelFlow listener(TaskEventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    /**
     * Executes all tasks in parallel.
     */
    public FlowResult execute() {
        long startTime = System.nanoTime();
        ContextSnapshot snapshot = propagateContext ? ContextSnapshot.capture() : null;

        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, Throwable> errors = new ConcurrentHashMap<>();
        Map<String, Future<?>> futures = new LinkedHashMap<>();

        // Use semaphore for concurrency limiting
        Semaphore semaphore = maxConcurrency < Integer.MAX_VALUE
                ? new Semaphore(maxConcurrency)
                : null;

        // Submit all tasks
        for (Map.Entry<String, Supplier<?>> entry : tasks.entrySet()) {
            String name = entry.getKey();
            Supplier<?> supplier = entry.getValue();

            Future<?> future = EXECUTOR.submit(() -> {
                try {
                    if (semaphore != null)
                        semaphore.acquire();

                    if (snapshot != null)
                        snapshot.restore();
                    fireEvent(TaskEvent.started(name));

                    long taskStart = System.nanoTime();
                    Object result = supplier.get();
                    results.put(name, result);

                    fireEvent(TaskEvent.completed(name, System.nanoTime() - taskStart));
                } catch (Exception e) {
                    errors.put(name, e);
                    fireEvent(TaskEvent.failed(name, e, 0));

                    if (failFast) {
                        // Cancel other tasks
                        futures.values().forEach(f -> f.cancel(true));
                    }
                } finally {
                    if (semaphore != null)
                        semaphore.release();
                    if (snapshot != null)
                        snapshot.clear();
                }
            });

            futures.put(name, future);
        }

        // Wait for completion
        try {
            if (timeout != null) {
                waitWithTimeout(futures, timeout);
            } else {
                for (Future<?> future : futures.values()) {
                    try {
                        future.get();
                    } catch (CancellationException | ExecutionException ignored) {
                        // Errors already captured
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futures.values().forEach(f -> f.cancel(true));
        }

        long duration = System.nanoTime() - startTime;
        return new FlowResult(results, errors, duration);
    }

    private void waitWithTimeout(Map<String, Future<?>> futures, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();

        for (Map.Entry<String, Future<?>> entry : futures.entrySet()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                futures.values().forEach(f -> f.cancel(true));
                fireEvent(TaskEvent.timedOut("flow", timeout));
                break;
            }

            try {
                entry.getValue().get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                futures.values().forEach(f -> f.cancel(true));
                fireEvent(TaskEvent.timedOut(entry.getKey(), timeout));
            } catch (CancellationException | ExecutionException ignored) {
                // Errors already captured
            }
        }
    }

    private void fireEvent(TaskEvent event) {
        for (TaskEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
            }
        }
    }
}
