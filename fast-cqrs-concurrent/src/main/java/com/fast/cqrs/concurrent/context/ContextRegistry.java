package com.fast.cqrs.concurrent.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for custom ThreadLocal values to propagate across threads.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * // Register a ThreadLocal for propagation
 * ThreadLocal<String> requestId = new ThreadLocal<>();
 * ContextRegistry.register("requestId", requestId);
 * 
 * // Set value and capture
 * requestId.set("req-123");
 * ContextSnapshot snapshot = ContextSnapshot.capture();
 * 
 * // Value will be available in async context
 * executor.submit(snapshot.wrap(() -> {
 *     System.out.println(requestId.get()); // "req-123"
 * }));
 * }</pre>
 */
public final class ContextRegistry {

    private static final Map<String, ThreadLocal<?>> registry = new ConcurrentHashMap<>();

    private ContextRegistry() {
    }

    /**
     * Registers a ThreadLocal for context propagation.
     */
    public static void register(String name, ThreadLocal<?> threadLocal) {
        registry.put(name, threadLocal);
    }

    /**
     * Unregisters a ThreadLocal.
     */
    public static void unregister(String name) {
        registry.remove(name);
    }

    /**
     * Captures all registered ThreadLocal values.
     */
    static Map<ThreadLocal<?>, Object> captureAll() {
        Map<ThreadLocal<?>, Object> snapshot = new HashMap<>();
        for (ThreadLocal<?> tl : registry.values()) {
            Object value = tl.get();
            if (value != null) {
                snapshot.put(tl, value);
            }
        }
        return snapshot;
    }

    /**
     * Restores captured ThreadLocal values.
     */
    @SuppressWarnings("unchecked")
    static void restoreAll(Map<ThreadLocal<?>, Object> snapshot) {
        if (snapshot == null)
            return;
        for (Map.Entry<ThreadLocal<?>, Object> entry : snapshot.entrySet()) {
            ((ThreadLocal<Object>) entry.getKey()).set(entry.getValue());
        }
    }

    /**
     * Clears all registered ThreadLocal values.
     */
    static void clearAll() {
        for (ThreadLocal<?> tl : registry.values()) {
            tl.remove();
        }
    }
}
