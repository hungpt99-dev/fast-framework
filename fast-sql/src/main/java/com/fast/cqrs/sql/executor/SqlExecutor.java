package com.fast.cqrs.sql.executor;

import com.fast.cqrs.sql.annotation.Execute;
import com.fast.cqrs.sql.annotation.Param;
import com.fast.cqrs.sql.annotation.Select;
import com.fast.cqrs.sql.mapper.ResultMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes SQL statements using Spring's NamedParameterJdbcTemplate.
 * <p>
 * This class handles:
 * <ul>
 *   <li>Parameter binding from @Param annotations</li>
 *   <li>Query vs Execute dispatch</li>
 *   <li>Result mapping based on return type</li>
 * </ul>
 */
public class SqlExecutor {

    private static final Logger log = LoggerFactory.getLogger(SqlExecutor.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ResultMapper resultMapper;

    public SqlExecutor(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.resultMapper = new ResultMapper();
    }

    /**
     * Executes a method with @Select annotation.
     */
    public Object executeSelect(Method method, Object[] args, Select select) {
        String sql = select.value();
        Map<String, Object> params = extractParameters(method, args);
        
        log.debug("Executing SELECT: {} with params: {}", sql, params);

        Class<?> returnType = method.getReturnType();
        
        // Handle Optional return type
        if (returnType == Optional.class) {
            Class<?> elementType = extractOptionalType(method);
            List<?> results = jdbcTemplate.query(sql, params, resultMapper.getRowMapper(elementType));
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        }
        
        // Handle collection return type
        if (Collection.class.isAssignableFrom(returnType)) {
            Class<?> elementType = extractCollectionElementType(method);
            return jdbcTemplate.query(sql, params, resultMapper.getRowMapper(elementType));
        }
        
        // Handle single object return type
        if (returnType == void.class || returnType == Void.class) {
            throw new SqlExecutionException("@Select method cannot have void return type");
        }
        
        List<?> results = jdbcTemplate.query(sql, params, resultMapper.getRowMapper(returnType));
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new SqlExecutionException(
                "Expected single result but got " + results.size() + " for method: " + method.getName()
            );
        }
        return results.get(0);
    }

    /**
     * Executes a method with @Execute annotation.
     */
    public Object executeExecute(Method method, Object[] args, Execute execute) {
        String sql = execute.value();
        Map<String, Object> params = extractParameters(method, args);
        
        log.debug("Executing SQL: {} with params: {}", sql, params);

        int rowsAffected = jdbcTemplate.update(sql, params);
        
        Class<?> returnType = method.getReturnType();
        if (returnType == int.class || returnType == Integer.class) {
            return rowsAffected;
        }
        if (returnType == long.class || returnType == Long.class) {
            return (long) rowsAffected;
        }
        
        return null; // void return
    }

    private Map<String, Object> extractParameters(Method method, Object[] args) {
        Map<String, Object> params = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        
        if (args == null || args.length == 0) {
            return params;
        }
        
        for (int i = 0; i < parameters.length; i++) {
            Param param = parameters[i].getAnnotation(Param.class);
            if (param != null) {
                params.put(param.value(), args[i]);
            }
        }
        
        return params;
    }

    private Class<?> extractCollectionElementType(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                return c;
            }
        }
        return Object.class;
    }

    private Class<?> extractOptionalType(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> c) {
                return c;
            }
        }
        return Object.class;
    }
}
