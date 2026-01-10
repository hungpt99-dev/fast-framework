package com.fast.cqrs.logging.aspect;

import com.fast.cqrs.logging.FrameworkLoggers;
import com.fast.cqrs.logging.annotation.TraceLog;
import com.fast.cqrs.logging.util.SafeLogUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * AOP aspect for @TraceLog annotation.
 * <p>
 * Logs method execution timing and detects slow methods.
 * <p>
 * <strong>Critical:</strong> This aspect NEVER logs exceptions.
 * Exceptions are propagated and logged by the global exception handler.
 */
@Aspect
public class TraceLogAspect {

    @Around("@annotation(traceLog)")
    public Object trace(ProceedingJoinPoint joinPoint, TraceLog traceLog) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        // Log entry
        if (traceLog.logArgs()) {
            FrameworkLoggers.TRACE.debug("-> {} args={}", 
                methodName, SafeLogUtil.safeArgs(joinPoint.getArgs()));
        } else {
            FrameworkLoggers.TRACE.debug("-> {}", methodName);
        }

        long startTime = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
        } finally {
            // Always log timing, even on exception
            long duration = System.currentTimeMillis() - startTime;
            logCompletion(methodName, duration, traceLog.slowMs());
        }

        // Log result if enabled
        if (traceLog.logResult() && result != null) {
            FrameworkLoggers.TRACE.debug("<- {} result={}", 
                methodName, SafeLogUtil.safeValue(result));
        }

        return result;
    }

    private void logCompletion(String methodName, long duration, long slowMs) {
        if (duration >= slowMs) {
            FrameworkLoggers.TRACE.warn("<- {} SLOW {}ms (threshold: {}ms)", 
                methodName, duration, slowMs);
        } else {
            FrameworkLoggers.TRACE.debug("<- {} {}ms", methodName, duration);
        }
    }
}
