package com.fast.cqrs.cqrs.interceptor;

import java.lang.reflect.Method;

/**
 * Interceptor for command/query execution.
 * <p>
 * Interceptors form a chain and can modify or wrap the execution.
 */
public interface CommandInterceptor {
    
    /**
     * Intercepts a command/query execution.
     *
     * @param context the invocation context
     * @param chain   the interceptor chain to continue execution
     * @return the result of the execution
     * @throws Exception if an error occurs
     */
    Object intercept(InterceptorContext context, InterceptorChain chain) throws Exception;
    
    /**
     * Returns the order of this interceptor.
     * Lower values execute first.
     */
    default int getOrder() {
        return 0;
    }
}
