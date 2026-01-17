package com.fast.cqrs.cqrs.gateway;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Gateway for dispatching queries with fluent configuration options.
 * <p>
 * Provides a higher-level API compared to {@link com.fast.cqrs.cqrs.QueryBus}
 * with support for caching, timeouts, fallbacks, and parallel execution.
 * <p>
 * Example:
 * <pre>{@code
 * // Simple query
 * OrderDto order = gateway.query(new GetOrderQuery(id));
 * 
 * // With fluent options
 * OrderDto order = gateway.with(new GetOrderQuery(id))
 *     .cache(Duration.ofMinutes(5))
 *     .timeout(Duration.ofSeconds(2))
 *     .fallback(() -> OrderDto.EMPTY)
 *     .execute();
 * 
 * // Parallel queries
 * List<OrderDto> orders = gateway.parallel()
 *     .add(new GetOrderQuery("id1"))
 *     .add(new GetOrderQuery("id2"))
 *     .execute();
 * }</pre>
 *
 * @see CommandGateway
 */
public interface QueryGateway {

    /**
     * Execute a query and return the result.
     *
     * @param query the query to execute
     * @param <R> result type
     * @return query result
     */
    <R> R query(Object query);

    /**
     * Execute a query with timeout.
     *
     * @param query the query to execute
     * @param timeout maximum wait time
     * @param <R> result type
     * @return query result
     */
    <R> R query(Object query, Duration timeout);

    /**
     * Execute a query asynchronously.
     *
     * @param query the query to execute
     * @param <R> result type
     * @return future with result
     */
    <R> CompletableFuture<R> queryAsync(Object query);

    /**
     * Start building a query dispatch with fluent API.
     *
     * @param query the query to dispatch
     * @return builder for configuring dispatch options
     */
    QueryDispatch with(Object query);

    /**
     * Start building parallel query execution.
     *
     * @return parallel query builder
     */
    ParallelQueries parallel();

    /**
     * Fluent builder for query dispatch configuration.
     */
    interface QueryDispatch {
        
        /**
         * Enable caching with TTL.
         */
        QueryDispatch cache(Duration ttl);

        /**
         * Set execution timeout.
         */
        QueryDispatch timeout(Duration timeout);

        /**
         * Provide fallback result on error.
         */
        <R> QueryDispatch fallback(Supplier<R> fallback);

        /**
         * Execute synchronously.
         */
        <R> R execute();

        /**
         * Execute asynchronously.
         */
        <R> CompletableFuture<R> executeAsync();
    }

    /**
     * Builder for parallel query execution.
     */
    interface ParallelQueries {
        
        /**
         * Add a query to execute in parallel.
         */
        ParallelQueries add(Object query);

        /**
         * Set timeout for all queries.
         */
        ParallelQueries timeout(Duration timeout);

        /**
         * Execute all queries and return results.
         */
        <R> List<R> execute();

        /**
         * Execute all and aggregate results.
         */
        <R, A> A executeAndAggregate(Function<List<R>, A> aggregator);
    }
}
