package com.fast.cqrs.web;

import com.fast.cqrs.web.HttpInvocationContext;
import com.fast.cqrs.cqrs.CqrsDispatcher;
import com.fast.cqrs.web.SecurityInvocationInterceptor;
import com.fast.cqrs.web.ValidationInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK dynamic proxy invocation handler for controller interfaces.
 * <p>
 * This handler intercepts all method calls on the controller proxy
 * and delegates them to the {@link CqrsDispatcher} for CQRS routing.
 * <p>
 * Object methods ({@code toString}, {@code equals}, {@code hashCode})
 * are handled directly without dispatch.
 * <p>
 * Security annotations like {@code @PreAuthorize} are checked before dispatch
 * when Spring Security is available on the classpath.
 * <p>
 * Validation annotations like {@code @Valid} are processed before dispatch
 * when Bean Validation is available on the classpath.
 */
public class ControllerInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(ControllerInvocationHandler.class);

    private final Class<?> controllerInterface;
    private final CqrsDispatcher dispatcher;

    /**
     * Creates a new invocation handler.
     *
     * @param controllerInterface the controller interface being proxied
     * @param dispatcher          the CQRS dispatcher for routing
     */
    public ControllerInvocationHandler(Class<?> controllerInterface, CqrsDispatcher dispatcher) {
        this.controllerInterface = controllerInterface;
        this.dispatcher = dispatcher;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods directly
        if (isObjectMethod(method)) {
            return handleObjectMethod(proxy, method, args);
        }

        log.debug("Intercepted call to {}.{}", 
                  controllerInterface.getSimpleName(), method.getName());

        // 1. Check security annotations (e.g., @PreAuthorize)
        SecurityInvocationInterceptor.checkSecurity(method);

        // 2. Validate arguments (e.g., @Valid)
        ValidationInterceptor.validate(method, args);

        // 3. Create invocation context and dispatch
        HttpInvocationContext context = new HttpInvocationContext(method, args);
        return dispatcher.dispatch(context);
    }

    private boolean isObjectMethod(Method method) {
        String name = method.getName();
        return switch (name) {
            case "toString", "hashCode", "equals", "getClass" -> true;
            default -> false;
        };
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> controllerInterface.getSimpleName() + "@CqrsProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "getClass" -> controllerInterface;
            default -> throw new UnsupportedOperationException(
                "Unsupported Object method: " + method.getName()
            );
        };
    }
}
