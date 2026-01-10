package com.fast.cqrs.sql.proxy;

import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.executor.SqlExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * JDK dynamic proxy handler for SQL Repository interfaces.
 * <p>
 * Intercepts repository method calls and delegates to the SqlExecutor
 * based on the method's SQL annotation (@Select or @Execute).
 */
public class SqlRepositoryInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(SqlRepositoryInvocationHandler.class);

    private final Class<?> repositoryInterface;
    private final SqlExecutor sqlExecutor;

    public SqlRepositoryInvocationHandler(Class<?> repositoryInterface, SqlExecutor sqlExecutor) {
        this.repositoryInterface = repositoryInterface;
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods
        if (isObjectMethod(method)) {
            return handleObjectMethod(proxy, method, args);
        }

        log.debug("Intercepted call to {}.{}", 
                  repositoryInterface.getSimpleName(), method.getName());

        // Check for @Select annotation
        Select select = method.getAnnotation(Select.class);
        if (select != null) {
            return sqlExecutor.executeSelect(method, args, select);
        }

        // Check for @Execute annotation
        Execute execute = method.getAnnotation(Execute.class);
        if (execute != null) {
            return sqlExecutor.executeExecute(method, args, execute);
        }

        // No SQL annotation found - fail fast
        throw new SqlRepositoryException(
            "Method '" + method.getName() + "' in " + repositoryInterface.getSimpleName() +
            " must be annotated with @Select or @Execute"
        );
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
            case "toString" -> repositoryInterface.getSimpleName() + "@SqlProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "getClass" -> repositoryInterface;
            default -> throw new UnsupportedOperationException(
                "Unsupported Object method: " + method.getName()
            );
        };
    }
}
