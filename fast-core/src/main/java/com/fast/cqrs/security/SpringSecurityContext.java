package com.fast.cqrs.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Spring Security implementation of {@link SecurityContext}.
 * <p>
 * This class uses Spring Security types directly, providing type-safe access
 * to security context without reflection. It is only loaded when Spring Security
 * is present on the classpath.
 * <p>
 * <b>GraalVM Compatible:</b> No reflection, no dynamic proxies.
 */
public class SpringSecurityContext implements SecurityContext {

    @Override
    public boolean isAuthenticated() {
        Authentication auth = getAuthentication();
        return auth != null && auth.isAuthenticated();
    }

    @Override
    public String getUsername() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "anonymous";
    }

    @Override
    public Set<String> getAuthorities() {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return Collections.emptySet();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean hasRole(String role) {
        return hasAuthority("ROLE_" + role);
    }

    @Override
    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAuthority(String authority) {
        Authentication auth = getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if (authority.equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAnyAuthority(String... authorities) {
        for (String authority : authorities) {
            if (hasAuthority(authority)) {
                return true;
            }
        }
        return false;
    }

    private Authentication getAuthentication() {
        org.springframework.security.core.context.SecurityContext context = 
            org.springframework.security.core.context.SecurityContextHolder.getContext();
        return context != null ? context.getAuthentication() : null;
    }
}
