package com.fast.cqrs.logging.audit;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an audit event for tracking important operations.
 * <p>
 * Audit events capture who did what, when, and to what resource.
 * They are essential for compliance, debugging, and security monitoring.
 *
 * @param action     the action performed (e.g., "CREATE_ORDER", "UPDATE_USER")
 * @param actor      the user or system that performed the action
 * @param resource   the type of resource affected (e.g., "Order", "User")
 * @param resourceId the identifier of the specific resource
 * @param details    additional details about the action (can be null)
 * @param timestamp  when the action occurred
 * @param success    whether the action was successful
 * @param traceId    the trace ID for correlation with logs
 */
public record AuditEvent(
        String action,
        String actor,
        String resource,
        String resourceId,
        Map<String, Object> details,
        Instant timestamp,
        boolean success,
        String traceId
) {

    /**
     * Creates a builder for constructing AuditEvent instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AuditEvent instances.
     */
    public static class Builder {
        private String action;
        private String actor;
        private String resource;
        private String resourceId;
        private Map<String, Object> details;
        private Instant timestamp = Instant.now();
        private boolean success = true;
        private String traceId;

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder actor(String actor) {
            this.actor = actor;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder resourceId(String resourceId) {
            this.resourceId = resourceId;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public AuditEvent build() {
            return new AuditEvent(
                    action, actor, resource, resourceId,
                    details, timestamp, success, traceId
            );
        }
    }
}
