package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.QueryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance implementation of {@link QueryBus}.
 * <p>
 * Optimizations:
 * <ul>
 *   <li>Skip lifecycle methods when using defaults (no overhead)</li>
 *   <li>Lazy context creation only when needed</li>
 *   <li>Minimal logging in hot path</li>
 * </ul>
 */
public class DefaultQueryBus implements QueryBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultQueryBus.class);

    private final Map<Class<?>, QueryHandler<?, ?>> handlers = new ConcurrentHashMap<>();
    private final Set<Class<?>> hasCustomPreQuery = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> hasCustomPostQuery = ConcurrentHashMap.newKeySet();

    public DefaultQueryBus(List<QueryHandler<?, ?>> handlerList) {
        for (QueryHandler<?, ?> handler : handlerList) {
            Class<?> queryType = handler.getQueryType();
            if (queryType != null && queryType != Object.class) {
                handlers.put(queryType, handler);
                
                // Detect custom lifecycle methods at startup
                Class<?> handlerClass = handler.getClass();
                if (hasOverriddenMethod(handlerClass, "preQuery")) {
                    hasCustomPreQuery.add(queryType);
                }
                if (hasOverriddenMethod(handlerClass, "postQuery")) {
                    hasCustomPostQuery.add(queryType);
                }
            }
        }
        log.info("Initialized DefaultQueryBus with {} handlers", handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <Q, R> R dispatch(Q query) {
        Class<?> queryType = query.getClass();
        QueryHandler<Q, R> handler = (QueryHandler<Q, R>) handlers.get(queryType);

        if (handler == null) {
            throw new IllegalArgumentException("No handler for: " + queryType.getName());
        }

        // Fast path: no lifecycle overhead
        boolean needsLifecycle = hasCustomPreQuery.contains(queryType) 
                              || hasCustomPostQuery.contains(queryType);
        
        if (!needsLifecycle) {
            return handler.handle(query);
        }

        // Lifecycle path
        QueryContext ctx = new QueryContext();
        
        if (hasCustomPreQuery.contains(queryType)) {
            R cached = handler.preQuery(query, ctx);
            if (cached != null) {
                return cached;
            }
        }
        
        R result = handler.handle(query);
        
        if (hasCustomPostQuery.contains(queryType)) {
            handler.postQuery(query, result, ctx);
        }
        
        return result;
    }

    private boolean hasOverriddenMethod(Class<?> clazz, String methodName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Method scan failure is non-critical, log at debug level
                log.debug("Failed to scan methods in {}: {}", current.getName(), e.getMessage());
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
