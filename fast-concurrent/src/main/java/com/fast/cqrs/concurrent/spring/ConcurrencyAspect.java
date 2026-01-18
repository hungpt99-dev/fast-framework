package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.annotation.ConcurrentLimit;
import com.fast.cqrs.concurrent.annotation.ConcurrentSafe;
import com.fast.cqrs.concurrent.annotation.KeyedConcurrency;
import com.fast.cqrs.concurrent.resilience.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aspect to enforce concurrency limits.
 * Handles @ConcurrentLimit, @ConcurrentSafe, and @KeyedConcurrency.
 */
@Aspect
@Order(100) // Run before Transaction (usually 200)
public class ConcurrencyAspect {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyAspect.class);
    
    private final ConcurrencyRegistry concurrencyRegistry;
    private final KeyedSemaphoreRegistry keyedRegistry;
    private final FastConcurrentProperties properties;
    private final ApplicationContext applicationContext;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public ConcurrencyAspect(ConcurrencyRegistry concurrencyRegistry,
                             KeyedSemaphoreRegistry keyedRegistry,
                             FastConcurrentProperties properties,
                             ApplicationContext applicationContext) {
        this.concurrencyRegistry = concurrencyRegistry;
        this.keyedRegistry = keyedRegistry;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(safe)")
    public Object handleConcurrentSafe(ProceedingJoinPoint pjp, ConcurrentSafe safe) throws Throwable {
        return handleLimit(pjp, properties.getPermits(), properties.getWaitTimeoutMs(), properties.getRejectPolicy(), null, null);
    }

    @Around("@annotation(limit)")
    public Object handleConcurrentLimit(ProceedingJoinPoint pjp, ConcurrentLimit limit) throws Throwable {
        int permits = limit.permits() > 0 ? limit.permits() : properties.getPermits();
        long timeout = limit.waitTimeoutMs() > 0 ? limit.waitTimeoutMs() : properties.getWaitTimeoutMs();
        RejectPolicy policy = limit.rejectPolicy();
        String fallback = limit.fallback();
        Class<? extends RejectHandler> handlerClass = limit.rejectHandler();

        return handleLimit(pjp, permits, timeout, policy, fallback, handlerClass);
    }
    
    // Class-level annotation support would require @Around("@within(...)") or similar
    // For brevity, focusing on method level first as usually prioritized. 
    // To support class level: @Around("@within(com.fast.cqrs.concurrent.annotation.ConcurrentLimit) || @within(com.fast.cqrs.concurrent.annotation.ConcurrentSafe)")

    @Around("@annotation(keyed)")
    public Object handleKeyedConcurrency(ProceedingJoinPoint pjp, KeyedConcurrency keyed) throws Throwable {
        String key = resolveKey(keyed.key(), pjp);
        String contextKey = pjp.getSignature().toLongString(); // Method ID
        
        int permits = keyed.permits();
        long timeout = keyed.waitTimeoutMs();
        
        if (!keyedRegistry.acquire(contextKey, key, permits, timeout)) {
            return handleRejection(pjp, keyed.rejectPolicy(), keyed.fallback(), keyed.rejectHandler(), "Keyed limit reached for " + key);
        }
        
        try {
            return pjp.proceed();
        } finally {
            keyedRegistry.release(contextKey, key);
        }
    }

    private Object handleLimit(ProceedingJoinPoint pjp, int permits, long timeout, RejectPolicy policy, String fallbackMethod, Class<? extends RejectHandler> handlerClass) throws Throwable {
        String key = pjp.getSignature().toLongString();
        
        if (!concurrencyRegistry.acquire(key, permits, timeout)) {
            return handleRejection(pjp, policy, fallbackMethod, handlerClass, "Concurrency limit reached: " + permits);
        }

        try {
            return pjp.proceed();
        } finally {
            concurrencyRegistry.release(key);
        }
    }

    private Object handleRejection(ProceedingJoinPoint pjp, RejectPolicy policy, String fallbackMethod, Class<? extends RejectHandler> handlerClass, String reason) throws Throwable {
        // 1. Custom Handler
        if (handlerClass != null && handlerClass != RejectHandler.class) {
             RejectHandler handler = applicationContext.getBean(handlerClass); // Assume managed bean
             // If not bean, instantiate? Better to require bean.
             return handler.onReject(new RejectHandler.RejectContext(pjp.getSignature().getName(), pjp.getArgs(), reason));
        }

        // 2. Policy
        switch (policy) {
            case FAIL_FAST:
            case WAIT_TIMEOUT: // Timeout happened
                throw new ConcurrencyRejectedException(reason);
            case WAIT:
                // If we are here, WAIT policy failed (should have waited indefinitely ideally, 
                // but acquire logic uses timeout. If policy is WAIT, logic should have used huge timeout)
                // For simplicity, treating as reject.
                throw new ConcurrencyRejectedException(reason + " (Wait failed)");
            case FALLBACK:
                if (StringUtils.hasText(fallbackMethod)) {
                    return invokeFallback(pjp, fallbackMethod);
                }
                throw new ConcurrencyRejectedException(reason + " (No fallback defined)");
            default:
                throw new ConcurrencyRejectedException(reason);
        }
    }

    private Object invokeFallback(ProceedingJoinPoint pjp, String fallbackMethodName) throws Throwable {
        Object target = pjp.getTarget();
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = target.getClass().getMethod(fallbackMethodName, signature.getParameterTypes());
        method.setAccessible(true);
        return method.invoke(target, pjp.getArgs());
    }

    private String resolveKey(String keyExpression, ProceedingJoinPoint pjp) {
        if (!keyExpression.startsWith("#")) {
            return keyExpression; // Literal
        }
        
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = pjp.getArgs();
        
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        
        Expression expr = expressionCache.computeIfAbsent(keyExpression, parser::parseExpression);
        Object value = expr.getValue(context);
        return value != null ? value.toString() : "null";
    }
}
