package com.fast.cqrs.concurrent.resilience;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Manages concurrency limits per key (e.g. orderId).
 * Uses Caffeine cache to expire unused locks and prevent memory leaks.
 */
public class KeyedSemaphoreRegistry {

    private static final Logger log = LoggerFactory.getLogger(KeyedSemaphoreRegistry.class);

    // Cache of Key -> Semaphore
    // Expire after access to prevent memory leaks for infinite keys
    private final Cache<String, Semaphore> semaphoreCache;
    private final MeterRegistry meterRegistry;

    public KeyedSemaphoreRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.semaphoreCache = Caffeine.newBuilder()
                .expireAfterAccess(1, TimeUnit.HOURS) // Configurable?
                .maximumSize(100_000)
                .build();
    }

    public boolean acquire(String contextKey, String lockKey, int permits, long timeoutMs) {
        String fullKey = contextKey + ":" + lockKey;
        Semaphore sem = getSemaphore(fullKey, permits);
        try {
            boolean acquired = sem.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired && meterRegistry != null) {
                meterRegistry.counter("fast.concurrent.keyed.acquired", "context", contextKey).increment();
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release(String contextKey, String lockKey) {
        String fullKey = contextKey + ":" + lockKey;
        Semaphore sem = semaphoreCache.getIfPresent(fullKey);
        if (sem != null) {
            sem.release();
        }
    }

    private Semaphore getSemaphore(String fullKey, int permits) {
        return semaphoreCache.get(fullKey, k -> new Semaphore(permits, true));
    }
}
