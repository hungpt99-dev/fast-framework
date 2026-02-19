package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.EventContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance implementation of {@link EventBus}.
 * Supports multiple handlers per event type.
 */
public class DefaultEventBus implements EventBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventBus.class);

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
    private final Set<Class<?>> hasCustomPreHandle = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> hasCustomPostHandle = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> hasCustomOnError = ConcurrentHashMap.newKeySet();

    public DefaultEventBus(List<EventHandler<?>> handlerList) {
        for (EventHandler<?> handler : handlerList) {
            Class<?> eventType = handler.getEventType();
            if (eventType != null && eventType != Object.class) {
                handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
                
                // Detect custom lifecycle methods
                Class<?> handlerClass = handler.getClass();
                if (hasOverriddenMethod(handlerClass, "preHandle")) {
                    hasCustomPreHandle.add(eventType);
                }
                if (hasOverriddenMethod(handlerClass, "postHandle")) {
                    hasCustomPostHandle.add(eventType);
                }
                if (hasOverriddenMethod(handlerClass, "onError")) {
                    hasCustomOnError.add(eventType);
                }
            }
        }
        log.info("Initialized DefaultEventBus with {} event types", handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> void publish(E event) {
        Class<?> eventType = event.getClass();
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            return; // No handlers, just ignore
        }

        for (EventHandler<?> h : eventHandlers) {
            EventHandler<E> handler = (EventHandler<E>) h;
            dispatchToHandler(handler, event, eventType);
        }
    }

    private <E> void dispatchToHandler(EventHandler<E> handler, E event, Class<?> eventType) {
        // Fast path check - simplistic since pre/post/error sets are per-event-type, 
        // but here multiple handlers might exist. If ANY handler overrides, we construct context.
        // For correctness with multiple handlers, we should likely track per-handler customization.
        // However, following the CommandBus pattern which optimizes per Type. 
        // We'll treat the sets as "does any handler for this event type need lifecycle?" 
        // Actually, existing code in DefaultCommandBus assumes 1:1 mapping. 
        // For 1:N, if ANY handler overrides, we pay the cost. Optimized enough.
        
        boolean needsLifecycle = hasCustomPreHandle.contains(eventType) 
                              || hasCustomPostHandle.contains(eventType)
                              || hasCustomOnError.contains(eventType);

        if (!needsLifecycle) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                 log.error("Error handling event {} with handler {}", eventType.getSimpleName(), handler.getClass().getSimpleName(), e);
            }
            return;
        }

        EventContext ctx = new EventContext();
        try {
            if (hasCustomPreHandle.contains(eventType)) {
                if (!handler.preHandle(event, ctx) || ctx.shouldSkipHandler()) {
                    return;
                }
            }
            
            handler.handle(event);
            
            if (hasCustomPostHandle.contains(eventType)) {
                handler.postHandle(event, ctx);
            }
        } catch (Throwable error) {
            if (hasCustomOnError.contains(eventType)) {
                handler.onError(event, error, ctx);
            } else {
                log.error("Error dispatching event {} to {}", eventType.getSimpleName(), handler.getClass().getSimpleName(), error);
            }
        }
    }

    private boolean hasOverriddenMethod(Class<?> clazz, String methodName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(methodName) && !method.isSynthetic() && !method.isBridge()) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
