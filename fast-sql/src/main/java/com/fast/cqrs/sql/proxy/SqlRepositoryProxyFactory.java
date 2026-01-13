package com.fast.cqrs.sql.proxy;

import com.fast.cqrs.sql.annotation.SqlRepository;
import com.fast.cqrs.sql.executor.SqlExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Proxy;

/**
 * Factory for creating dynamic proxy instances of SQL Repository interfaces.
 */
public class SqlRepositoryProxyFactory {

    private static final Logger log = LoggerFactory.getLogger(SqlRepositoryProxyFactory.class);

    private final SqlExecutor sqlExecutor;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SqlRepositoryProxyFactory(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlExecutor = new SqlExecutor(jdbcTemplate);
    }

    /**
     * Creates a proxy instance for the given repository interface.
     *
     * @param repositoryInterface the repository interface to proxy
     * @param <T>                 the interface type
     * @return a proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T createProxy(Class<T> repositoryInterface) {
        validateInterface(repositoryInterface);

        log.debug("Creating SQL repository proxy for: {}", repositoryInterface.getName());

        SqlRepositoryInvocationHandler handler = new SqlRepositoryInvocationHandler(
            repositoryInterface,
            sqlExecutor,
            jdbcTemplate
        );

        return (T) Proxy.newProxyInstance(
            repositoryInterface.getClassLoader(),
            new Class<?>[] { repositoryInterface },
            handler
        );
    }

    private void validateInterface(Class<?> repositoryInterface) {
        if (!repositoryInterface.isInterface()) {
            throw new SqlRepositoryException(
                repositoryInterface.getName() + " is not an interface"
            );
        }

        if (!repositoryInterface.isAnnotationPresent(SqlRepository.class)) {
            throw new SqlRepositoryException(
                repositoryInterface.getName() + " is not annotated with @SqlRepository"
            );
        }
    }
}
