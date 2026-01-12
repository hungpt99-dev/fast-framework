package com.fast.cqrs.concurrent.task;

import com.fast.cqrs.concurrent.context.ContextSnapshot;
import com.fast.cqrs.concurrent.event.TaskEvent;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Fluent builder for Task configuration.
 * <p>
 * Supports:
 * <ul>
 * <li>Timeout with configurable duration</li>
 * <li>Retry with exponential backoff</li>
 * <li>Fallback supplier on failure</li>
 * <li>Execution strategy selection</li>
 * <li>Context propagation (MDC, SecurityContext)</li>
 * <li>Lifecycle event listeners</li>
 * </ul>
 *
 * @param <T> the result type
 */
public class TaskBuilder<T> {

    private final String name;
    private final Supplier<T> supplier;

    private Duration timeout;
    private int maxRetries = 0;
    private Duration retryDelay = Duration.ofMillis(100);
    private boolean exponentialBackoff = false;
    private Supplier<T> fallback;
    private ExecutionStrategy strategy = ExecutionStrategy.AUTO;
    private boolean propagateContext = true;
    private boolean trace = false;
    private final List<TaskEventListener> listeners = new ArrayList<>();

    public TaskBuilder(String name, Supplier<T> supplier) {
        this.name = name;
        this.supplier = supplier;
    }

    /**
     * Sets execution timeout.
     */
    public TaskBuilder<T> timeout(long amount, TimeUnit unit) {
        this.timeout = Duration.of(amount, unit.toChronoUnit());
        return this;
    }

    /**
     * Sets execution timeout.
     */
    public TaskBuilder<T> timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    /**
     * Sets retry count with default delay.
     */
    public TaskBuilder<T> retry(int maxAttempts) {
        this.maxRetries = maxAttempts;
        return this;
    }

    /**
     * Sets retry with custom delay.
     */
    public TaskBuilder<T> retry(int maxAttempts, long delayMs) {
        this.maxRetries = maxAttempts;
        this.retryDelay = Duration.ofMillis(delayMs);
        return this;
    }

    /**
     * Enables exponential backoff for retries.
     */
    public TaskBuilder<T> exponentialBackoff() {
        this.exponentialBackoff = true;
        return this;
    }

    /**
     * Sets fallback supplier on failure.
     */
    public TaskBuilder<T> fallback(Supplier<T> fallback) {
        this.fallback = fallback;
        return this;
    }

    /**
     * Sets fallback value on failure.
     */
    public TaskBuilder<T> fallback(T value) {
        this.fallback = () -> value;
        return this;
    }

