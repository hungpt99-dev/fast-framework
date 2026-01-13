package com.fast.cqrs.logging.audit;

/**
 * Interface for logging audit events.
 * <p>
 * Implementations can write audit events to various destinations:
 * - Log files (SLF4J)
 * - Database tables
 * - Message queues
 * - External audit services
 */
public interface AuditLogger {

    /**
     * Logs an audit event.
     *
     * @param event the audit event to log
     */
    void log(AuditEvent event);

    /**
     * Logs an audit event for an action that is about to be performed.
     * <p>
     * This is called before the action executes.
     *
     * @param action the action name
     * @param actor  the user performing the action
     * @param target the object being acted upon
     */
    default void logBefore(String action, String actor, Object target) {
        log(AuditEvent.builder()
                .action(action + "_STARTED")
                .actor(actor)
                .resource(target.getClass().getSimpleName())
                .build());
    }

    /**
     * Logs a successful action.
     *
     * @param action the action name
     * @param actor  the user who performed the action
     * @param target the object that was acted upon
     */
    default void logSuccess(String action, String actor, Object target) {
        log(AuditEvent.builder()
                .action(action)
                .actor(actor)
                .resource(target.getClass().getSimpleName())
                .success(true)
                .build());
    }

    /**
     * Logs a failed action.
     *
     * @param action the action name
     * @param actor  the user who attempted the action
     * @param target the object that was acted upon
     * @param error  the error that occurred
     */
    default void logFailure(String action, String actor, Object target, Throwable error) {
        log(AuditEvent.builder()
                .action(action + "_FAILED")
                .actor(actor)
                .resource(target.getClass().getSimpleName())
                .success(false)
                .build());
    }
}
