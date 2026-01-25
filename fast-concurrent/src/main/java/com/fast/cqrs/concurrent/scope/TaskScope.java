package com.fast.cqrs.concurrent.scope;

import com.fast.cqrs.concurrent.context.ContextSnapshot;
import com.fast.cqrs.concurrent.executor.VirtualExecutorManager;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Structured concurrency scope for managing child tasks.
 * <p>
 * Features:
 * <ul>
 * <li>Parent-child task ownership</li>
 * <li>Automatic cancellation on parent failure</li>
 * <li>Exception aggregation</li>
 * <li>Guaranteed cleanup</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * try (TaskScope scope = TaskScope.open("load-profile")) {
 *     Future<User> user = scope.fork(() -> loadUser(userId));
 *     Future<List<Order>> orders = scope.fork(() -> loadOrders(userId));
 * 
 *     scope.join(); // Wait for all
 * 
 *     return new Profile(user.get(), orders.get());
 * } // All tasks cancelled on exception or close
 * }</pre>
 */
public class TaskScope implements AutoCloseable {

    private static ExecutorService getExecutor() {
        return VirtualExecutorManager.getStaticExecutor();
    }

    private final String name;
    private final List<Future<?>> children = new CopyOnWriteArrayList<>();
    private final List<Throwable> errors = new CopyOnWriteArrayList<>();
    private final ContextSnapshot snapshot;
    private Duration timeout;
    private boolean shutdownOnFailure = false;
    private volatile boolean closed = false;

    private TaskScope(String name) {
        this.name = name;
        this.snapshot = ContextSnapshot.capture();
    }

    /**
     * Opens a new task scope.
     */
    public static TaskScope open(String name) {
        return new TaskScope(name);
    }

    /**
     * Opens an unnamed scope.
     */
    public static TaskScope open() {
        return new TaskScope("unnamed");
    }

    /**
     * Sets scope timeout.
     */
    public TaskScope timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    /**
     * Sets scope timeout.
     */
    public TaskScope timeout(long amount, TimeUnit unit) {
        this.timeout = Duration.of(amount, unit.toChronoUnit());
        return this;
    }

    /**
     * Enables shutdown on any failure.
     */
    public TaskScope shutdownOnFailure() {
        this.shutdownOnFailure = true;
        return this;
    }

    /**
     * Forks a new child task.
     */
    public <T> Future<T> fork(Supplier<T> task) {
        ensureOpen();

        Future<T> future = getExecutor().submit(() -> {
            snapshot.restore();
            try {
                return task.get();
            } catch (Exception e) {
                errors.add(e);
                if (shutdownOnFailure) {
                    shutdown();
                }
                throw e;
            } finally {
                snapshot.clear();
            }
        });

        children.add(future);
        return future;
    }

    /**
     * Forks a new child task (runnable).
     */
    public Future<Void> fork(Runnable task) {
        return fork(() -> {
            task.run();
            return null;
        });
    }

    /**
     * Waits for all forked tasks to complete.
     */
    public TaskScope join() throws InterruptedException {
        ensureOpen();

        long deadline = timeout != null
                ? System.nanoTime() + timeout.toNanos()
                : Long.MAX_VALUE;

        for (Future<?> future : children) {
            try {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    throw new TimeoutException("Scope '" + name + "' timed out");
                }
                future.get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                shutdown();
                throw new InterruptedException("Scope '" + name + "' timed out");
            } catch (ExecutionException e) {
                // Error already captured
            } catch (CancellationException ignored) {
            }
        }

        return this;
    }

    /**
     * Waits or throws if any task failed.
     */
    public TaskScope joinOrThrow() throws InterruptedException {
        join();
        throwIfFailed();
        return this;
    }

    /**
     * Throws aggregated exceptions if any task failed.
     */
    public void throwIfFailed() {
        if (!errors.isEmpty()) {
            RuntimeException aggregate = new RuntimeException(
                    "Scope '" + name + "' had " + errors.size() + " failures");
            errors.forEach(aggregate::addSuppressed);
            throw aggregate;
        }
    }

    /**
     * Shuts down all child tasks.
     */
    public void shutdown() {
        for (Future<?> future : children) {
            future.cancel(true);
        }
    }

    /**
     * Returns true if any child task failed.
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns all errors.
     */
    public List<Throwable> errors() {
        return List.copyOf(errors);
    }

    /**
     * Gets the scope name.
     */
    public String name() {
        return name;
    }

    @Override
    public void close() {
        closed = true;
        shutdown();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Scope '" + name + "' is closed");
        }
    }
}
