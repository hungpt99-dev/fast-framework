package com.fast.cqrs.cqrs;

import com.fast.cqrs.cqrs.context.CommandContext;

/**
 * Interface for command handlers in the CQRS pattern with lifecycle hooks.
 * <p>
 * Command handlers process state-changing operations. Each handler is 
 * responsible for a single command type.
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 *   <li>{@link #preHandle} - Validation, security, logging (can abort)</li>
 *   <li>{@link #handle} - Main business logic</li>
 *   <li>{@link #postHandle} - Audit, notifications, cleanup</li>
 *   <li>{@link #onError} - Error recovery or propagation</li>
 * </ol>
 * <p>
 * Example:
 * <pre>{@code
 * @Component
 * public class CreateOrderHandler implements CommandHandler<CreateOrderCmd> {
 *     
 *     @Override
 *     public boolean preHandle(CreateOrderCmd cmd, CommandContext ctx) {
 *         if (!ctx.hasPermission("orders:create")) {
 *             throw new AccessDeniedException("Cannot create orders");
 *         }
 *         return true;
 *     }
 *
 *     @Override
 *     public void handle(CreateOrderCmd command) {
 *         // Business logic
 *     }
 *     
 *     @Override
 *     public void postHandle(CreateOrderCmd cmd, CommandContext ctx) {
 *         auditService.log("ORDER_CREATED", cmd);
 *     }
 * }
 * }</pre>
 *
 * @param <C> the command type this handler processes
 * @see com.fast.cqrs.cqrs.CommandBus
 */
public interface CommandHandler<C> {

    // ==================== LIFECYCLE HOOKS ====================

    /**
     * Called before {@link #handle(Object)}.
     * <p>
     * Use for:
     * <ul>
     *   <li>Authorization checks</li>
     *   <li>Input validation</li>
     *   <li>Logging / tracing</li>
     *   <li>Idempotency checks</li>
     * </ul>
     *
     * @param command the command to process
     * @param ctx context with metadata and user info
     * @return true to proceed, false to skip handler (use ctx.setResult() first)
     */
    default boolean preHandle(C command, CommandContext ctx) {
        return true;
    }

    /**
     * Main handler method - processes the command.
     *
     * @param command the command to handle
     */
    void handle(C command);

    /**
     * Called after successful {@link #handle(Object)}.
     * <p>
     * Use for:
     * <ul>
     *   <li>Audit logging</li>
     *   <li>Sending notifications</li>
     *   <li>Publishing domain events</li>
     *   <li>Cleanup</li>
     * </ul>
     *
     * @param command the processed command
     * @param ctx context with metadata
     */
    default void postHandle(C command, CommandContext ctx) {
        // Default: no-op
    }

    /**
     * Called when an error occurs in {@link #handle(Object)}.
     * <p>
     * Can be used to:
     * <ul>
     *   <li>Log errors</li>
     *   <li>Transform exceptions</li>
     *   <li>Attempt recovery</li>
     *   <li>Rollback side effects</li>
     * </ul>
     *
     * @param command the command that caused the error
     * @param error the thrown exception
     * @param ctx context with metadata
     * @throws RuntimeException to propagate the error (default behavior)
     */
    default void onError(C command, Throwable error, CommandContext ctx) {
        if (error instanceof RuntimeException re) {
            throw re;
        }
        throw new RuntimeException(error);
    }

    // ==================== TYPE RESOLUTION ====================

    /**
     * Returns the command type this handler can process.
     *
     * @return the command class
     */
    @SuppressWarnings("unchecked")
    default Class<C> getCommandType() {
        return (Class<C>) GenericTypeResolver.resolveTypeArgument(getClass(), CommandHandler.class, 0);
    }
}
