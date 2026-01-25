package com.fast.cqrs.cqrs.idempotency;

import java.time.Duration;
import java.util.Optional;

/**
 * Storage interface for command idempotency tracking.
 * <p>
 * Implementations store idempotency keys and their results to ensure
 * commands with the same key are executed only once.
 * <p>
 * Usage:
 * <pre>{@code
 * @Command(handler = CreatePaymentHandler.class, idempotencyKey = "#cmd.requestId")
 * void createPayment(CreatePaymentCommand cmd);
 * }</pre>
 *
 * @see com.fast.cqrs.cqrs.annotation.Command#idempotencyKey()
 */
public interface IdempotencyStore {

    /**
     * Checks if a command with the given key has already been processed.
     *
     * @param key the idempotency key
     * @return the stored result if already processed, empty otherwise
     */
    Optional<IdempotencyRecord> get(String key);

    /**
     * Stores the result of a command execution.
     *
     * @param key the idempotency key
     * @param result the command result (may be null for void commands)
     * @param ttl time-to-live for the record
     */
    void store(String key, Object result, Duration ttl);

    /**
     * Marks a key as "in progress" to prevent concurrent execution.
     *
     * @param key the idempotency key
     * @param ttl lock timeout
     * @return true if lock acquired, false if already locked or completed
     */
    boolean tryLock(String key, Duration ttl);

    /**
     * Releases a lock (called on failure to allow retry).
     *
     * @param key the idempotency key
     */
    void unlock(String key);

    /**
     * Record of a completed idempotent command.
     */
    record IdempotencyRecord(
            String key,
            Object result,
            long timestamp,
            boolean completed
    ) {}
}
