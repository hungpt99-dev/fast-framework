package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.annotation.Query;
import com.fast.cqrs.cqrs.CommandBus;
import com.fast.cqrs.cqrs.QueryBus;
import com.fast.cqrs.web.HttpInvocationContext;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.springframework.context.ApplicationContextAware;

/**
 * Central CQRS dispatcher that routes requests to the appropriate bus.
 * <p>
 * Supports routing via:
 * <ul>
 * <li>@Query/@Command with explicit handler class</li>
 * <li>@Query/@Command with query/command class</li>
 * <li>Auto-detection from @ModelAttribute parameters</li>
 * <li>Auto-detection from method parameters</li>
 * </ul>
 * <p>
 * For @Query with @ModelAttribute:
 * 
 * <pre>
 * {@code
 * &#64;Query
 * &#64;GetMapping
 * List<OrderDto> listOrders(@ModelAttribute ListOrdersQuery query);
 * 
 * @Query(handler = GetOrderHandler.class)
 * &#64;GetMapping("/{id}")
 * OrderDto getOrder(@PathVariable String id, @ModelAttribute GetOrderQuery query);
 * }
 * </pre>
 */
public class CqrsDispatcher implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(CqrsDispatcher.class);

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private ApplicationContext applicationContext;

    public CqrsDispatcher(CommandBus commandBus, QueryBus queryBus) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Object dispatch(HttpInvocationContext context) {
        Method method = context.method();

        boolean isQuery = method.isAnnotationPresent(Query.class);
        boolean isCommand = method.isAnnotationPresent(Command.class);

        if (!isQuery && !isCommand) {
            throw new CqrsDispatchException(
                    "Method '" + method.getName() + "' must be annotated with @Query or @Command");
        }

        if (isQuery && isCommand) {
            throw new CqrsDispatchException(
                    "Method '" + method.getName() + "' cannot have both @Query and @Command");
        }

        if (isQuery) {
            return dispatchQuery(context, method.getAnnotation(Query.class));
        } else {
            return dispatchCommand(context, method.getAnnotation(Command.class));
        }
    }

    @SuppressWarnings("unchecked")
    private Object dispatchQuery(HttpInvocationContext context, Query annotation) {
        Method method = context.method();

        // Option 1: Explicit handler class specified
        if (annotation.handler() != Query.DefaultHandler.class) {
            QueryHandler<Object, Object> handler = getHandler(annotation.handler());
            Object query = extractQueryObject(annotation, context, handler);
            log.debug("Dispatching query {} to handler {}",
                    query.getClass().getSimpleName(),
                    annotation.handler().getSimpleName());
            return handler.handle(query);
        }

        // Option 2: Explicit query class specified
        if (annotation.query() != Void.class) {
            Object query = buildObjectFromParams(annotation.query(), context.arguments());
            log.debug("Dispatching query {} via QueryBus", query.getClass().getSimpleName());
            return queryBus.dispatch(query);
        }

        // Option 3: Auto-detect from @ModelAttribute parameter
        Object modelAttributeQuery = extractModelAttributeParam(method, context.arguments());
        if (modelAttributeQuery != null) {
            log.debug("Dispatching @ModelAttribute query {} via QueryBus",
                    modelAttributeQuery.getClass().getSimpleName());
            return queryBus.dispatch(modelAttributeQuery);
        }

        // Option 4: Auto-detect from other parameters
        Object query = extractPayload(context);
        if (query != null) {
            log.debug("Dispatching auto-detected query {} via QueryBus",
                    query.getClass().getSimpleName());
            return queryBus.dispatch(query);
        }

        // Option 5: Simple query wrapper
        log.debug("Dispatching SimpleQuery for method {}", method.getName());
        return queryBus.dispatch(new SimpleQuery(method, context.arguments()));
    }

    private Object extractQueryObject(Query annotation, HttpInvocationContext context,
            QueryHandler<Object, Object> handler) {
        // If explicit query class specified, use it
        if (annotation.query() != Void.class) {
            return buildObjectFromParams(annotation.query(), context.arguments());
        }

        // Try to find @ModelAttribute parameter
        Object modelAttrQuery = extractModelAttributeParam(context.method(), context.arguments());
        if (modelAttrQuery != null) {
            return modelAttrQuery;
        }

        // Infer from handler generic type
        Class<?> queryType = handler.getQueryType();
        if (queryType != null && queryType != Object.class) {
            // Try to find matching argument
            for (Object arg : context.arguments()) {
                if (arg != null && queryType.isAssignableFrom(arg.getClass())) {
                    return arg;
                }
            }
            // Build from params
            return buildObjectFromParams(queryType, context.arguments());
        }

        // Fallback to SimpleQuery
        return new SimpleQuery(context.method(), context.arguments());
    }

    /**
     * Extracts the parameter annotated with @ModelAttribute.
     */
    private Object extractModelAttributeParam(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (hasModelAttributeAnnotation(params[i])) {
                if (i < args.length && args[i] != null) {
                    return args[i];
                }
            }
        }
        return null;
    }

    private boolean hasModelAttributeAnnotation(Parameter param) {
        for (Annotation ann : param.getAnnotations()) {
            if (ann.annotationType().getSimpleName().equals("ModelAttribute")) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Object dispatchCommand(HttpInvocationContext context, Command annotation) {
        Method method = context.method();

        // Option 1: Explicit handler class
        if (annotation.handler() != Command.DefaultHandler.class) {
            CommandHandler<Object> handler = getHandler(annotation.handler());
            Object command = extractCommandObject(annotation, context, handler);
            log.debug("Dispatching command {} to handler {}",
                    command.getClass().getSimpleName(),
                    annotation.handler().getSimpleName());
            handler.handle(command);
            return null;
        }

        // Option 2: Explicit command class
        if (annotation.command() != Void.class) {
            Object command = buildObjectFromParams(annotation.command(), context.arguments());
            log.debug("Dispatching command {} via CommandBus", command.getClass().getSimpleName());
            commandBus.dispatch(command);
            return null;
        }

        // Option 3: Auto-detect from parameters
        Object command = extractPayload(context);
        if (command != null) {
            log.debug("Dispatching auto-detected command {} via CommandBus",
                    command.getClass().getSimpleName());
            commandBus.dispatch(command);
        } else {
            log.debug("Dispatching SimpleCommand for method {}", method.getName());
            commandBus.dispatch(new SimpleCommand(method, context.arguments()));
        }
        return null;
    }

    private Object extractCommandObject(Command annotation, HttpInvocationContext context,
            CommandHandler<Object> handler) {
        // If explicit command class specified, use it
        if (annotation.command() != Void.class) {
            return buildObjectFromParams(annotation.command(), context.arguments());
        }

        // Infer from handler generic type
        Class<?> commandType = handler.getCommandType();
        if (commandType != null && commandType != Object.class) {
            // Try to find matching argument
            for (Object arg : context.arguments()) {
                if (arg != null && commandType.isAssignableFrom(arg.getClass())) {
                    return arg;
                }
            }
            // Build from params
            return buildObjectFromParams(commandType, context.arguments());
        }

        // Fallback to SimpleCommand
        return new SimpleCommand(context.method(), context.arguments());
    }

    @SuppressWarnings("unchecked")
    private <T> T getHandler(Class<?> handlerClass) {
        if (applicationContext != null) {
            return (T) applicationContext.getBean(handlerClass);
        }
        throw new CqrsDispatchException("ApplicationContext not available for handler lookup");
    }

    private Object buildObjectFromParams(Class<?> targetClass, Object[] args) {
        // If single argument matches target type, return it directly
        if (args.length == 1 && args[0] != null && targetClass.isAssignableFrom(args[0].getClass())) {
            return args[0];
        }

        // Look for matching argument
        for (Object arg : args) {
            if (arg != null && targetClass.isAssignableFrom(arg.getClass())) {
                return arg;
            }
        }

        try {
            // Try constructor matching args
            for (Constructor<?> ctor : targetClass.getConstructors()) {
                if (ctor.getParameterCount() == args.length) {
                    return ctor.newInstance(args);
                }
            }
            // Try no-arg constructor
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new CqrsDispatchException("Failed to create " + targetClass.getSimpleName(), e);
        }
    }

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

    public record SimpleQuery(Method method, Object[] arguments) {
    }

    public record SimpleCommand(Method method, Object[] arguments) {
    }
}
