package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.web.ControllerProxyFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory bean for creating controller proxy instances.
 * <p>
 * This factory bean is used by Spring to create the actual proxy
 * instances for {@code @HttpController} interfaces.
 *
 * @param <T> the controller interface type
 */
public class ControllerProxyFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> controllerInterface;
    private ControllerProxyFactory proxyFactory;

    /**
     * Creates a new factory bean for the given controller interface.
     *
     * @param controllerInterface the controller interface to proxy
     */
    public ControllerProxyFactoryBean(Class<T> controllerInterface) {
        this.controllerInterface = controllerInterface;
    }

    @Autowired
    public void setProxyFactory(ControllerProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public T getObject() {
        return proxyFactory.createProxy(controllerInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return controllerInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
