package com.fast.cqrs.cqrs.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache interface for query result caching.
 * <p>
 * Implementations store query results with TTL (time-to-live) for
 * automatic expiration.
 * <p>
 * Usage:
 * <pre>{@code
 * @Query(handler = GetOrderHandler.class, cache = "5m", cacheKey = "#query.id")
 * OrderDto getOrder(GetOrderQuery query);
 * }</pre>
 *
 * @see com.fast.cqrs.cqrs.annotation.Query#cache()
 * @see com.fast.cqrs.cqrs.annotation.Query#cacheKey()
 */
public interface QueryCache {

    /**
     * Gets a cached result.
     *
     * @param key the cache key
     * @param <T> the result type
     * @return the cached result if present and not expired, empty otherwise
     */
    <T> Optional<T> get(String key);

    /**
     * Stores a result in the cache.
     *
     * @param key the cache key
     * @param value the result to cache
     * @param ttl time-to-live
     */
    void put(String key, Object value, Duration ttl);

    /**
     * Removes a cached result.
     *
     * @param key the cache key
     */
    void evict(String key);

    /**
     * Removes all cached results matching a pattern.
     *
     * @param keyPattern the key pattern (supports * wildcard)
     */
    void evictByPattern(String keyPattern);

    /**
     * Clears all cached results.
     */
    void clear();

    /**
     * Returns the number of cached entries (for monitoring).
     */
    int size();
}
