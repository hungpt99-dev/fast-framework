package com.fast.cqrs.logging.audit;

import com.fast.cqrs.cqrs.CommandBus;
import com.fast.cqrs.logging.context.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decorator that wraps a {@link CommandBus} to automatically audit all commands.
 * <p>
 * Every command dispatched through this bus will be logged before execution,
 * and the result (success or failure) will be recorded.
 * <p>
 * This implements the Decorator pattern, allowing audit functionality to be
 * added without modifying the underlying CommandBus implementation.
 */
public class AuditingCommandBusDecorator implements CommandBus {

    private static final Logger log = LoggerFactory.getLogger(AuditingCommandBusDecorator.class);

    private final CommandBus delegate;
    private final AuditLogger auditLogger;

    /**
     * Creates a new auditing command bus decorator.
     *
     * @param delegate    the underlying command bus to delegate to
     * @param auditLogger the audit logger to use
     */
    public AuditingCommandBusDecorator(CommandBus delegate, AuditLogger auditLogger) {
        this.delegate = delegate;
        this.auditLogger = auditLogger;
    }

    @Override
    public <C> void dispatch(C command) {
        String commandName = command.getClass().getSimpleName();
        String actor = getCurrentActor();
        String traceId = TraceContext.getTraceId();

        log.debug("Auditing command: {} by actor: {}", commandName, actor);

        try {
            delegate.dispatch(command);

            auditLogger.log(AuditEvent.builder()
                    .action(commandName)
                    .actor(actor)
                    .resource("Command")
                    .success(true)
                    .traceId(traceId)
                    .build());

        } catch (Exception e) {
            auditLogger.log(AuditEvent.builder()
                    .action(commandName)
                    .actor(actor)
                    .resource("Command")
                    .success(false)
                    .traceId(traceId)
                    .build());

            throw e;
        }
    }

    /**
     * Gets the current actor from the security context.
     * <p>
     * Override this method to customize actor resolution.
     *
     * @return the current actor name, or "ANONYMOUS" if not authenticated
     */
    protected String getCurrentActor() {
        try {
            // Try to get actor from Spring Security if available
            Class<?> securityContextHolder = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder"
            );
            Object context = securityContextHolder.getMethod("getContext").invoke(null);
            Object authentication = context.getClass().getMethod("getAuthentication").invoke(context);

            if (authentication != null) {
                Object name = authentication.getClass().getMethod("getName").invoke(authentication);
                if (name != null) {
                    return name.toString();
                }
            }
        } catch (Exception e) {
            // Spring Security not available or not authenticated
        }

        return "ANONYMOUS";
    }
}
