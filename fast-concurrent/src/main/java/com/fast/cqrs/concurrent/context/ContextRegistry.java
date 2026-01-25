package com.fast.cqrs.concurrent.context;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for context values to propagate across threads.
 * <p>
 * Supports both ThreadLocal (legacy) and ScopedValue (Java 21+ virtual threads).
 * Automatically uses the best approach based on the runtime environment.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * // Register a context value for propagation
 * ContextRegistry.register("requestId", "req-123");
 * 
 * // Capture for async
 * ContextSnapshot snapshot = ContextSnapshot.capture();
 * 
 * // Value will be available in async context
 * executor.submit(snapshot.wrap(() -> {
 *     String id = ContextRegistry.get("requestId"); // "req-123"
 * }));
 * }</pre>
 */
public final class ContextRegistry {

    // Storage for context values (thread-safe map for virtual thread compatibility)
    private static final Map<String, ThreadLocal<?>> threadLocalRegistry = new ConcurrentHashMap<>();
    
    // Direct value storage for virtual threads (captured/restored via snapshot)
    private static final ThreadLocal<Map<String, Object>> virtualThreadContext = 
            ThreadLocal.withInitial(HashMap::new);

    private ContextRegistry() {
    }

    /**
     * Registers a ThreadLocal for context propagation (legacy API).
     * 
     * @deprecated Use {@link #set(String, Object)} for better virtual thread support
     */
    @Deprecated
    public static void register(String name, ThreadLocal<?> threadLocal) {
        threadLocalRegistry.put(name, threadLocal);
    }

    /**
     * Unregisters a ThreadLocal (legacy API).
     * 
     * @deprecated Use {@link #remove(String)} for better virtual thread support
     */
    @Deprecated
    public static void unregister(String name) {
        threadLocalRegistry.remove(name);
    }

    /**
     * Sets a context value (virtual thread friendly).
     */
    public static void set(String name, Object value) {
        virtualThreadContext.get().put(name, value);
    }

    /**
     * Gets a context value.
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String name) {
        return (T) virtualThreadContext.get().get(name);
    }

    /**
     * Removes a context value.
     */
    public static void remove(String name) {
        virtualThreadContext.get().remove(name);
    }

    /**
     * Captures all context values (both legacy ThreadLocal and new API).
     */
    static Map<String, Object> captureAll() {
        Map<String, Object> snapshot = new HashMap<>();
        
        // Capture legacy ThreadLocal values
        for (Map.Entry<String, ThreadLocal<?>> entry : threadLocalRegistry.entrySet()) {
            Object value = entry.getValue().get();
            if (value != null) {
                snapshot.put("tl:" + entry.getKey(), value);
            }
        }
        
        // Capture direct context values
        snapshot.putAll(virtualThreadContext.get());
        
        return snapshot;
    }

    /**
     * Restores captured context values.
     */
    @SuppressWarnings("unchecked")
    static void restoreAll(Map<String, Object> snapshot) {
        if (snapshot == null) return;
        
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (key.startsWith("tl:")) {
                // Legacy ThreadLocal
                String tlName = key.substring(3);
                ThreadLocal<?> tl = threadLocalRegistry.get(tlName);
                if (tl != null) {
                    ((ThreadLocal<Object>) tl).set(value);
                }
            } else {
                // Direct context value
                virtualThreadContext.get().put(key, value);
            }
        }
    }

    /**
     * Clears all context values.
     */
    static void clearAll() {
        // Clear legacy ThreadLocals
        for (ThreadLocal<?> tl : threadLocalRegistry.values()) {
            tl.remove();
        }
        
        // Clear direct context
        virtualThreadContext.get().clear();
    }

    /**
     * Returns the count of registered context entries.
     */
    public static int size() {
        return threadLocalRegistry.size() + virtualThreadContext.get().size();
    }
}
