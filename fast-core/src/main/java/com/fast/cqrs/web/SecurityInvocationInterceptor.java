package com.fast.cqrs.web;

import com.fast.cqrs.security.FastSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            available = true;
        } catch (ClassNotFoundException e) {
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

    /**
     * Evaluates a security expression.
     * <p>
     * Supports:
     * <ul>
     *   <li>Simple expressions: hasRole('X'), isAuthenticated()</li>
     *   <li>Compound expressions: expr1 and expr2, expr1 or expr2</li>
     *   <li>Grouped expressions: (expr1 or expr2) and expr3</li>
     * </ul>
     *
     * @param expression the SpEL-like expression
     * @return true if access granted
     */
    public static boolean evaluateExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        expression = expression.trim();

        // Check for parameter references (not supported at runtime)
        if (PARAM_REFERENCE_PATTERN.matcher(expression).find()) {
            log.warn("Parameter references (#param) are not supported at runtime. Expression: {}", expression);
            // For safety, deny access when we can't evaluate
            return false;
        }

        // Parse and evaluate compound expressions
        return evaluateCompound(expression);
    }

    /**
     * Evaluates compound expressions with 'and' / 'or' operators.
     */
    private static boolean evaluateCompound(String expression) {
        expression = expression.trim();

        // Handle parentheses first - find matching pairs
        if (expression.startsWith("(")) {
            int closeIdx = findMatchingParen(expression, 0);
            if (closeIdx == expression.length() - 1) {
                // Entire expression is wrapped in parentheses
                return evaluateCompound(expression.substring(1, closeIdx));
            }
        }

        // Split by 'or' at the top level (lowest precedence)
        List<String> orParts = splitTopLevel(expression, " or ");
        if (orParts.size() > 1) {
            for (String part : orParts) {
                if (evaluateCompound(part.trim())) {
                    return true;
                }
            }
            return false;
        }

        // Split by 'and' at the top level
        List<String> andParts = splitTopLevel(expression, " and ");
        if (andParts.size() > 1) {
            for (String part : andParts) {
                if (!evaluateCompound(part.trim())) {
                    return false;
                }
            }
            return true;
        }

        // Handle negation
        if (expression.startsWith("!") || expression.startsWith("not ")) {
            String inner = expression.startsWith("!") 
                ? expression.substring(1).trim() 
                : expression.substring(4).trim();
            return !evaluateCompound(inner);
        }

        // Evaluate single expression
        return evaluateSingle(expression);
    }

    /**
     * Splits expression by operator, respecting parentheses.
     */
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

    /**
     * Finds the index of the closing parenthesis matching the one at startIdx.
     */
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

    /**
     * Evaluates a single (non-compound) expression.
     */
    private static boolean evaluateSingle(String expression) {
        expression = expression.trim();

        // Handle grouped expression
        if (expression.startsWith("(") && expression.endsWith(")")) {
            return evaluateCompound(expression.substring(1, expression.length() - 1));
        }

        // Handle permitAll()
        if (expression.equals("permitAll()") || expression.equals("permitAll")) {
            return true;
        }

        // Handle denyAll()
        if (expression.equals("denyAll()") || expression.equals("denyAll")) {
            return false;
        }

        // Handle isAuthenticated()
        if (expression.equals("isAuthenticated()") || expression.equals("isAuthenticated")) {
            return FastSecurityContext.isAuthenticated();
        }

        // Handle isAnonymous()
        if (expression.equals("isAnonymous()") || expression.equals("isAnonymous")) {
            return !FastSecurityContext.isAuthenticated();
        }

        // Handle isFullyAuthenticated() - treat same as isAuthenticated for simplicity
        if (expression.equals("isFullyAuthenticated()") || expression.equals("isFullyAuthenticated")) {
            return FastSecurityContext.isAuthenticated();
        }

        // Handle isRememberMe() - not supported, return false
        if (expression.equals("isRememberMe()") || expression.equals("isRememberMe")) {
            log.warn("isRememberMe() is not supported, returning false");
            return false;
        }

        // Require authentication for role/authority checks
        if (!FastSecurityContext.isAuthenticated()) {
            return false;
        }

        // Handle hasRole('ROLE')
        Matcher hasRoleMatcher = HAS_ROLE_PATTERN.matcher(expression);
        if (hasRoleMatcher.matches()) {
            String role = hasRoleMatcher.group(1);
            return FastSecurityContext.hasRole(role);
        }

        // Handle hasAnyRole('ROLE1', 'ROLE2')
        Matcher hasAnyRoleMatcher = HAS_ANY_ROLE_PATTERN.matcher(expression);
        if (hasAnyRoleMatcher.matches()) {
            String[] roles = parseStringList(hasAnyRoleMatcher.group(1));
            return FastSecurityContext.hasAnyRole(roles);
        }

        // Handle hasAuthority('AUTHORITY')
        Matcher hasAuthorityMatcher = HAS_AUTHORITY_PATTERN.matcher(expression);
        if (hasAuthorityMatcher.matches()) {
            String authority = hasAuthorityMatcher.group(1);
            return FastSecurityContext.hasAuthority(authority);
        }

        // Handle hasAnyAuthority('AUTH1', 'AUTH2')
        Matcher hasAnyAuthorityMatcher = HAS_ANY_AUTHORITY_PATTERN.matcher(expression);
        if (hasAnyAuthorityMatcher.matches()) {
            String[] authorities = parseStringList(hasAnyAuthorityMatcher.group(1));
            return FastSecurityContext.hasAnyAuthority(authorities);
        }

        // Unknown expression - deny by default for security
        log.warn("Unknown @PreAuthorize expression: '{}'. Denying access.", expression);
        return false;
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

    /**
     * Parses a comma-separated list of quoted strings.
     * e.g., "'ADMIN', 'USER'" -> ["ADMIN", "USER"]
     */
    private static String[] parseStringList(String listStr) {
        return listStr.replaceAll("['\"]", "")
                .split("\\s*,\\s*");
    }
}
