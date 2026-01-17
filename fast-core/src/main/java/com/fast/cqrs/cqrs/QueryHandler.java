package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.QueryContext;

/**
 * Interface for query handlers in the CQRS pattern with lifecycle hooks.
 * <p>
 * Query handlers process read-only operations and return data.
 * Each handler is responsible for a single query type.
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>{@link #preQuery} - Cache lookup, validation (can return early)</li>
 *   <li>{@link #handle} - Main query execution</li>
 *   <li>{@link #postQuery} - Cache update, logging</li>
 * </ol>
 * <p>
 * Example with caching:
 * <pre>{@code
 * @Component
 * public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {
 *     
 *     @Override
 *     public OrderDto preQuery(GetOrderQuery query, QueryContext ctx) {
 *         // Return cached result or null to proceed
 *         return cache.get("order:" + query.id(), OrderDto.class);
 *     }
 *
 *     @Override
 *     public OrderDto handle(GetOrderQuery query) {
 *         return repository.findById(query.id())
 *             .map(this::toDto)
 *             .orElseThrow(() -> new NotFoundException());
 *     }
 *     
 *     @Override
 *     public void postQuery(GetOrderQuery query, OrderDto result, QueryContext ctx) {
 *         cache.put("order:" + query.id(), result, Duration.ofMinutes(5));
 *     }
 * }
 * }</pre>
 *
 * @param <Q> the query type this handler processes
 * @param <R> the result type returned by this handler
 * @see com.fast.cqrs.cqrs.QueryBus
 */
public interface QueryHandler<Q, R> {

    // ==================== LIFECYCLE HOOKS ====================

    /**
     * Called before {@link #handle(Object)}.
     * <p>
     * Use for:
     * <ul>
     *   <li>Cache lookups - return cached result to skip handle()</li>
     *   <li>Authorization checks</li>
     *   <li>Input validation</li>
     * </ul>
     *
     * @param query the query to process
     * @param ctx context with metadata
     * @return cached result to skip handle(), or null to proceed
     */
    default R preQuery(Q query, QueryContext ctx) {
        return null;  // Proceed to handle()
    }

    /**
     * Main handler method - executes the query.
     *
     * @param query the query to handle
     * @return the query result
     */
    R handle(Q query);

    /**
     * Called after successful {@link #handle(Object)}.
     * <p>
     * Use for:
     * <ul>
     *   <li>Caching results</li>
     *   <li>Logging / metrics</li>
     *   <li>Result transformation</li>
     * </ul>
     *
     * @param query the processed query
     * @param result the query result
     * @param ctx context with metadata
     */
    default void postQuery(Q query, R result, QueryContext ctx) {
        // Default: no-op
    }

    // ==================== TYPE RESOLUTION ====================

    /**
     * Returns the query type this handler can process.
     *
     * @return the query class
     */
    @SuppressWarnings("unchecked")
    default Class<Q> getQueryType() {
        return (Class<Q>) GenericTypeResolver.resolveTypeArgument(getClass(), QueryHandler.class, 0);
    }
}
