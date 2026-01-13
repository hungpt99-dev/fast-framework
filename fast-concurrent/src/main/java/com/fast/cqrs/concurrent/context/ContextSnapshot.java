package com.fast.cqrs.concurrent.context;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Captures and restores execution context for async operations.
 * <p>
 * Supports:
 * <ul>
 * <li>SLF4J MDC (logging context)</li>
 * <li>Spring SecurityContext</li>
 * <li>Custom ThreadLocal values</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * ContextSnapshot snapshot = ContextSnapshot.capture();
 * 
 * executor.submit(() -> {
 *     snapshot.restore();
 *     try {
 *         // MDC and SecurityContext are now available
 *         doWork();
 *     } finally {
 *         snapshot.clear();
 *     }
 * });
 * }</pre>
 */
public class ContextSnapshot {

    private final Map<String, String> mdcContext;
    private final Object securityContext;
    private final Map<ThreadLocal<?>, Object> customContext;

    private ContextSnapshot(Map<String, String> mdcContext, Object securityContext,
            Map<ThreadLocal<?>, Object> customContext) {
        this.mdcContext = mdcContext;
        this.securityContext = securityContext;
        this.customContext = customContext;
    }

    /**
     * Captures the current thread's context.
     */
    public static ContextSnapshot capture() {
        Map<String, String> mdc = MDC.getCopyOfContextMap();
        Object security = captureSecurityContext();
        Map<ThreadLocal<?>, Object> custom = ContextRegistry.captureAll();

        return new ContextSnapshot(
                mdc != null ? new HashMap<>(mdc) : new HashMap<>(),
                security,
                custom);
    }

    /**
     * Restores the captured context to the current thread.
     */
    public void restore() {
        // Restore MDC
        if (mdcContext != null && !mdcContext.isEmpty()) {
            MDC.setContextMap(mdcContext);
        }

        // Restore SecurityContext
        restoreSecurityContext(securityContext);

        // Restore custom ThreadLocals
        ContextRegistry.restoreAll(customContext);
    }

    /**
     * Clears the context from the current thread.
     */
    public void clear() {
        MDC.clear();
        clearSecurityContext();
        ContextRegistry.clearAll();
    }

    /**
     * Returns a runnable that wraps the given runnable with context propagation.
     */
    public Runnable wrap(Runnable runnable) {
        return () -> {
            restore();
            try {
                runnable.run();
            } finally {
                clear();
            }
        };
    }

    /**
     * Returns a supplier that wraps the given supplier with context propagation.
     */
    public <T> java.util.function.Supplier<T> wrap(java.util.function.Supplier<T> supplier) {
        return () -> {
            restore();
            try {
                return supplier.get();
            } finally {
                clear();
            }
        };
    }

    // Spring Security integration (optional dependency)
    private static Object captureSecurityContext() {
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            return holder.getMethod("getContext").invoke(null);
        } catch (Exception e) {
            return null; // Spring Security not available
        }
    }

    private static void restoreSecurityContext(Object context) {
        if (context == null)
            return;
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            holder.getMethod("setContext", Class.forName("org.springframework.security.core.context.SecurityContext"))
                    .invoke(null, context);
        } catch (Exception ignored) {
            // Spring Security not available
        }
    }

    private static void clearSecurityContext() {
        try {
            Class<?> holder = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            holder.getMethod("clearContext").invoke(null);
        } catch (Exception ignored) {
            // Spring Security not available
        }
    }
}
