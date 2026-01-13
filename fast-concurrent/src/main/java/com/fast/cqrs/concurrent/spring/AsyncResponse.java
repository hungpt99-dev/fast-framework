package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.flow.FlowResult;
import com.fast.cqrs.concurrent.flow.ParallelFlow;

import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Async response builder for Spring MVC controllers.
 * <p>
 * Enables fluent async responses with parallel task execution.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * @GetMapping("/profile/{id}")
 * public AsyncResponse<Profile> getProfile(@PathVariable Long id) {
 *     return AsyncResponse.parallel()
 *             .task("user", () -> loadUser(id))
 *             .task("orders", () -> loadOrders(id))
 *             .map((user, orders) -> new Profile(user, orders))
 *             .timeout(3, TimeUnit.SECONDS);
 * }
 * }</pre>
 */
public class AsyncResponse<T> {

    private final CompletableFuture<T> future;

    private AsyncResponse(CompletableFuture<T> future) {
        this.future = future;
    }

    /**
     * Creates a simple async response.
     */
    public static <T> AsyncResponse<T> of(Supplier<T> supplier) {
        return new AsyncResponse<>(CompletableFuture.supplyAsync(supplier));
    }

    /**
     * Creates a parallel flow builder.
     */
    public static ParallelBuilder parallel() {
        return new ParallelBuilder();
    }

    /**
     * Gets the underlying future.
     */
    public CompletableFuture<T> future() {
        return future;
    }

    /**
     * Gets the result, blocking if necessary.
     */
    public T get() {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Gets the result with timeout.
     */
    public T get(long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Request timed out", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    /**
     * Builder for parallel async responses.
     */
    public static class ParallelBuilder {
        private final ParallelFlow flow = ParallelFlow.of();

        public <T> ParallelBuilder task(String name, Supplier<T> supplier) {
            flow.task(name, supplier);
            return this;
        }

        public ParallelBuilder timeout(long amount, TimeUnit unit) {
            flow.timeout(amount, unit);
            return this;
        }

        public ParallelBuilder failFast() {
            flow.failFast();
            return this;
        }

        /**
         * Maps two task results into a response.
         */
        public <A, B, R> AsyncResponse<R> map(
                String task1, String task2,
                BiFunction<A, B, R> mapper) {
            return new AsyncResponse<>(CompletableFuture.supplyAsync(() -> {
                FlowResult result = flow.execute();
                A a = result.get(task1);
                B b = result.get(task2);
                return mapper.apply(a, b);
            }));
        }

        /**
         * Maps all results using a custom mapper.
         */
        public <R> AsyncResponse<R> map(java.util.function.Function<FlowResult, R> mapper) {
            return new AsyncResponse<>(CompletableFuture.supplyAsync(() -> {
                FlowResult result = flow.execute();
                return mapper.apply(result);
            }));
        }

        /**
         * Returns the raw flow result.
         */
        public AsyncResponse<FlowResult> execute() {
            return new AsyncResponse<>(CompletableFuture.supplyAsync(flow::execute));
        }
    }
}
