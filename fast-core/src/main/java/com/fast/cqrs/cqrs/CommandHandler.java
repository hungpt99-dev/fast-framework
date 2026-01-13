package com.fast.cqrs.cqrs;

/**
 * Interface for command handlers in the CQRS pattern.
 * <p>
 * Command handlers process state-changing operations and typically
 * do not return values. Each handler is responsible for a single
 * command type.
 * <p>
 * Implementations should be registered as Spring beans and will be
 * automatically discovered by the {@link com.fast.cqrs.bus.CommandBus}.
 * <p>
 * Example:
 * <pre>{@code
 * @Component
 * public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {
 *
 *     @Override
 *     public void handle(CreateOrderCmd command) {
 *         // Business logic here
 *     }
 * }
 * }</pre>
 *
 * @param <C> the command type this handler processes
 * @see com.fast.cqrs.bus.CommandBus
 */
public interface CommandHandler<C> {

    /**
     * Handles the given command.
     *
     * @param command the command to handle
     */
    void handle(C command);

    /**
     * Returns the command type this handler can process.
     * <p>
     * Default implementation uses reflection to determine the type parameter.
     *
     * @return the command class
     */
    @SuppressWarnings("unchecked")
    default Class<C> getCommandType() {
        return (Class<C>) GenericTypeResolver.resolveTypeArgument(getClass(), CommandHandler.class, 0);
    }
}
