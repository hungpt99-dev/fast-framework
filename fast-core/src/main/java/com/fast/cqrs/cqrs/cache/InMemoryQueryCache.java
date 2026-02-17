package com.fast.cqrs.cqrs.cache;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * In-memory implementation of {@link QueryCache}.
 * <p>
 * Suitable for single-instance applications. For distributed systems,
 * use a Redis or Caffeine-backed implementation.
 * <p>
 * Features:
 * <ul>
 * <li>Thread-safe with ConcurrentHashMap</li>
 * <li>Automatic expiration of old entries</li>
 * <li>Pattern-based eviction</li>
 * </ul>
 */
public class InMemoryQueryCache implements QueryCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // Cleanup runs every N operations
    private static final int CLEANUP_THRESHOLD = 500;
    private final AtomicInteger operationCount = new AtomicInteger(0);

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        maybeCleanup();
        
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        
        // Check expiration
        if (entry.isExpired()) {
            cache.remove(key);
            return Optional.empty();
        }
        
        return Optional.of((T) entry.value);
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        long expiresAt = System.currentTimeMillis() + ttl.toMillis();
        cache.put(key, new CacheEntry(value, expiresAt));
    }

    @Override
    public void evict(String key) {
        cache.remove(key);
    }

    @Override
    public void evictByPattern(String keyPattern) {
        // Convert simple wildcard pattern to regex
        String regex = keyPattern
                .replace(".", "\\.")
                .replace("*", ".*");
        Pattern pattern = Pattern.compile(regex);
        
        cache.keySet().removeIf(key -> pattern.matcher(key).matches());
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        return cache.size();
    }

    private void maybeCleanup() {
        if (operationCount.incrementAndGet() >= CLEANUP_THRESHOLD) {
            operationCount.set(0);
            long now = System.currentTimeMillis();
            cache.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        }
    }

    private record CacheEntry(Object value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
