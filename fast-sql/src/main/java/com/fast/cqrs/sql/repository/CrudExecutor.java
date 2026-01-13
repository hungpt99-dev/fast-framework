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
        log.debug("Executing findById: {}", sql);
        
        Map<String, Object> params = Map.of("id", id);
        List<T> results = jdbcTemplate.query(sql, params, rowMapper);
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<T> findAll() {
        String sql = CrudSqlGenerator.selectAll(metadata);
        log.debug("Executing findAll: {}", sql);
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    public Page<T> findAll(Pageable pageable) {
        // Get total count
        long total = count();
        
        // Get page content
        String sql = CrudSqlGenerator.selectAllPaged(metadata, pageable);
        log.debug("Executing findAll paged: {}", sql);
        
        Map<String, Object> params = Map.of(
            "limit", pageable.size(),
            "offset", pageable.getOffset()
        );
        List<T> content = jdbcTemplate.query(sql, params, rowMapper);
        
        return Page.of(content, pageable, total);
    }

    public List<T> findAll(Sort sort) {
        String sql = CrudSqlGenerator.selectAllSorted(metadata, sort);
        log.debug("Executing findAll sorted: {}", sql);
        
        return jdbcTemplate.query(sql, rowMapper);
    }

    public T save(T entity) {
        ID id = (ID) metadata.getIdValue(entity);
        
        if (id == null || !existsById(id)) {
            // Insert
            String sql = CrudSqlGenerator.insert(metadata);
            log.debug("Executing insert: {}", sql);
            jdbcTemplate.update(sql, new BeanPropertySqlParameterSource(entity));
        } else {
            // Update
            String sql = CrudSqlGenerator.update(metadata);
            log.debug("Executing update: {}", sql);
            
            Map<String, Object> params = new HashMap<>();
            params.put("id", id);
            for (EntityMetadata.ColumnInfo col : metadata.getColumns()) {
                params.put(col.field().getName(), col.getValue(entity));
            }
            jdbcTemplate.update(sql, params);
        }
        
        return entity;
    }

    public void saveAll(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = CrudSqlGenerator.insert(metadata);
        log.debug("Executing batch insert: {} ({} entities)", sql, entities.size());

        BeanPropertySqlParameterSource[] batchParams = entities.stream()
            .map(BeanPropertySqlParameterSource::new)
            .toArray(BeanPropertySqlParameterSource[]::new);

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
        log.debug("Executing batch update: {} ({} entities)", sql, entities.size());

        MapSqlParameterSource[] batchParams = entities.stream()
            .map(entity -> {
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("id", metadata.getIdValue(entity));
                for (EntityMetadata.ColumnInfo col : metadata.getColumns()) {
                    params.addValue(col.field().getName(), col.getValue(entity));
                }
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
        log.debug("Executing batch delete: {} ({} ids)", sql, ids.size());

        MapSqlParameterSource[] batchParams = ids.stream()
            .map(id -> new MapSqlParameterSource("id", id))
            .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    public void deleteById(ID id) {
        String sql = CrudSqlGenerator.deleteById(metadata);
        log.debug("Executing deleteById: {}", sql);
        
        jdbcTemplate.update(sql, Map.of("id", id));
    }

    public void deleteAll() {
        String sql = CrudSqlGenerator.deleteAll(metadata);
        log.debug("Executing deleteAll: {}", sql);
        
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    public boolean existsById(ID id) {
        String sql = CrudSqlGenerator.existsById(metadata);
        log.debug("Executing existsById: {}", sql);
        
        Long count = jdbcTemplate.queryForObject(sql, Map.of("id", id), Long.class);
        return count != null && count > 0;
    }

    public long count() {
        String sql = CrudSqlGenerator.count(metadata);
        log.debug("Executing count: {}", sql);
        
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        return count != null ? count : 0;
    }
}
