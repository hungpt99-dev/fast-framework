package com.fast.cqrs.web;

import com.fast.cqrs.security.FastSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interceptor for processing Spring Security's {@code @PreAuthorize} annotations.
 * <p>
 * Supports common security expressions including:
 * <ul>
 *   <li>{@code hasRole('ADMIN')}</li>
 *   <li>{@code hasAnyRole('ADMIN', 'USER')}</li>
 *   <li>{@code hasAuthority('READ_PRIVILEGE')}</li>
 *   <li>{@code hasAnyAuthority('READ_PRIVILEGE', 'WRITE_PRIVILEGE')}</li>
 *   <li>{@code isAuthenticated()}</li>
 *   <li>{@code permitAll()}</li>
 *   <li>{@code denyAll()}</li>
 *   <li>{@code expr1 and expr2}</li>
 *   <li>{@code expr1 or expr2}</li>
 *   <li>{@code (expr1 or expr2) and expr3}</li>
 * </ul>
 * <p>
 * <b>Note:</b> Parameter references like {@code #user.id} are NOT supported at runtime.
 * These should be validated at compile-time by the processor.
 * <p>
 * <b>GraalVM Compatible:</b> Uses {@link FastSecurityContext} for type-safe security access.
 */
public class SecurityInvocationInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SecurityInvocationInterceptor.class);

    private static final Pattern HAS_ROLE_PATTERN = Pattern.compile("hasRole\\(['\"](.+?)['\"]\\)");
    private static final Pattern HAS_ANY_ROLE_PATTERN = Pattern.compile("hasAnyRole\\((.+?)\\)");
    private static final Pattern HAS_AUTHORITY_PATTERN = Pattern.compile("hasAuthority\\(['\"](.+?)['\"]\\)");
    private static final Pattern HAS_ANY_AUTHORITY_PATTERN = Pattern.compile("hasAnyAuthority\\((.+?)\\)");
    private static final Pattern PARAM_REFERENCE_PATTERN = Pattern.compile("#\\w+");

    private static final boolean SECURITY_AVAILABLE;

    static {
        boolean available = false;
        try {
            // Safe linkage check
            org.springframework.security.core.context.SecurityContextHolder.getContext();
            available = true;
        } catch (Throwable e) {
            // Spring Security not available
        }
        SECURITY_AVAILABLE = available;
    }

    /**
     * Checks if Spring Security is available on the classpath.
     */
    public static boolean isSecurityAvailable() {
        return SECURITY_AVAILABLE;
    }

    /**
     * Checks security constraints for the given method.
     *
     * @param method the method to check
     * @throws FastSecurityContext.SecurityException if access is denied
     */
    public static void checkSecurity(Method method) {
        if (!SECURITY_AVAILABLE) {
            return;
        }
        SecurityHelper.checkSecurity(method);
    }

    /**
     * Helper class to isolate Spring Security annotation dependencies.
     * Loaded only when Spring Security is available.
     */
    private static class SecurityHelper {
        static void checkSecurity(Method method) {
            String expression = findPreAuthorizeExpression(method);
            if (expression == null) {
                return;
            }

            log.debug("Evaluating @PreAuthorize: {} for method: {}", expression, method.getName());

            if (!evaluateExpression(expression)) {
                log.warn("Access denied for method {} with expression: {}", method.getName(), expression);
                throw new FastSecurityContext.SecurityException("Access is denied");
            }
        }

        private static String findPreAuthorizeExpression(Method method) {
            org.springframework.security.access.prepost.PreAuthorize annotation = 
                method.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);
            
            if (annotation != null) {
                return annotation.value();
            }
            return null;
        }

        /**
         * Evaluates a security expression.
         */
        public static boolean evaluateExpression(String expression) {
            if (expression == null || expression.trim().isEmpty()) {
                return true;
            }

            expression = expression.trim();

            if (PARAM_REFERENCE_PATTERN.matcher(expression).find()) {
                log.warn("Parameter references (#param) are not supported at runtime. Expression: {}", expression);
                return false;
            }

            return evaluateCompound(expression);
        }

        private static boolean evaluateCompound(String expression) {
            expression = expression.trim();

            if (expression.startsWith("(")) {
                int closeIdx = findMatchingParen(expression, 0);
                if (closeIdx == expression.length() - 1) {
                    return evaluateCompound(expression.substring(1, closeIdx));
                }
            }

            List<String> orParts = splitTopLevel(expression, " or ");
            if (orParts.size() > 1) {
                for (String part : orParts) {
                    if (evaluateCompound(part.trim())) {
                        return true;
                    }
                }
                return false;
            }

            List<String> andParts = splitTopLevel(expression, " and ");
            if (andParts.size() > 1) {
                for (String part : andParts) {
                    if (!evaluateCompound(part.trim())) {
                        return false;
                    }
                }
                return true;
            }

            if (expression.startsWith("!") || expression.startsWith("not ")) {
                String inner = expression.startsWith("!") 
                    ? expression.substring(1).trim() 
                    : expression.substring(4).trim();
                return !evaluateCompound(inner);
            }

            return evaluateSingle(expression);
        }

        private static List<String> splitTopLevel(String expression, String operator) {
            List<String> parts = new ArrayList<>();
            int depth = 0;
            int lastSplit = 0;

            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (depth == 0 && expression.substring(i).startsWith(operator)) {
                    parts.add(expression.substring(lastSplit, i));
                    lastSplit = i + operator.length();
                    i += operator.length() - 1;
                }
            }

            if (lastSplit < expression.length()) {
                parts.add(expression.substring(lastSplit));
            }

            return parts;
    }

    private static int findMatchingParen(String expression, int startIdx) {
        int depth = 0;
        for (int i = startIdx; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static boolean evaluateSingle(String expression) {
        expression = expression.trim();

        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluateCompound(expression.substring(1, expression.length() - 1));
        }

        if (expression.equals("permitAll()") || expression.equals("permitAll")) {
            return true;
        }

        if (expression.equals("denyAll()") || expression.equals("denyAll")) {
            return false;
        }

        if (expression.equals("isAuthenticated()") || expression.equals("isAuthenticated")) {
            return FastSecurityContext.isAuthenticated();
        }

        if (expression.equals("isAnonymous()") || expression.equals("isAnonymous")) {
            return !FastSecurityContext.isAuthenticated();
        }

        if (expression.equals("isFullyAuthenticated()") || expression.equals("isFullyAuthenticated")) {
            return FastSecurityContext.isAuthenticated();
        }

        if (expression.equals("isRememberMe()") || expression.equals("isRememberMe")) {
            log.warn("isRememberMe() is not supported, returning false");
            return false;
        }

        if (!FastSecurityContext.isAuthenticated()) {
            return false;
        }

        Matcher hasRoleMatcher = HAS_ROLE_PATTERN.matcher(expression);
        if (hasRoleMatcher.matches()) {
            String role = hasRoleMatcher.group(1);
            return FastSecurityContext.hasRole(role);
        }

        Matcher hasAnyRoleMatcher = HAS_ANY_ROLE_PATTERN.matcher(expression);
        if (hasAnyRoleMatcher.matches()) {
            String[] roles = parseStringList(hasAnyRoleMatcher.group(1));
            return FastSecurityContext.hasAnyRole(roles);
        }

        Matcher hasAuthorityMatcher = HAS_AUTHORITY_PATTERN.matcher(expression);
        if (hasAuthorityMatcher.matches()) {
            String authority = hasAuthorityMatcher.group(1);
            return FastSecurityContext.hasAuthority(authority);
        }

        Matcher hasAnyAuthorityMatcher = HAS_ANY_AUTHORITY_PATTERN.matcher(expression);
        if (hasAnyAuthorityMatcher.matches()) {
            String[] authorities = parseStringList(hasAnyAuthorityMatcher.group(1));
            return FastSecurityContext.hasAnyAuthority(authorities);
        }

        log.warn("Unknown @PreAuthorize expression: '{}'. Denying access.", expression);
        return false;
    }

    private static String[] parseStringList(String listStr) {
        return listStr.replaceAll("['\"]", "")
                .split("\\s*,\\s*");
    }
}
}
