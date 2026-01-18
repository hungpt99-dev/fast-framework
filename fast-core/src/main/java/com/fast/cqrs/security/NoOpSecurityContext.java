package com.fast.cqrs.security;

import java.util.Collections;
import java.util.Set;

/**
 * No-op implementation of {@link SecurityContext}.
 * <p>
 * Used when Spring Security is not available on the classpath.
 * All security checks return false/anonymous.
 */
public class NoOpSecurityContext implements SecurityContext {

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public String getUsername() {
        return "anonymous";
    }

    @Override
    public Set<String> getAuthorities() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasRole(String role) {
        return false;
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        return false;
    }

    @Override
    public boolean hasAuthority(String authority) {
        return false;
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        return false;
    }
}
