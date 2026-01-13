package com.fast.cqrs.concurrent.resilience;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for circuit breakers.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreakerRegistry.get("payment-service");
 * 
 * // Or with config
 * CircuitBreakerRegistry.register(
 *         CircuitBreaker.of("payment-service")
 *                 .failureThreshold(3)
 *                 .resetTimeout(60)
 *                 .build());
 * }</pre>
 */
public final class CircuitBreakerRegistry {

    private static final Map<String, CircuitBreaker> registry = new ConcurrentHashMap<>();

    private CircuitBreakerRegistry() {
    }

    /**
     * Gets or creates a circuit breaker.
     */
    public static CircuitBreaker get(String name) {
        return registry.computeIfAbsent(name, n -> CircuitBreaker.of(n).build());
    }

    /**
     * Registers a custom circuit breaker.
     */
    public static void register(CircuitBreaker breaker) {
        registry.put(breaker.getName(), breaker);
    }

    /**
     * Removes a circuit breaker.
     */
    public static void remove(String name) {
        registry.remove(name);
    }

    /**
     * Resets all circuit breakers.
     */
    public static void resetAll() {
        registry.values().forEach(CircuitBreaker::reset);
    }

    /**
     * Gets all registered circuit breakers.
     */
    public static Map<String, CircuitBreaker> getAll() {
        return Map.copyOf(registry);
    }
}
