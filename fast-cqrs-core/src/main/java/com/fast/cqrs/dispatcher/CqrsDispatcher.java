package com.fast.cqrs.dispatcher;

import com.fast.cqrs.annotation.Command;
import com.fast.cqrs.annotation.Query;
import com.fast.cqrs.bus.CommandBus;
import com.fast.cqrs.bus.QueryBus;
import com.fast.cqrs.context.HttpInvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Central CQRS dispatcher that routes requests to the appropriate bus.
 * <p>
 * This is the enforcement point for CQRS rules in the framework.
 * It inspects controller method annotations and routes to either
 * the {@link CommandBus} or {@link QueryBus}.
 * <p>
 * Methods must be annotated with either {@link Query} or {@link Command}.
 * Unannotated methods will cause a fail-fast error.
 */
public class CqrsDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CqrsDispatcher.class);

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    /**
     * Creates a new CqrsDispatcher.
     *
     * @param commandBus the command bus for dispatching commands
     * @param queryBus   the query bus for dispatching queries
     */
    public CqrsDispatcher(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    /**
     * Dispatches an HTTP invocation to the appropriate bus.
     *
     * @param context the invocation context containing method and arguments
     * @return the result of the operation (null for commands)
     * @throws CqrsDispatchException if the method is not properly annotated
     */
    public Object dispatch(HttpInvocationContext context) {
        Method method = context.method();
        
        boolean isQuery = method.isAnnotationPresent(Query.class);
        boolean isCommand = method.isAnnotationPresent(Command.class);

        // Validate CQRS annotations
        if (!isQuery && !isCommand) {
            throw new CqrsDispatchException(
                "Method '" + method.getName() + "' in " + method.getDeclaringClass().getSimpleName() +
                " must be annotated with @Query or @Command"
            );
        }

        if (isQuery && isCommand) {
            throw new CqrsDispatchException(
                "Method '" + method.getName() + "' in " + method.getDeclaringClass().getSimpleName() +
                " cannot be annotated with both @Query and @Command"
            );
        }

        // Dispatch to appropriate bus
        if (isQuery) {
            return dispatchQuery(context);
        } else {
            return dispatchCommand(context);
        }
    }

    private Object dispatchQuery(HttpInvocationContext context) {
        Object query = extractPayload(context);
        log.debug("Dispatching query from method: {}", context.method().getName());
        
        if (query == null) {
            // For queries without a request body, create a simple query object
            return queryBus.dispatch(new SimpleQuery(context.method(), context.arguments()));
        }
        
        return queryBus.dispatch(query);
    }

    private Object dispatchCommand(HttpInvocationContext context) {
        Object command = extractPayload(context);
        log.debug("Dispatching command from method: {}", context.method().getName());
        
        if (command == null) {
            // For commands without a request body, create a simple command object
            commandBus.dispatch(new SimpleCommand(context.method(), context.arguments()));
        } else {
            commandBus.dispatch(command);
        }
        
        return null; // Commands don't return values
    }

    /**
     * Extracts the primary payload (command/query object) from the context.
     * This looks for the first complex object argument (not primitive/String).
     */
    private Object extractPayload(HttpInvocationContext context) {
        for (Object arg : context.arguments()) {
            if (arg != null && isPayloadType(arg.getClass())) {
                return arg;
            }
        }
        return null;
    }

    private boolean isPayloadType(Class<?> type) {
        return !type.isPrimitive() &&
               !type.equals(String.class) &&
               !Number.class.isAssignableFrom(type) &&
               !type.equals(Boolean.class);
    }

    /**
     * Simple query wrapper for methods without explicit query objects.
     */
    public record SimpleQuery(Method method, Object[] arguments) {}

    /**
     * Simple command wrapper for methods without explicit command objects.
     */
    public record SimpleCommand(Method method, Object[] arguments) {}
}