    /**
     * Sets execution strategy.
     */
    public TaskBuilder<T> strategy(ExecutionStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    /**
     * Enables context propagation (MDC, SecurityContext).
     */
    public TaskBuilder<T> propagateContext(boolean propagate) {
        this.propagateContext = propagate;
        return this;
    }

    /**
     * Enables tracing/logging for this task.
     */
    public TaskBuilder<T> trace() {
        this.trace = true;
        return this;
    }

    /**
     * Adds event listener.
     */
    public TaskBuilder<T> listener(TaskEventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    /**
     * Builds the task.
     */
    public Task<T> build() {
        return new DefaultTask<>(this);
    }

    /**
     * Builds and executes the task.
     */
    public T execute() {
        return build().execute();
    }

    /**
     * Builds and executes the task asynchronously.
     */
    public Future<T> executeAsync() {
        return build().executeAsync();
    }

    // Getters for DefaultTask
    String getName() {
        return name;
    }

    Supplier<T> getSupplier() {
        return supplier;
    }

    Duration getTimeout() {
        return timeout;
    }

    int getMaxRetries() {
        return maxRetries;
    }

    Duration getRetryDelay() {
        return retryDelay;
    }

    boolean isExponentialBackoff() {
        return exponentialBackoff;
    }

    Supplier<T> getFallback() {
        return fallback;
    }

    ExecutionStrategy getStrategy() {
        return strategy;
    }

    boolean isPropagateContext() {
        return propagateContext;
    }

    boolean isTrace() {
        return trace;
    }

    List<TaskEventListener> getListeners() {
        return listeners;
    }
}

/**
 * Default task implementation.
 */
class DefaultTask<T> implements Task<T> {

    private static final ForkJoinPool FORK_JOIN = ForkJoinPool.commonPool();
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final String name;
    private final Supplier<T> supplier;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryDelay;
    private final boolean exponentialBackoff;
    private final Supplier<T> fallback;
    private final ExecutionStrategy strategy;
    private final boolean propagateContext;
    private final boolean trace;
    private final List<TaskEventListener> listeners;

    DefaultTask(TaskBuilder<T> builder) {
        this.name = builder.getName();
        this.supplier = builder.getSupplier();
        this.timeout = builder.getTimeout();
        this.maxRetries = builder.getMaxRetries();
        this.retryDelay = builder.getRetryDelay();
        this.exponentialBackoff = builder.isExponentialBackoff();
        this.fallback = builder.getFallback();
        this.strategy = builder.getStrategy();
        this.propagateContext = builder.isPropagateContext();
        this.trace = builder.isTrace();
        this.listeners = new ArrayList<>(builder.getListeners());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public T execute() {
        long startTime = System.nanoTime();
        ContextSnapshot snapshot = propagateContext ? ContextSnapshot.capture() : null;

        fireEvent(TaskEvent.started(name));

        try {
            T result = executeWithRetry(snapshot);
            long duration = System.nanoTime() - startTime;
            fireEvent(TaskEvent.completed(name, duration));
            return result;
        } catch (Exception e) {
            long duration = System.nanoTime() - startTime;
            fireEvent(TaskEvent.failed(name, e, duration));

            if (fallback != null) {
                if (trace) {
                    System.out.println("[TASK] " + name + " using fallback due to: " + e.getMessage());
                }
                return fallback.get();
            }
            throw wrapException(e);
        }
    }

    @Override
    public Future<T> executeAsync() {
        ContextSnapshot snapshot = propagateContext ? ContextSnapshot.capture() : null;

        return getExecutor().submit(() -> {
            if (snapshot != null)
                snapshot.restore();
            try {
                return execute();
            } finally {
                if (snapshot != null)
                    snapshot.clear();
            }
        });
    }

    private T executeWithRetry(ContextSnapshot snapshot) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (snapshot != null)
                    snapshot.restore();

                if (timeout != null) {
                    return executeWithTimeout();
                } else {
                    return supplier.get();
                }
            } catch (Exception e) {
                lastException = e;

                if (attempt < maxRetries) {
                    fireEvent(TaskEvent.retrying(name, attempt + 1, maxRetries));

                    long delay = exponentialBackoff
                            ? retryDelay.toMillis() * (1L << attempt)
                            : retryDelay.toMillis();

                    if (trace) {
                        System.out.println("[TASK] " + name + " retry " + (attempt + 1) + "/" + maxRetries + " after "
                                + delay + "ms");
                    }

                    sleep(delay);
                }
            } finally {
                if (snapshot != null)
                    snapshot.clear();
            }
        }

        throw wrapException(lastException);
    }

    private T executeWithTimeout() {
        Future<T> future = getExecutor().submit(supplier::get);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            fireEvent(TaskEvent.timedOut(name, timeout));
            throw new TaskTimeoutException("Task '" + name + "' timed out after " + timeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TaskExecutionException("Task '" + name + "' was interrupted", e);
        } catch (ExecutionException e) {
            throw wrapException(e.getCause());
        }
    }

    private ExecutorService getExecutor() {
        return switch (strategy) {
            case VIRTUAL_THREAD -> VIRTUAL_EXECUTOR;
            case FORK_JOIN -> FORK_JOIN;
            case PLATFORM_THREAD -> Executors.newCachedThreadPool();
            case CALLER -> MoreExecutors.directExecutor();
            case AUTO -> VIRTUAL_EXECUTOR; // Default to virtual threads
        };
    }

    private void fireEvent(TaskEvent event) {
        if (trace) {
            System.out.println("[TASK] " + event);
        }
        for (TaskEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception ignored) {
                // Don't let listener errors affect task execution
            }
        }
    }

    private RuntimeException wrapException(Throwable e) {
        if (e instanceof RuntimeException re)
            return re;
        return new TaskExecutionException("Task '" + name + "' failed", e);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * Direct executor that runs on caller thread.
 */
class MoreExecutors {
    static ExecutorService directExecutor() {
        return new AbstractExecutorService() {
            private volatile boolean shutdown = false;

            @Override
            public void execute(Runnable command) {
                command.run();
            }

            @Override
            public void shutdown() {
                shutdown = true;
            }

            @Override
            public List<Runnable> shutdownNow() {
                shutdown = true;
                return List.of();
            }

            @Override
            public boolean isShutdown() {
                return shutdown;
            }

            @Override
            public boolean isTerminated() {
                return shutdown;
            }

            @Override
            public boolean awaitTermination(long timeout, TimeUnit unit) {
                return true;
            }
        };
    }
}
