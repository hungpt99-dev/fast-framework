package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.annotation.ExecutionTimeout;
import com.fast.cqrs.concurrent.context.ContextSnapshot;
import com.fast.cqrs.concurrent.executor.ExecutorRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Enforces strict execution timeout by running the task in a separate thread.
 */
@Aspect
@Order(50) // Run inside ConcurrencyAspect (100) -> [Concurrency [Timeout [Transaction [Method]]]]
public class ExecutionTimeoutAspect {

    @Around("@annotation(timeout)")
    public Object handleTimeout(ProceedingJoinPoint pjp, ExecutionTimeout timeout) throws Throwable {
        long ms = timeout.ms();
        if (ms <= 0) {
            return pjp.proceed();
        }

        ContextSnapshot snapshot = ContextSnapshot.capture();
        ExecutorService executor = ExecutorRegistry.getOrDefault(ExecutorRegistry.VIRTUAL);

        Future<Object> future = executor.submit(() -> {
            snapshot.restore();
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                // Wrap checked exceptions to runtime to propogate out of Future
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e);
            } finally {
                snapshot.clear();
            }
        });

        try {
            return future.get(ms, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Execution timed out after " + ms + "ms");
        } catch (ExecutionException e) {
            throw e.getCause(); // Unwrap invocation exception
        }
    }
}
