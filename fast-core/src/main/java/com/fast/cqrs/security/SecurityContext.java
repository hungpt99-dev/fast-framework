package com.fast.cqrs.security;

import java.util.Set;

/**
 * Interface for accessing security context information safely.
 * <p>
 * This abstraction layer allows the framework to access security information
 * without unsafe reflection, making it GraalVM native-image compatible.
 * <p>
 * Implementations:
 * <ul>
 *   <li>{@link SpringSecurityContext} - Uses Spring Security (when available)</li>
 *   <li>{@link NoOpSecurityContext} - Fallback when no security provider</li>
 * </ul>
 *
 * @see FastSecurityContext
 */
public interface SecurityContext {

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated, false otherwise
     */
    boolean isAuthenticated();

    /**
     * Gets the current username or principal name.
     *
     * @return username or "anonymous" if not authenticated
     */
    String getUsername();

    /**
     * Gets all authorities/roles of the current user.
     *
     * @return set of authority strings, empty if not authenticated
     */
    Set<String> getAuthorities();

    /**
     * Checks if the user has the specified role.
     * <p>
     * This method automatically prepends "ROLE_" prefix.
     *
     * @param role role name without ROLE_ prefix (e.g., "ADMIN")
     * @return true if user has the role
     */
    boolean hasRole(String role);

    /**
     * Checks if the user has any of the specified roles.
     *
     * @param roles role names without ROLE_ prefix
     * @return true if user has any of the roles
     */
    boolean hasAnyRole(String... roles);

    /**
     * Checks if the user has the specified authority.
     *
     * @param authority full authority string (e.g., "ROLE_ADMIN" or "READ_PRIVILEGE")
     * @return true if user has the authority
     */
    boolean hasAuthority(String authority);

    /**
     * Checks if the user has any of the specified authorities.
     *
     * @param authorities authority strings
     * @return true if user has any of the authorities
     */
    boolean hasAnyAuthority(String... authorities);
}
