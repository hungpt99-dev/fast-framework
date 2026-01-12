package com.fast.cqrs.eventsourcing.outbox;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entry in the transactional outbox.
 * <p>
 * The outbox pattern ensures at-least-once delivery by storing
 * events in the same transaction as business data.
 */
public record OutboxEntry(
        String id,
        String aggregateId,
        String eventType,
        String payload,
        Map<String, String> headers,
        Instant createdAt,
        OutboxStatus status,
        int attempts,
        Instant lastAttemptAt,
        String errorMessage) {

    public OutboxEntry {
        if (id == null)
            id = UUID.randomUUID().toString();
        if (createdAt == null)
            createdAt = Instant.now();
        if (status == null)
            status = OutboxStatus.PENDING;
        if (headers == null)
            headers = Map.of();
    }

    /**
     * Creates a new outbox entry.
     */
    public static OutboxEntry create(String aggregateId, String eventType, String payload) {
        return new OutboxEntry(
                UUID.randomUUID().toString(),
                aggregateId,
                eventType,
                payload,
                Map.of(),
                Instant.now(),
                OutboxStatus.PENDING,
                0,
                null,
                null);
    }

    /**
     * Creates entry with headers.
     */
    public static OutboxEntry create(String aggregateId, String eventType, String payload,
            Map<String, String> headers) {
        return new OutboxEntry(
                UUID.randomUUID().toString(),
                aggregateId,
                eventType,
                payload,
                headers,
                Instant.now(),
                OutboxStatus.PENDING,
                0,
                null,
                null);
    }

    /**
     * Marks as sent.
     */
    public OutboxEntry markSent() {
        return new OutboxEntry(id, aggregateId, eventType, payload, headers,
                createdAt, OutboxStatus.SENT, attempts + 1, Instant.now(), null);
    }

    /**
     * Marks as failed.
     */
    public OutboxEntry markFailed(String error) {
        return new OutboxEntry(id, aggregateId, eventType, payload, headers,
                createdAt, OutboxStatus.FAILED, attempts + 1, Instant.now(), error);
    }
}
