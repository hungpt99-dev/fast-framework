package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.annotation.Query;
import com.fast.cqrs.cqrs.cache.DurationParser;
import com.fast.cqrs.cqrs.cache.QueryCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Aspect that implements query result caching.
 * <p>
 * Intercepts methods annotated with @Query that have cache set,
 * and caches the result for the specified TTL.
 * <p>
 * For GraalVM native image: This aspect uses reflection-free SpEL evaluation.
 * If you need zero-reflection, use the annotation processor to generate
 * caching logic at compile time.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)  // Run before other interceptors
public class QueryCacheAspect {

    private static final Logger log = LoggerFactory.getLogger(QueryCacheAspect.class);
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private final QueryCache queryCache;

    public QueryCacheAspect(QueryCache queryCache) {
        this.queryCache = queryCache;
    }

    @Around("@annotation(query)")
    public Object handleCache(ProceedingJoinPoint joinPoint, Query query) throws Throwable {
        // Skip if no cache TTL configured
        String cacheTtl = query.cache();
        if (cacheTtl == null || cacheTtl.isBlank()) {
            return joinPoint.proceed();
        }

        // Build cache key
        String cacheKey = buildCacheKey(joinPoint, query);
        Duration ttl = DurationParser.parse(cacheTtl);

        // Check cache
        Optional<Object> cached = queryCache.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("Cache hit for key: {}", cacheKey);
            return cached.get();
        }

        // Execute query
        log.debug("Cache miss for key: {}", cacheKey);
        Object result = joinPoint.proceed();

        // Store in cache (don't cache null results)
        if (result != null) {
            queryCache.put(cacheKey, result, ttl);
            log.debug("Cached result for key: {} with TTL: {}", cacheKey, cacheTtl);
        }

        return result;
    }

    private String buildCacheKey(ProceedingJoinPoint joinPoint, Query query) {
        String keyExpression = query.cacheKey();
        
        if (keyExpression != null && !keyExpression.isBlank()) {
            // Use SpEL expression
            return evaluateKey(joinPoint, keyExpression);
        }
        
        // Auto-generate key from method signature and arguments
        return generateDefaultKey(joinPoint);
    }

    private String evaluateKey(ProceedingJoinPoint joinPoint, String expression) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expr = PARSER.parseExpression(expression);
            Object value = expr.getValue(context);
            
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String prefix = signature.getDeclaringType().getSimpleName() + "." + signature.getName();
            
            return prefix + ":" + (value != null ? value.toString() : "null");
        } catch (Exception e) {
            log.error("Failed to evaluate cache key expression: {}, falling back to default", expression, e);
            return generateDefaultKey(joinPoint);
        }
    }

    private EvaluationContext createEvaluationContext(ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        // Add all parameters by name
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            context.setVariable(paramName, args[i]);
            
            // Also add common aliases
            if (i == 0) {
                context.setVariable("query", args[i]);
                context.setVariable("q", args[i]);
            }
        }

        // Add indexed access
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
        }

        return context;
    }

    private String generateDefaultKey(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        
        String argsKey = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg != null ? String.valueOf(arg.hashCode()) : "null")
                .collect(Collectors.joining(","));
        
        return className + "." + methodName + ":" + argsKey;
    }
}
