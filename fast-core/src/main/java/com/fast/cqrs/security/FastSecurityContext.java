package com.fast.cqrs.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static accessor for the current {@link SecurityContext}.
 * <p>
 * This class provides a single access point for security context, automatically
 * detecting whether Spring Security is available and using the appropriate
 * implementation.
 * <p>
 * <b>GraalVM Compatible:</b> Uses classpath detection at initialization time,
 * not per-request reflection.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // Check if user is authenticated
 * if (FastSecurityContext.get().isAuthenticated()) {
 *     String user = FastSecurityContext.get().getUsername();
 * }
 *
 * // Check roles
 * if (FastSecurityContext.get().hasRole("ADMIN")) {
 *     // admin logic
 * }
 * }</pre>
 */
public final class FastSecurityContext {

    private static final Logger log = LoggerFactory.getLogger(FastSecurityContext.class);
    private static final SecurityContext INSTANCE;

    static {
        SecurityContext context;
        try {
            // Check if Spring Security is available
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            context = new SpringSecurityContext();
            log.debug("Spring Security detected, using SpringSecurityContext");
        } catch (ClassNotFoundException e) {
            context = new NoOpSecurityContext();
            log.debug("Spring Security not found, using NoOpSecurityContext");
        }
        INSTANCE = context;
    }

    private FastSecurityContext() {
        // Static utility class
    }

    /**
     * Gets the current security context.
     *
     * @return the security context (never null)
     */
    public static SecurityContext get() {
        return INSTANCE;
    }

    /**
     * Convenience method to check if user is authenticated.
     */
    public static boolean isAuthenticated() {
        return INSTANCE.isAuthenticated();
    }

    /**
     * Convenience method to get current username.
     */
    public static String getUsername() {
        return INSTANCE.getUsername();
    }

    /**
     * Convenience method to check role.
     */
    public static boolean hasRole(String role) {
        return INSTANCE.hasRole(role);
    }

    /**
     * Convenience method to check any role.
     */
    public static boolean hasAnyRole(String... roles) {
        return INSTANCE.hasAnyRole(roles);
    }

    /**
     * Convenience method to check authority.
     */
    public static boolean hasAuthority(String authority) {
        return INSTANCE.hasAuthority(authority);
    }

    /**
     * Convenience method to check any authority.
     */
    public static boolean hasAnyAuthority(String... authorities) {
        return INSTANCE.hasAnyAuthority(authorities);
    }

    /**
     * Throws SecurityException if user is not authenticated.
     *
     * @throws SecurityException if not authenticated
     */
    public static void requireAuthenticated() {
        if (!INSTANCE.isAuthenticated()) {
            throw new SecurityException("Authentication required");
        }
    }

    /**
     * Throws SecurityException if user does not have the role.
     *
     * @param role required role
     * @throws SecurityException if role not present
     */
    public static void requireRole(String role) {
        if (!INSTANCE.hasRole(role)) {
            throw new SecurityException("Access denied: requires role " + role);
        }
    }

    /**
     * Throws SecurityException if user does not have the authority.
     *
     * @param authority required authority
     * @throws SecurityException if authority not present
     */
    public static void requireAuthority(String authority) {
        if (!INSTANCE.hasAuthority(authority)) {
            throw new SecurityException("Access denied: requires authority " + authority);
        }
    }

    /**
     * Security exception for access denied scenarios.
     */
    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }
}
