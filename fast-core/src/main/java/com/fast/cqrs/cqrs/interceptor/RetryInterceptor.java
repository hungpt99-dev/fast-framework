package com.fast.cqrs.cqrs.interceptor;

import com.fast.cqrs.cqrs.annotation.RetryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor that retries failed commands based on {@link RetryCommand} annotation.
 */
public class RetryInterceptor implements CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RetryInterceptor.class);

    @Override
    public Object intercept(InterceptorContext context, InterceptorChain chain) throws Exception {
        Method method = context.getMethod();
        RetryCommand retryCommand = method.getAnnotation(RetryCommand.class);
        
        if (retryCommand == null) {
            return chain.proceed(context);
        }

        int maxAttempts = retryCommand.maxAttempts();
        long backoffMs = parseBackoff(retryCommand.backoff());
        double multiplier = retryCommand.multiplier();
        Class<? extends Throwable>[] retryOn = retryCommand.retryOn();

        Exception lastException = null;
        long currentBackoff = backoffMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return chain.proceed(context);
            } catch (Exception e) {
                if (!shouldRetry(e, retryOn)) {
                    throw e;
                }
                
                lastException = e;
                log.warn("Attempt {}/{} failed for {}: {}", 
                         attempt, maxAttempts, method.getName(), e.getMessage());
                
                if (attempt < maxAttempts) {
                    Thread.sleep(currentBackoff);
                    currentBackoff = (long) (currentBackoff * multiplier);
                }
            }
        }

        throw lastException;
    }

    private boolean shouldRetry(Exception e, Class<? extends Throwable>[] retryOn) {
        if (retryOn.length == 0) {
            return true; // Retry on all exceptions
        }
        for (Class<? extends Throwable> type : retryOn) {
            if (type.isInstance(e)) {
                return true;
            }
        }
        return false;
    }

    private long parseBackoff(String backoff) {
        if (backoff.endsWith("ms")) {
            return Long.parseLong(backoff.replace("ms", ""));
        } else if (backoff.endsWith("s")) {
            return TimeUnit.SECONDS.toMillis(Long.parseLong(backoff.replace("s", "")));
        }
        return 100; // Default 100ms
    }

    @Override
    public int getOrder() {
        return 200; // Run before most interceptors
    }
}
