package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.annotation.UseExecutor;
import com.fast.cqrs.concurrent.context.ContextSnapshot;
import com.fast.cqrs.concurrent.executor.ExecutorRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Offloads execution to a named executor.
 * Supports void (fire-and-forget), Future, and CompletableFuture return types.
 */
@Aspect
@Order(50)
public class UseExecutorAspect {

    @Around("@annotation(useExecutor)")
    public Object handleUseExecutor(ProceedingJoinPoint pjp, UseExecutor useExecutor) throws Throwable {
        String executorName = useExecutor.value();
        ExecutorService executor = ExecutorRegistry.get(executorName);
        ContextSnapshot snapshot = ContextSnapshot.capture();

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> returnType = signature.getReturnType();

        // Case 1: Void (Fire and Forget)
        if (returnType == void.class) {
            executor.submit(snapshot.wrap(() -> {
                try {
                    pjp.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }));
            return null;
        }

        // Case 2: CompletableFuture
        if (CompletableFuture.class.isAssignableFrom(returnType)) {
            return CompletableFuture.supplyAsync(() -> {
                snapshot.restore();
                try {
                    Object result = pjp.proceed();
                    // If the method itself returns a Future, we should ideally unwrap it, 
                    // but standard usage for @UseExecutor implies the method logic is synchronous 
                    // and we make it async. 
                    // If the method returns CompletableFuture, it might be double wrapped.
                    // Assuming user writes sync logic inside code but returns CompletableFuture signature?
                    // Actually, if they write `public CompletableFuture<String> do()` they probably return `CompletableFuture.completedFuture()`.
                    // Let's assume standard usage: public T doSomething() annotated with @UseExecutor returns CompletableFuture<T> ?
                    // No, AspectJ doesn't change return type. The method signature MUST be CompletableFuture.
                    // So the user method must return CompletableFuture.
                    
                    if (result instanceof CompletableFuture) {
                        return ((CompletableFuture<?>) result).join();
                    }
                    return result;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                } finally {
                    snapshot.clear();
                }
            }, executor);
        }

        // Case 3: Future (Simple submit)
        if (Future.class.isAssignableFrom(returnType)) {
            return executor.submit(snapshot.wrap((Callable<Object>) () -> {
                try {
                    return pjp.proceed();
                } catch (Throwable e) {
                    if (e instanceof Exception) throw (Exception) e;
                    throw new RuntimeException(e);
                }
            }));
        }

        // Case 4: Synchronous return type (Blocking offload)
        Future<Object> future = executor.submit(snapshot.wrap((Callable<Object>) () -> {
            try {
                return pjp.proceed();
            } catch (Throwable e) {
                if (e instanceof Exception) throw (Exception) e;
                throw new RuntimeException(e);
            }
        }));
        
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }
}
