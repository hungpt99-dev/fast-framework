package com.fast.cqrs.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor for processing Spring Security's {@code @PreAuthorize} annotations
 * on interface controller methods.
 * <p>
 * This interceptor is activated only when Spring Security is present on the classpath.
 * It supports common expressions like:
 * <ul>
 *   <li>{@code hasRole('ADMIN')}</li>
 *   <li>{@code hasAnyRole('ADMIN', 'USER')}</li>
 *   <li>{@code hasAuthority('READ_PRIVILEGE')}</li>
 *   <li>{@code hasAnyAuthority('READ_PRIVILEGE', 'WRITE_PRIVILEGE')}</li>
 *   <li>{@code isAuthenticated()}</li>
 *   <li>{@code permitAll()}</li>
 *   <li>{@code denyAll()}</li>
 * </ul>
 */
public class SecurityInvocationInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SecurityInvocationInterceptor.class);

    private static final Pattern HAS_ROLE_PATTERN = Pattern.compile("hasRole\\(['\"](.+?)['\"]\\)");
    private static final Pattern HAS_ANY_ROLE_PATTERN = Pattern.compile("hasAnyRole\\((.+?)\\)");
    private static final Pattern HAS_AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\(['\"](.+?)['\"]\\)");
    private static final Pattern HAS_ANY_AUTHORITY_PATTERN = Pattern.compile("hasAnyAuthority\\((.+?)\\)");

    private static final boolean SECURITY_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            available = true;
        } catch (ClassNotFoundException e) {
            // Spring Security not available
        }
        SECURITY_AVAILABLE = available;
    }

    /**
     * Checks if Spring Security is available on the classpath.
     *
     * @return true if Spring Security is available
     */
    public static boolean isSecurityAvailable() {
        return SECURITY_AVAILABLE;
    }

    /**
     * Checks security constraints for the given method.
     * <p>
     * If the method is annotated with {@code @PreAuthorize}, the expression will
     * be evaluated against the current security context. If access is denied,
     * a RuntimeException is thrown.
     *
     * @param method the method to check
     * @throws RuntimeException if access is denied
     */
    public static void checkSecurity(Method method) {
        if (!SECURITY_AVAILABLE) {
            return;
        }

        // Look for @PreAuthorize annotation
        String expression = findPreAuthorizeExpression(method);
        if (expression == null) {
            return;
        }

        log.debug("Evaluating @PreAuthorize expression: {} for method: {}", expression, method.getName());

        if (!evaluateExpression(expression)) {
            log.warn("Access denied for method {} with expression: {}", method.getName(), expression);
            throw new SecurityException("Access is denied");
        }

        log.debug("Access granted for method: {}", method.getName());
    }

    private static String findPreAuthorizeExpression(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().getName().equals(
                    "org.springframework.security.access.prepost.PreAuthorize")) {
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    return (String) valueMethod.invoke(annotation);
                } catch (Exception e) {
                    log.error("Failed to read @PreAuthorize value", e);
                }
            }
        }
        return null;
    }

    private static boolean evaluateExpression(String expression) {
        // Handle permitAll()
        if (expression.contains("permitAll()")) {
            return true;
        }

        // Handle denyAll()
        if (expression.contains("denyAll()")) {
            return false;
        }

        Object authentication = getAuthentication();

        // Handle isAuthenticated()
        if (expression.contains("isAuthenticated()")) {
            return authentication != null && isAuthenticated(authentication);
        }

        // Handle isAnonymous()
        if (expression.contains("isAnonymous()")) {
            return authentication == null || !isAuthenticated(authentication);
        }

        if (authentication == null || !isAuthenticated(authentication)) {
            return false;
        }

        Collection<?> authorities = getAuthorities(authentication);
        if (authorities == null) {
            return false;
        }

        // Handle hasRole('ROLE')
        Matcher hasRoleMatcher = HAS_ROLE_PATTERN.matcher(expression);
        if (hasRoleMatcher.find()) {
            String role = hasRoleMatcher.group(1);
            return hasAuthority(authorities, "ROLE_" + role);
        }

        // Handle hasAnyRole('ROLE1', 'ROLE2')
        Matcher hasAnyRoleMatcher = HAS_ANY_ROLE_PATTERN.matcher(expression);
        if (hasAnyRoleMatcher.find()) {
            String rolesStr = hasAnyRoleMatcher.group(1);
            String[] roles = parseRoles(rolesStr);
            for (String role : roles) {
                if (hasAuthority(authorities, "ROLE_" + role)) {
                    return true;
                }
            }
            return false;
        }

        // Handle hasAuthority('AUTHORITY')
        Matcher hasAuthorityMatcher = HAS_AUTHORITY_PATTERN.matcher(expression);
        if (hasAuthorityMatcher.find()) {
            String authority = hasAuthorityMatcher.group(1);
            return hasAuthority(authorities, authority);
        }

        // Handle hasAnyAuthority('AUTH1', 'AUTH2')
        Matcher hasAnyAuthorityMatcher = HAS_ANY_AUTHORITY_PATTERN.matcher(expression);
        if (hasAnyAuthorityMatcher.find()) {
            String authoritiesStr = hasAnyAuthorityMatcher.group(1);
            String[] auths = parseRoles(authoritiesStr);
            for (String auth : auths) {
                if (hasAuthority(authorities, auth)) {
                    return true;
                }
            }
            return false;
        }

        // Unknown expression - deny by default for security
        log.warn("Unknown @PreAuthorize expression: {}. Denying access.", expression);
        return false;
    }

    private static Object getAuthentication() {
        try {
            Class<?> securityContextHolder = Class.forName(
                    "org.springframework.security.core.context.SecurityContextHolder");
            Object context = securityContextHolder.getMethod("getContext").invoke(null);
            return context.getClass().getMethod("getAuthentication").invoke(context);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isAuthenticated(Object authentication) {
        try {
            return (Boolean) authentication.getClass().getMethod("isAuthenticated").invoke(authentication);
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<?> getAuthorities(Object authentication) {
        try {
            return (Collection<?>) authentication.getClass().getMethod("getAuthorities").invoke(authentication);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean hasAuthority(Collection<?> authorities, String authority) {
        for (Object auth : authorities) {
            try {
                String authString = (String) auth.getClass().getMethod("getAuthority").invoke(auth);
                if (authority.equals(authString)) {
                    return true;
                }
            } catch (Exception e) {
                // Skip this authority
            }
        }
        return false;
    }

    private static String[] parseRoles(String rolesStr) {
        return rolesStr.replaceAll("['\"]", "")
                .split("\\s*,\\s*");
    }

    /**
     * Security exception thrown when access is denied.
     */
    public static class SecurityException extends RuntimeException {
        public SecurityException(String message) {
            super(message);
        }
    }
}
