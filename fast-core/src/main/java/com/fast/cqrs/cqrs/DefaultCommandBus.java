package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance implementation of {@link CommandBus}.
 * <p>
 * Optimizations:
 * <ul>
 *   <li>Skip lifecycle methods when using defaults (no overhead)</li>
 *   <li>Lazy context creation only when needed</li>
 *   <li>Minimal logging in hot path</li>
 * </ul>
 */
public class DefaultCommandBus implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultCommandBus.class);

    private final Map<Class<?>, CommandHandler<?>> handlers = new ConcurrentHashMap<>();
    private final Set<Class<?>> hasCustomPreHandle = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> hasCustomPostHandle = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> hasCustomOnError = ConcurrentHashMap.newKeySet();

    public DefaultCommandBus(List<CommandHandler<?>> handlerList) {
        for (CommandHandler<?> handler : handlerList) {
            Class<?> commandType = handler.getCommandType();
            if (commandType != null && commandType != Object.class) {
                handlers.put(commandType, handler);
                
                // Detect custom lifecycle methods at startup (reflection once, not per-dispatch)
                Class<?> handlerClass = handler.getClass();
                if (hasOverriddenMethod(handlerClass, "preHandle")) {
                    hasCustomPreHandle.add(commandType);
                }
                if (hasOverriddenMethod(handlerClass, "postHandle")) {
                    hasCustomPostHandle.add(commandType);
                }
                if (hasOverriddenMethod(handlerClass, "onError")) {
                    hasCustomOnError.add(commandType);
                }
            }
        }
        log.info("Initialized DefaultCommandBus with {} handlers", handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> void dispatch(C command) {
        Class<?> commandType = command.getClass();
        CommandHandler<C> handler = (CommandHandler<C>) handlers.get(commandType);

        if (handler == null) {
            throw new IllegalArgumentException("No handler for: " + commandType.getName());
        }

        // Fast path: no lifecycle overhead if only handle() is implemented
        boolean needsLifecycle = hasCustomPreHandle.contains(commandType) 
                              || hasCustomPostHandle.contains(commandType)
                              || hasCustomOnError.contains(commandType);
        
        if (!needsLifecycle) {
            // Direct execution - maximum performance
            handler.handle(command);
            return;
        }

        // Lifecycle path
        CommandContext ctx = new CommandContext();
        try {
            if (hasCustomPreHandle.contains(commandType)) {
                if (!handler.preHandle(command, ctx) || ctx.shouldSkipHandler()) {
                    return;
                }
            }
            
            handler.handle(command);
            
            if (hasCustomPostHandle.contains(commandType)) {
                handler.postHandle(command, ctx);
            }
        } catch (Throwable error) {
            if (hasCustomOnError.contains(commandType)) {
                handler.onError(command, error, ctx);
            } else {
                throw new CqrsDispatchException("Command dispatch failed: " + commandType.getSimpleName(), error);
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
                // Method scan failure is non-critical, log at debug level
                log.debug("Failed to scan methods in {}: {}", current.getName(), e.getMessage());
            }
            current = current.getSuperclass();
        }
        return false;
    }
}
