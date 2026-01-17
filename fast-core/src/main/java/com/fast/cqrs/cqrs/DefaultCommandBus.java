package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link CommandBus} with handler lifecycle support.
 * <p>
 * This implementation:
 * <ul>
 *   <li>Discovers all {@link CommandHandler} beans</li>
 *   <li>Routes commands to the appropriate handler</li>
 *   <li>Invokes lifecycle methods: preHandle → handle → postHandle</li>
 *   <li>Calls onError on exception</li>
 * </ul>
 */
public class DefaultCommandBus implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultCommandBus.class);

    private final Map<Class<?>, CommandHandler<?>> handlers = new ConcurrentHashMap<>();

    /**
     * Creates a new DefaultCommandBus with the given handlers.
     *
     * @param handlerList the list of command handlers to register
     */
    public DefaultCommandBus(List<CommandHandler<?>> handlerList) {
        for (CommandHandler<?> handler : handlerList) {
            Class<?> commandType = handler.getCommandType();
            if (commandType != null && commandType != Object.class) {
                handlers.put(commandType, handler);
                log.debug("Registered command handler: {} for type: {}", 
                         handler.getClass().getSimpleName(), commandType.getSimpleName());
            }
        }
        log.info("Initialized DefaultCommandBus with {} handlers", handlers.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <C> void dispatch(C command) {
        if (command == null) {
            throw new IllegalArgumentException("Command cannot be null");
        }

        Class<?> commandType = command.getClass();
        CommandHandler<C> handler = (CommandHandler<C>) handlers.get(commandType);

        if (handler == null) {
            throw new IllegalArgumentException(
                "No handler found for command type: " + commandType.getName()
            );
        }

        log.debug("Dispatching command: {} to handler: {}", 
                  commandType.getSimpleName(), handler.getClass().getSimpleName());
        
        // Create context for lifecycle
        CommandContext ctx = new CommandContext();
        
        try {
            // === LIFECYCLE: preHandle ===
            boolean proceed = handler.preHandle(command, ctx);
            
            if (!proceed || ctx.shouldSkipHandler()) {
                log.debug("Handler execution skipped by preHandle");
                return;
            }
            
            // === LIFECYCLE: handle ===
            handler.handle(command);
            
            // === LIFECYCLE: postHandle ===
            handler.postHandle(command, ctx);
            
        } catch (Throwable error) {
            // === LIFECYCLE: onError ===
            log.debug("Command execution error, invoking onError lifecycle", error);
            handler.onError(command, error, ctx);
        }
    }
}
