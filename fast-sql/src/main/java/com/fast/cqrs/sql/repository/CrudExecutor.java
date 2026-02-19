package com.fast.cqrs.sql.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Executes CRUD operations for FastRepository methods.
 * <p>
 * Generates and executes SQL based on entity metadata.
 */
public class CrudExecutor<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(CrudExecutor.class);

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EntityMetadata<T> metadata;
    private final BeanPropertyRowMapper<T> rowMapper;

    public CrudExecutor(NamedParameterJdbcTemplate jdbcTemplate, Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadata = EntityMetadata.forClass(entityClass);
        this.rowMapper = new BeanPropertyRowMapper<>(entityClass);
    }

    public Optional<T> findById(ID id) {
        String sql = CrudSqlGenerator.selectById(metadata);
        log.trace("Executing findById: {}", sql);
        
        Map<String, Object> params = Map.of("id", id);
        List<T> results = jdbcTemplate.query(sql, params, rowMapper);
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<T> findAll() {
        String sql = CrudSqlGenerator.selectAll(metadata);
        log.trace("Executing findAll: {}", sql);
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Page<T> findAll(Pageable pageable) {
        // Get total count
        long total = count();
        
        // Get page content
        String sql = CrudSqlGenerator.selectAllPaged(metadata, pageable);
        log.trace("Executing findAll paged: {}", sql);
        
        Map<String, Object> params = Map.of(
            "limit", pageable.size(),
            "offset", pageable.getOffset()
        );
        List<T> content = jdbcTemplate.query(sql, params, rowMapper);
        
        return Page.of(content, pageable, total);
    }

    public List<T> findAll(Sort sort) {
        String sql = CrudSqlGenerator.selectAllSorted(metadata, sort);
        log.trace("Executing findAll sorted: {}", sql);
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    @SuppressWarnings("unchecked")
    public T save(T entity) {
        Object idValue = metadata.getIdValue(entity);
        ID id = idValue != null ? (ID) idValue : null;
        
        if (id == null || !existsById(id)) {
            // Insert
            String sql = CrudSqlGenerator.insert(metadata);
            log.trace("Executing insert: {}", sql);
            jdbcTemplate.update(sql, createParameters(entity));
        } else {
            // Update
            String sql = CrudSqlGenerator.update(metadata);
            log.trace("Executing update: {}", sql);
            
            MapSqlParameterSource params = createParameters(entity);
            params.addValue("id", id);
            jdbcTemplate.update(sql, params);
        }
        
        return entity;
    }

    private MapSqlParameterSource createParameters(T entity) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        // Add ID if it's part of the parameters (usually handled separately for updates, but good for insert)
        Object idValue = metadata.getIdValue(entity);
        if (idValue != null) {
            params.addValue("id", idValue);
        }

        for (EntityMetadata.ColumnInfo col : metadata.getColumns()) {
            Object value = col.getValue(entity);
            if (value != null && value.getClass().isArray()) {
                 // Handle Arrays (String[])
                 if (value instanceof String[]) {
                     final String[] arrayValue = (String[]) value;
                     params.addValue(col.field().getName(), new org.springframework.jdbc.core.support.AbstractSqlTypeValue() {
                         @Override
                         protected java.lang.Object createTypeValue(java.sql.Connection conn, int sqlType, String typeName) throws java.sql.SQLException {
                             return conn.createArrayOf("varchar", arrayValue);
                         }
                     });
                 } else {
                     // Default fallback for other arrays or let Spring handle it
                     params.addValue(col.field().getName(), value);
                 }
            } else {
                params.addValue(col.field().getName(), value);
            }
        }
        return params;
    }

    public void saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = CrudSqlGenerator.insert(metadata);
        log.trace("Executing batch insert: {} ({} entities)", sql, entities.size());

        MapSqlParameterSource[] batchParams = entities.stream()
            .map(this::createParameters)
            .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    /**
     * Batch update multiple entities.
     */
    @SuppressWarnings("unchecked")
    public void updateAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = CrudSqlGenerator.update(metadata);
        log.trace("Executing batch update: {} ({} entities)", sql, entities.size());

        MapSqlParameterSource[] batchParams = entities.stream()
            .map(entity -> {
                MapSqlParameterSource params = createParameters(entity);
                // Ensure ID is present (createParameters adds it, but let's be safe)
                params.addValue("id", metadata.getIdValue(entity));
                return params;
            })
            .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    /**
     * Batch delete by IDs.
     */
    public void deleteAllById(List<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        String sql = CrudSqlGenerator.deleteById(metadata);
        log.trace("Executing batch delete: {} ({} ids)", sql, ids.size());

        MapSqlParameterSource[] batchParams = ids.stream()
            .map(id -> new MapSqlParameterSource("id", id))
            .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    public void deleteById(ID id) {
        String sql = CrudSqlGenerator.deleteById(metadata);
        log.trace("Executing deleteById: {}", sql);
        
        jdbcTemplate.update(sql, Map.of("id", id));
    }

    public void deleteAll() {
        String sql = CrudSqlGenerator.deleteAll(metadata);
        log.trace("Executing deleteAll: {}", sql);
        
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    public boolean existsById(ID id) {
        String sql = CrudSqlGenerator.existsById(metadata);
        log.trace("Executing existsById: {}", sql);
        
        Long count = jdbcTemplate.queryForObject(sql, Map.of("id", id), Long.class);
        return count != null && count > 0;
    }

    public long count() {
        String sql = CrudSqlGenerator.count(metadata);
        log.trace("Executing count: {}", sql);
        
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count != null ? count : 0;
    }
}
