package com.fast.cqrs.cqrs;

/**
 * Interface for query handlers in the CQRS pattern.
 * <p>
 * Query handlers process read-only operations and return data.
 * Each handler is responsible for a single query type.
 * <p>
 * Implementations should be registered as Spring beans and will be
 * automatically discovered by the {@link com.fast.cqrs.bus.QueryBus}.
 * <p>
 * Example:
 * <pre>{@code
 * @Component
 * public class GetOrderHandler implements QueryHandler<GetOrderQuery, OrderDto> {
 *
 *     @Override
 *     public OrderDto handle(GetOrderQuery query) {
 *         // Return data here
 *     }
 * }
 * }</pre>
 *
 * @param <Q> the query type this handler processes
 * @param <R> the result type returned by this handler
 * @see com.fast.cqrs.bus.QueryBus
 */
public interface QueryHandler<Q, R> {

    /**
     * Handles the given query and returns the result.
     *
     * @param query the query to handle
     * @return the query result
     */
    R handle(Q query);

    /**
     * Returns the query type this handler can process.
     * <p>
     * Default implementation uses reflection to determine the type parameter.
     *
     * @return the query class
     */
    @SuppressWarnings("unchecked")
    default Class<Q> getQueryType() {
        return (Class<Q>) GenericTypeResolver.resolveTypeArgument(getClass(), QueryHandler.class, 0);
    }
}
