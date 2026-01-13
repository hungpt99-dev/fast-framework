package com.fast.cqrs.sql.autoconfigure;

import com.fast.cqrs.sql.proxy.SqlRepositoryProxyFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory bean for creating SQL repository proxy instances.
 *
 * @param <T> the repository interface type
 */
public class SqlRepositoryProxyFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> repositoryInterface;
    private SqlRepositoryProxyFactory proxyFactory;

    public SqlRepositoryProxyFactoryBean(Class<T> repositoryInterface) {
        this.repositoryInterface = repositoryInterface;
    }

    @Autowired
    public void setProxyFactory(SqlRepositoryProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public T getObject() {
        return proxyFactory.createProxy(repositoryInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return repositoryInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
