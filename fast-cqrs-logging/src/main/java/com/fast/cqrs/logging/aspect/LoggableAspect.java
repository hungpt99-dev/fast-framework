package com.fast.cqrs.logging.aspect;

import com.fast.cqrs.logging.FrameworkLoggers;
import com.fast.cqrs.logging.annotation.Loggable;
import com.fast.cqrs.logging.util.SafeLogUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

/**
 * AOP aspect for @Loggable annotation.
 * <p>
 * Logs explicit business events at INFO level.
 */
@Aspect
public class LoggableAspect {

    @Around("@annotation(loggable)")
    public Object log(ProceedingJoinPoint joinPoint, Loggable loggable) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        // Log business event
        if (loggable.logArgs()) {
            FrameworkLoggers.BUSINESS.info("[BIZ] {} - {} args={}", 
                loggable.value(), methodName, SafeLogUtil.safeArgs(joinPoint.getArgs()));
        } else {
            FrameworkLoggers.BUSINESS.info("[BIZ] {} - {}", loggable.value(), methodName);
        }

        // Proceed (no exception logging here)
        return joinPoint.proceed();
    }
}
