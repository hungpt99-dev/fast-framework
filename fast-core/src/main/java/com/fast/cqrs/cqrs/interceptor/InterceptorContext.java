package com.fast.cqrs.cqrs.interceptor;

import java.lang.reflect.Method;

/**
 * Context for intercepted method execution.
 */
public class InterceptorContext {
    
    private final Method method;
    private final Object[] args;
    private final Object target;

    public InterceptorContext(Method method, Object[] args, Object target) {
        this.method = method;
        this.args = args;
        this.target = target;
    }

    public Method getMethod() {
        return method;
    }

    public Object[] getArgs() {
        return args;
    }

    public Object getTarget() {
        return target;
    }

    /**
     * Gets the first argument of the specified type.
     */
    @SuppressWarnings("unchecked")
    public <T> T getArg(Class<T> type) {
        if (args == null) return null;
        for (Object arg : args) {
            if (type.isInstance(arg)) {
                return (T) arg;
            }
        }
        return null;
    }
}
