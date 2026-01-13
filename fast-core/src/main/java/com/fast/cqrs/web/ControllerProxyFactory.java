package com.fast.cqrs.web;

import com.fast.cqrs.cqrs.annotation.HttpController;
import com.fast.cqrs.cqrs.CqrsDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * Factory for creating dynamic proxy instances of controller interfaces.
 * <p>
 * This factory creates JDK dynamic proxies for interfaces annotated with
 * {@link HttpController}. The proxies delegate all method calls to the
 * {@link CqrsDispatcher}.
 */
public class ControllerProxyFactory {

    private static final Logger log = LoggerFactory.getLogger(ControllerProxyFactory.class);

    private final CqrsDispatcher dispatcher;

    /**
     * Creates a new proxy factory.
     *
     * @param dispatcher the CQRS dispatcher to use for proxies
     */
    public ControllerProxyFactory(CqrsDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Creates a proxy instance for the given controller interface.
     *
     * @param controllerInterface the controller interface to proxy
     * @param <T>                 the interface type
     * @return a proxy instance implementing the interface
     * @throws IllegalArgumentException if the interface is not valid
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> controllerInterface) {
        validateInterface(controllerInterface);

        log.debug("Creating proxy for controller: {}", controllerInterface.getName());

        ControllerInvocationHandler handler = new ControllerInvocationHandler(
            controllerInterface, 
            dispatcher
        );

        return (T) Proxy.newProxyInstance(
            controllerInterface.getClassLoader(),
            new Class<?>[] { controllerInterface },
            handler
        );
    }

    private void validateInterface(Class<?> controllerInterface) {
        if (!controllerInterface.isInterface()) {
            throw new IllegalArgumentException(
                controllerInterface.getName() + " is not an interface"
            );
        }

        if (!controllerInterface.isAnnotationPresent(HttpController.class)) {
            throw new IllegalArgumentException(
                controllerInterface.getName() + " is not annotated with @HttpController"
            );
        }
    }
}
