package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.annotation.Command;
import com.fast.cqrs.cqrs.cache.DurationParser;
import com.fast.cqrs.cqrs.idempotency.IdempotencyStore;
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
import java.util.Optional;

/**
 * Aspect that implements command idempotency.
 * <p>
 * Intercepts methods annotated with @Command that have idempotencyKey set,
 * and ensures the command is executed only once per unique key.
 * <p>
 * For GraalVM native image: This aspect uses reflection-free SpEL evaluation.
 * If you need zero-reflection, use the annotation processor to generate
 * idempotency checks at compile time.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)  // Run before transaction
public class IdempotencyAspect {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyAspect.class);
    private static final SpelExpressionParser PARSER = new SpelExpressionParser();
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration MAX_WAIT_TIMEOUT = Duration.ofSeconds(30);

    private final IdempotencyStore idempotencyStore;

    public IdempotencyAspect(IdempotencyStore idempotencyStore) {
        this.idempotencyStore = idempotencyStore;
    }

    @Around("@annotation(command)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Command command) throws Throwable {
        // Skip if no idempotency key configured
        String keyExpression = command.idempotencyKey();
        if (keyExpression == null || keyExpression.isBlank()) {
            return joinPoint.proceed();
        }

        // Evaluate the key expression
        String idempotencyKey = evaluateKey(joinPoint, keyExpression);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            log.warn("Idempotency key evaluated to null/empty for method {}, proceeding without idempotency",
                    joinPoint.getSignature().getName());
            return joinPoint.proceed();
        }

        // Add method signature to key to avoid collisions across different commands
        String fullKey = buildFullKey(joinPoint, idempotencyKey);
        Duration ttl = DurationParser.parseOrDefault(command.idempotencyTtl(), Duration.ofHours(24));

        // Check if already processed
        Optional<IdempotencyStore.IdempotencyRecord> existing = idempotencyStore.get(fullKey);
        if (existing.isPresent() && existing.get().completed()) {
            log.debug("Idempotency key {} already processed, returning cached result", fullKey);
            return existing.get().result();
        }

        // Try to acquire lock
        if (!idempotencyStore.tryLock(fullKey, LOCK_TIMEOUT)) {
            // Another request is processing this key
            log.debug("Idempotency key {} is being processed by another request, waiting...", fullKey);
            return waitForResult(fullKey, ttl);
        }

        try {
            // Execute the command
            Object result = joinPoint.proceed();
            
            // Store the result
            idempotencyStore.store(fullKey, result, ttl);
            log.debug("Stored idempotency result for key {}", fullKey);
            
            return result;
        } catch (Throwable e) {
            // Unlock on failure to allow retry
            idempotencyStore.unlock(fullKey);
            throw e;
        }
    }

    private String evaluateKey(ProceedingJoinPoint joinPoint, String expression) {
        try {
            EvaluationContext context = createEvaluationContext(joinPoint);
            Expression expr = PARSER.parseExpression(expression);
            Object value = expr.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Failed to evaluate idempotency key expression: {}", expression, e);
            return null;
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
                context.setVariable("cmd", args[i]);
                context.setVariable("command", args[i]);
            }
        }

        // Add indexed access
        for (int i = 0; i < args.length; i++) {
            context.setVariable("p" + i, args[i]);
        }

        return context;
    }

    private String buildFullKey(ProceedingJoinPoint joinPoint, String userKey) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName + ":" + userKey;
    }

    private Object waitForResult(String key, Duration ttl) throws InterruptedException {
        long maxWaitMs = Math.min(ttl.toMillis(), MAX_WAIT_TIMEOUT.toMillis());
        long deadline = System.currentTimeMillis() + maxWaitMs;
        
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);  // Poll every 100ms
            
            Optional<IdempotencyStore.IdempotencyRecord> record = idempotencyStore.get(key);
            if (record.isPresent() && record.get().completed()) {
                return record.get().result();
            }
        }
        
        throw new IllegalStateException("Timeout waiting for idempotent command result: " + key);
    }
}
