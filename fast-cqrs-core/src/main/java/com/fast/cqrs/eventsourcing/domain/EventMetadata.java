package com.fast.cqrs.eventsourcing.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Rich event metadata for event sourcing.
 * <p>
 * Contains all contextual information about an event:
 * <ul>
 * <li>Unique event ID</li>
 * <li>Aggregate context (ID, type, version)</li>
 * <li>Correlation and causation IDs</li>
 * <li>Tenant and user context</li>
 * <li>Distributed tracing</li>
 * <li>Custom metadata</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * EventMetadata metadata = EventMetadata.builder()
 *         .aggregateId("order-123")
 *         .aggregateType("Order")
 *         .correlationId(requestId)
 *         .userId(currentUser)
 *         .build();
 * }</pre>
 */
public record EventMetadata(
        String eventId,
        Instant timestamp,
        String aggregateId,
        String aggregateType,
        long aggregateVersion,
        String eventType,
        int schemaVersion,
        String correlationId,
        String causationId,
        String tenantId,
        String userId,
        String traceId,
        String spanId,
        Map<String, String> custom) {

    public EventMetadata {
        if (eventId == null)
            eventId = UUID.randomUUID().toString();
        if (timestamp == null)
            timestamp = Instant.now();
        if (custom == null)
            custom = Map.of();
        if (schemaVersion <= 0)
            schemaVersion = 1;
    }

    /**
     * Creates a builder for EventMetadata.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates metadata for a new event.
     */
    public static EventMetadata forAggregate(String aggregateId, String aggregateType, long version) {
        return builder()
                .aggregateId(aggregateId)
                .aggregateType(aggregateType)
                .aggregateVersion(version)
                .build();
    }

    /**
     * Builder for EventMetadata.
     */
    public static class Builder {
        private String eventId = UUID.randomUUID().toString();
        private Instant timestamp = Instant.now();
        private String aggregateId;
        private String aggregateType;
        private long aggregateVersion;
        private String eventType;
        private int schemaVersion = 1;
        private String correlationId;
        private String causationId;
        private String tenantId;
        private String userId;
        private String traceId;
        private String spanId;
        private final Map<String, String> custom = new HashMap<>();

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder aggregateId(String aggregateId) {
            this.aggregateId = aggregateId;
            return this;
        }

        public Builder aggregateType(String aggregateType) {
            this.aggregateType = aggregateType;
            return this;
        }

        public Builder aggregateVersion(long aggregateVersion) {
            this.aggregateVersion = aggregateVersion;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder schemaVersion(int schemaVersion) {
            this.schemaVersion = schemaVersion;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder causationId(String causationId) {
            this.causationId = causationId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder spanId(String spanId) {
            this.spanId = spanId;
            return this;
        }

        public Builder custom(String key, String value) {
            this.custom.put(key, value);
            return this;
        }

        public Builder custom(Map<String, String> custom) {
            this.custom.putAll(custom);
            return this;
        }

        public EventMetadata build() {
            return new EventMetadata(
                    eventId, timestamp, aggregateId, aggregateType,
                    aggregateVersion, eventType, schemaVersion,
                    correlationId, causationId, tenantId, userId,
                    traceId, spanId, Map.copyOf(custom));
        }
    }
}
