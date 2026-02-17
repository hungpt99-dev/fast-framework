package com.fast.cqrs.concurrent.context;

import org.slf4j.MDC;

import java.util.concurrent.Callable;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures and restores execution context for async operations.
 * <p>
 * Optimized for virtual threads (Java 21+) while maintaining backwards 
 * compatibility with platform threads.
 * <p>
 * Supports:
 * <ul>
 * <li>SLF4J MDC (logging context)</li>
 * <li>Spring SecurityContext</li>
 * <li>Custom context values via {@link ContextRegistry}</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * ContextSnapshot snapshot = ContextSnapshot.capture();
 * 
 * executor.submit(snapshot.wrap(() -> {
 *     // MDC, SecurityContext, and custom context are now available
 *     doWork();
 * }));
 * }</pre>
 */
public class ContextSnapshot {

    private final Map<String, String> mdcContext;
    private final Object securityContext;
    private final Map<String, Object> customContext;

    private ContextSnapshot(Map<String, String> mdcContext, Object securityContext,
            Map<String, Object> customContext) {
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
        Map<String, Object> custom = ContextRegistry.captureAll();

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

        // Restore custom context
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
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            restore();
            try {
                runnable.run();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Returns a supplier that wraps the given supplier with context propagation.
     */
    public <T> java.util.function.Supplier<T> wrap(java.util.function.Supplier<T> supplier) {
        return () -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            restore();
            try {
                return supplier.get();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Returns a callable that wraps the given callable with context propagation.
     */
    public <T> Callable<T> wrap(Callable<T> callable) {
        return () -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            restore();
            try {
                return callable.call();
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Returns a consumer that wraps the given consumer with context propagation.
     */
    public <T> java.util.function.Consumer<T> wrap(java.util.function.Consumer<T> consumer) {
        return (t) -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            restore();
            try {
                consumer.accept(t);
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    /**
     * Returns a function that wraps the given function with context propagation.
     */
    public <T, R> java.util.function.Function<T, R> wrap(java.util.function.Function<T, R> function) {
        return (t) -> {
            Map<String, String> originalMdc = MDC.getCopyOfContextMap();
            restore();
            try {
                return function.apply(t);
            } finally {
                if (originalMdc != null) {
                    MDC.setContextMap(originalMdc);
                } else {
                    MDC.clear();
                }
            }
        };
    }

    // Accessor strategy to avoid per-request reflection
    private static final SecurityContextAccessor SECURITY_ACCESSOR;

    static {
        SecurityContextAccessor accessor;
        try {
            // Try to instantiate - triggers NoClassDefFoundError if missing
            accessor = new SpringSecurityContextAccessor();
        } catch (Throwable e) {
            // Fallback if Spring Security is not on classpath
            accessor = new NoOpSecurityContextAccessor();
        }
        SECURITY_ACCESSOR = accessor;
    }

    // Capture security context using the selected strategy
    private static Object captureSecurityContext() {
        return SECURITY_ACCESSOR.capture();
    }

    private static void restoreSecurityContext(Object context) {
        SECURITY_ACCESSOR.restore(context);
    }

    private static void clearSecurityContext() {
        SECURITY_ACCESSOR.clear();
    }

    // Strategy interface
    private interface SecurityContextAccessor {
        Object capture();
        void restore(Object context);
        void clear();
    }

    // No-op implementation
    private static class NoOpSecurityContextAccessor implements SecurityContextAccessor {
        @Override public Object capture() { return null; }
        @Override public void restore(Object context) {}
        @Override public void clear() {}
    }

    // Spring Security implementation (Classes loaded only if Spring Security is present)
    private static class SpringSecurityContextAccessor implements SecurityContextAccessor {
        @Override
        public Object capture() {
            return org.springframework.security.core.context.SecurityContextHolder.getContext();
        }

        @Override
        public void restore(Object context) {
            if (context instanceof org.springframework.security.core.context.SecurityContext) {
                org.springframework.security.core.context.SecurityContextHolder.setContext(
                    (org.springframework.security.core.context.SecurityContext) context);
            }
        }

        @Override
        public void clear() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
