package com.fast.cqrs.sql.proxy;

import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.executor.SqlExecutor;
import com.fast.cqrs.sql.repository.CrudExecutor;
import com.fast.cqrs.sql.repository.FastRepository;
import com.fast.cqrs.sql.repository.Page;
import com.fast.cqrs.sql.repository.Pageable;
import com.fast.cqrs.sql.repository.PagingRepository;
import com.fast.cqrs.sql.repository.Sort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/**
 * JDK dynamic proxy handler for SQL Repository interfaces.
 * <p>
 * Intercepts repository method calls and delegates to:
 * <ul>
 *   <li>CrudExecutor for inherited FastRepository methods</li>
 *   <li>SqlExecutor for custom @Select/@Execute methods</li>
 * </ul>
 */
public class SqlRepositoryInvocationHandler implements InvocationHandler {

    private static final Logger log = LoggerFactory.getLogger(SqlRepositoryInvocationHandler.class);

    private final Class<?> repositoryInterface;
    private final SqlExecutor sqlExecutor;
    private final CrudExecutor<?, ?> crudExecutor;

    public SqlRepositoryInvocationHandler(Class<?> repositoryInterface, SqlExecutor sqlExecutor, 
                                          NamedParameterJdbcTemplate jdbcTemplate) {
        this.repositoryInterface = repositoryInterface;
        this.sqlExecutor = sqlExecutor;
        this.crudExecutor = createCrudExecutor(repositoryInterface, jdbcTemplate);
    }

    // Legacy constructor for backward compatibility
    public SqlRepositoryInvocationHandler(Class<?> repositoryInterface, SqlExecutor sqlExecutor) {
        this.repositoryInterface = repositoryInterface;
        this.sqlExecutor = sqlExecutor;
        this.crudExecutor = null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CrudExecutor<?, ?> createCrudExecutor(Class<?> repoInterface, NamedParameterJdbcTemplate jdbcTemplate) {
        // Check if repository extends FastRepository
        if (!FastRepository.class.isAssignableFrom(repoInterface)) {
            return null;
        }
        
        // Extract entity type from generic parameter
        Class<?> entityClass = extractEntityType(repoInterface);
        if (entityClass != null) {
            return new CrudExecutor(jdbcTemplate, entityClass);
        }
        return null;
    }

    private Class<?> extractEntityType(Class<?> repoInterface) {
        for (Type genericInterface : repoInterface.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType pt) {
                Type rawType = pt.getRawType();
                if (rawType == FastRepository.class || rawType == PagingRepository.class) {
                    Type[] typeArgs = pt.getActualTypeArguments();
                    if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                        return c;
                    }
                }
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods
        if (isObjectMethod(method)) {
            return handleObjectMethod(proxy, method, args);
        }

        log.debug("Intercepted call to {}.{}", 
                  repositoryInterface.getSimpleName(), method.getName());

        // Check for FastRepository CRUD methods
        if (crudExecutor != null) {
            Object crudResult = handleCrudMethod(method, args);
            if (crudResult != UNHANDLED) {
                return crudResult;
            }
        }

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

    private static final Object UNHANDLED = new Object();

    @SuppressWarnings("unchecked")
    private Object handleCrudMethod(Method method, Object[] args) {
        CrudExecutor<Object, Object> executor = (CrudExecutor<Object, Object>) crudExecutor;
        String methodName = method.getName();

        return switch (methodName) {
            case "findById" -> executor.findById(args[0]);
            case "findAll" -> {
                if (args == null || args.length == 0) {
                    yield executor.findAll();
                } else if (args[0] instanceof Pageable p) {
                    yield executor.findAll(p);
                } else if (args[0] instanceof Sort s) {
                    yield executor.findAll(s);
                }
                yield UNHANDLED;
            }
            case "save" -> executor.save(args[0]);
            case "saveAll" -> { executor.saveAll((List<Object>) args[0]); yield null; }
            case "updateAll" -> { executor.updateAll((List<Object>) args[0]); yield null; }
            case "deleteById" -> { executor.deleteById(args[0]); yield null; }
            case "deleteAllById" -> { executor.deleteAllById((List<Object>) args[0]); yield null; }
            case "deleteAll" -> { executor.deleteAll(); yield null; }
            case "existsById" -> executor.existsById(args[0]);
            case "count" -> executor.count();
            default -> UNHANDLED;
        };
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

