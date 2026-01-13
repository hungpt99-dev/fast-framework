package com.fast.cqrs.cqrs;

/**
 * Bus for dispatching command operations.
 * <p>
 * The CommandBus is responsible for routing commands to their
 * corresponding handlers. Commands are state-changing operations
 * that typically do not return values.
 * <p>
 * Example:
 * <pre>{@code
 * @Autowired
 * private CommandBus commandBus;
 *
 * public void processOrder() {
 *     commandBus.dispatch(new CreateOrderCmd("product-123", 2));
 * }
 * }</pre>
 *
 * @see com.fast.cqrs.handler.CommandHandler
 */
public interface CommandBus {

    /**
     * Dispatches the given command to its handler.
     *
     * @param command the command to dispatch
     * @param <C>     the command type
     * @throws IllegalArgumentException if no handler is found for the command
     */
    <C> void dispatch(C command);
}
