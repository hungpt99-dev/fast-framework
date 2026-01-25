package com.fast.cqrs.cqrs.idempotency;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link IdempotencyStore}.
 * <p>
 * Suitable for single-instance applications. For distributed systems,
 * use a Redis or database-backed implementation.
 * <p>
 * Features:
 * <ul>
 * <li>Thread-safe with ConcurrentHashMap</li>
 * <li>Automatic expiration of old records</li>
 * <li>Lock support for concurrent requests</li>
 * </ul>
 */
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    // Cleanup runs every N operations
    private static final int CLEANUP_THRESHOLD = 1000;
    private int operationCount = 0;

    @Override
    public Optional<IdempotencyRecord> get(String key) {
        maybeCleanup();
        
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        
        // Check expiration
        if (entry.isExpired()) {
            store.remove(key);
            return Optional.empty();
        }
        
        return Optional.of(new IdempotencyRecord(
                key,
                entry.result,
                entry.timestamp,
                entry.completed
        ));
    }

    @Override
    public void store(String key, Object result, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        store.put(key, new Entry(result, System.currentTimeMillis(), expiresAt, true));
    }

    @Override
    public boolean tryLock(String key, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        
        return store.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                // No entry or expired - acquire lock
                return new Entry(null, System.currentTimeMillis(), expiresAt, false);
            }
            // Already exists (locked or completed)
            return existing;
        }).result == null && !store.get(key).completed;
    }

    @Override
    public void unlock(String key) {
        store.remove(key);
    }

    private void maybeCleanup() {
        if (++operationCount >= CLEANUP_THRESHOLD) {
            operationCount = 0;
            long now = System.currentTimeMillis();
            store.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        }
    }

    /**
     * Returns the current size of the store (for monitoring).
     */
    public int size() {
        return store.size();
    }

    /**
     * Clears all entries (for testing).
     */
    public void clear() {
        store.clear();
    }

    private record Entry(
            Object result,
            long timestamp,
            long expiresAt,
            boolean completed
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
