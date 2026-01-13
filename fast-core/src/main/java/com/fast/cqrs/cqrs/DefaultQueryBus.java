package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link QueryBus}.
 * <p>
 * This implementation discovers all {@link QueryHandler} beans and
 * routes queries to the appropriate handler based on query type.
 */
public class DefaultQueryBus implements QueryBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultQueryBus.class);

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultQueryBus with the given handlers.
     *
     * @param handlerList the list of query handlers to register
     */
    public DefaultQueryBus(List<QueryHandler<?, ?>> handlerList) {
        for (QueryHandler<?, ?> handler : handlerList) {
            Class<?> queryType = handler.getQueryType();
            if (queryType != null && queryType != Object.class) {
                handlers.put(queryType, handler);
                log.debug("Registered query handler: {} for type: {}", 
                         handler.getClass().getSimpleName(), queryType.getSimpleName());
            }
        }
        log.info("Initialized DefaultQueryBus with {} handlers", handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Q, R> R dispatch(Q query) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        Class<?> queryType = query.getClass();
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(queryType);

        if (handler == null) {
            throw new IllegalArgumentException(
                "No handler found for query type: " + queryType.getName()
            );
        }

        log.debug("Dispatching query: {} to handler: {}", 
                  queryType.getSimpleName(), handler.getClass().getSimpleName());
        
        return handler.handle(query);
    }
}
