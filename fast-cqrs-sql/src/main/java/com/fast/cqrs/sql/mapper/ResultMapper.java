package com.fast.cqrs.sql.mapper;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default result mapper using BeanPropertyRowMapper.
 * <p>
 * Provides automatic mapping from SQL result sets to Java objects
 * with snake_case to camelCase conversion.
 */
public class ResultMapper {

    private final Map<Class<?>, RowMapper<?>> mapperCache = new ConcurrentHashMap<>();

    /**
     * Gets or creates a RowMapper for the given type.
     *
     * @param type the target type
     * @param <T>  the type parameter
     * @return the row mapper
     */
    @SuppressWarnings("unchecked")
    public <T> RowMapper<T> getRowMapper(Class<T> type) {
        return (RowMapper<T>) mapperCache.computeIfAbsent(type, this::createMapper);
    }

    private <T> RowMapper<T> createMapper(Class<T> type) {
        // Handle primitive wrappers and common types
        if (type == String.class) {
            return (rs, rowNum) -> type.cast(rs.getString(1));
        }
        if (type == Integer.class || type == int.class) {
            return (rs, rowNum) -> type.cast(rs.getInt(1));
        }
        if (type == Long.class || type == long.class) {
            return (rs, rowNum) -> type.cast(rs.getLong(1));
        }
        if (type == Boolean.class || type == boolean.class) {
            return (rs, rowNum) -> type.cast(rs.getBoolean(1));
        }
        
        // Default to BeanPropertyRowMapper for complex types
        return BeanPropertyRowMapper.newInstance(type);
    }
}
