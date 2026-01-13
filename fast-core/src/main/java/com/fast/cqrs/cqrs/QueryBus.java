package com.fast.cqrs.cqrs;

/**
 * Bus for dispatching query operations.
 * <p>
 * The QueryBus is responsible for routing queries to their
 * corresponding handlers. Queries are read-only operations
 * that return data without modifying system state.
 * <p>
 * Example:
 * <pre>{@code
 * @Autowired
 * private QueryBus queryBus;
 *
 * public OrderDto getOrder(String id) {
 *     return queryBus.dispatch(new GetOrderQuery(id));
 * }
 * }</pre>
 *
 * @see com.fast.cqrs.handler.QueryHandler
 */
public interface QueryBus {

    /**
     * Dispatches the given query to its handler and returns the result.
     *
     * @param query the query to dispatch
     * @param <Q>   the query type
     * @param <R>   the result type
     * @return the query result
     * @throws IllegalArgumentException if no handler is found for the query
     */
    <Q, R> R dispatch(Q query);
}
