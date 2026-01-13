package com.fast.cqrs.cqrs.interceptor;

/**
 * Chain of interceptors for command/query execution.
 */
public interface InterceptorChain {
    
    /**
     * Proceeds to the next interceptor in the chain.
     *
     * @param context the invocation context
     * @return the result of the execution
     * @throws Exception if an error occurs
     */
    Object proceed(InterceptorContext context) throws Exception;
}
