package com.fast.cqrs.eventsourcing;

import com.fast.cqrs.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Invokes @EventHandler methods on aggregates using reflection.
 */
public class EventHandlerInvoker {

    private static final Logger log = LoggerFactory.getLogger(EventHandlerInvoker.class);

    private static final Map<Class<?>, Map<Class<?>, Method>> HANDLER_CACHE = new ConcurrentHashMap<>();

    /**
     * Invokes the appropriate event handler method on the aggregate.
     */
    public static void invoke(Object aggregate, DomainEvent event) {
        Class<?> aggregateClass = aggregate.getClass();
        Class<?> eventClass = event.getClass();

        Method handler = findHandler(aggregateClass, eventClass);
        if (handler == null) {
            log.warn("No @EventHandler found for event {} in aggregate {}", 
                     eventClass.getSimpleName(), aggregateClass.getSimpleName());
            return;
        }

        try {
            handler.setAccessible(true);
            handler.invoke(aggregate, event);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke event handler for " + eventClass.getSimpleName(), e);
        }
    }

    private static Method findHandler(Class<?> aggregateClass, Class<?> eventClass) {
        Map<Class<?>, Method> handlers = HANDLER_CACHE.computeIfAbsent(aggregateClass, 
            clazz -> buildHandlerMap(clazz));
        return handlers.get(eventClass);
    }

    private static Map<Class<?>, Method> buildHandlerMap(Class<?> aggregateClass) {
        Map<Class<?>, Method> handlers = new ConcurrentHashMap<>();
        
        for (Method method : aggregateClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ApplyEvent.class)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && DomainEvent.class.isAssignableFrom(params[0])) {
                    handlers.put(params[0], method);
                }
            }
        }
        
        return handlers;
    }
}
