package com.fast.cqrs.concurrent.resilience;

import com.fast.cqrs.concurrent.spring.FastConcurrentProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages concurrency limits (Semaphores) for methods/classes.
 */
public class ConcurrencyRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyRegistry.class);

    private final Map<String, Semaphore> semaphores = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> activeCounts = new ConcurrentHashMap<>();
    private final FastConcurrentProperties properties;
    private final Object meterRegistry; // Opaque object to avoid hard dependency

    public ConcurrencyRegistry(FastConcurrentProperties properties, Object meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    public boolean acquire(String key, int permits, long timeoutMs) {
        Semaphore sem = getSemaphore(key, permits);
        try {
            boolean acquired = sem.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (acquired) {
                incrementActive(key);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void release(String key) {
        Semaphore sem = semaphores.get(key);
        if (sem != null) {
            sem.release();
            decrementActive(key);
        }
    }

    private Semaphore getSemaphore(String key, int configuredPermits) {
        return semaphores.computeIfAbsent(key, k -> {
            int permits = configuredPermits > 0 ? configuredPermits : properties.getPermits();
            Semaphore semaphore = new Semaphore(permits, true); // Fair semaphore
            if (this.meterRegistry != null) {
                MetricsHelper.registerMetrics(this.meterRegistry, k, semaphore, activeCounts);
            }
            return semaphore;
        });
    }

    private void incrementActive(String key) {
        AtomicInteger active = activeCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        active.incrementAndGet();
        if (this.meterRegistry != null) {
            MetricsHelper.incrementAcquired(this.meterRegistry, key);
        }
    }

    private void decrementActive(String key) {
        AtomicInteger active = activeCounts.get(key);
        if (active != null) {
            active.decrementAndGet();
        }
    }
    
    /**
     * Inner class to isolate Micrometer dependencies.
     * Loaded only if MeterRegistry is available and passed.
     */
    private static class MetricsHelper {
        static void registerMetrics(Object registryObj, String key, Semaphore semaphore, Map<String, AtomicInteger> activeCounts) {
            if (!(registryObj instanceof io.micrometer.core.instrument.MeterRegistry)) {
                return;
            }
            io.micrometer.core.instrument.MeterRegistry registry = (io.micrometer.core.instrument.MeterRegistry) registryObj;
            io.micrometer.core.instrument.Tags tags = io.micrometer.core.instrument.Tags.of("key", key);
            
            registry.gauge("fast.concurrent.available", tags, semaphore, Semaphore::availablePermits);
            registry.gauge("fast.concurrent.active", tags, activeCounts.computeIfAbsent(key, k -> new AtomicInteger(0)), AtomicInteger::get);
            registry.gauge("fast.concurrent.queue", tags, semaphore, s -> s.getQueueLength());
        }

        static void incrementAcquired(Object registryObj, String key) {
            if (!(registryObj instanceof io.micrometer.core.instrument.MeterRegistry)) {
                return;
            }
            io.micrometer.core.instrument.MeterRegistry registry = (io.micrometer.core.instrument.MeterRegistry) registryObj;
            registry.counter("fast.concurrent.acquired", "key", key).increment();
        }
    }
}
