package com.fast.cqrs.cqrs.interceptor;

import java.util.List;
import java.util.function.Function;

/**
 * Default implementation of {@link InterceptorChain}.
 */
public class DefaultInterceptorChain implements InterceptorChain {

    private final List<CommandInterceptor> interceptors;
    private final Function<InterceptorContext, Object> finalHandler;
    private int currentIndex = 0;

    public DefaultInterceptorChain(List<CommandInterceptor> interceptors, 
                                   Function<InterceptorContext, Object> finalHandler) {
        this.interceptors = interceptors;
        this.finalHandler = finalHandler;
    }

    @Override
    public Object proceed(InterceptorContext context) throws Exception {
        if (currentIndex < interceptors.size()) {
            CommandInterceptor interceptor = interceptors.get(currentIndex++);
            return interceptor.intercept(context, this);
        }
        return finalHandler.apply(context);
    }
}
