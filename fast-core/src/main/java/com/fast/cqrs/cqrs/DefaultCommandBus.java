package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link CommandBus}.
 * <p>
 * This implementation discovers all {@link CommandHandler} beans and
 * routes commands to the appropriate handler based on command type.
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
        
        handler.handle(command);
    }
}
