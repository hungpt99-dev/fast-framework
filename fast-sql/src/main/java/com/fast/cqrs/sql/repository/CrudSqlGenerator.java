package com.fast.cqrs.sql.repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates SQL statements for CRUD operations based on entity metadata.
 */
public class CrudSqlGenerator {

    /**
     * Generates SELECT * FROM table WHERE id = :id
     */
    public static String selectById(EntityMetadata<?> metadata) {
        return String.format(
            "SELECT * FROM %s WHERE %s = :id",
            metadata.getTableName(),
            metadata.getIdColumnName()
        );
    }

    /**
     * Generates SELECT * FROM table
     */
    public static String selectAll(EntityMetadata<?> metadata) {
        return String.format("SELECT * FROM %s", metadata.getTableName());
    }

    /**
     * Generates SELECT * FROM table with pagination
     */
    public static String selectAllPaged(EntityMetadata<?> metadata, Pageable pageable) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(metadata.getTableName());
        
        if (pageable.sort() != null && pageable.sort().isSorted()) {
            sql.append(pageable.sort().toSql());
        }
        
        sql.append(" LIMIT :limit OFFSET :offset");
        return sql.toString();
    }

    /**
     * Generates SELECT * FROM table with sorting
     */
    public static String selectAllSorted(EntityMetadata<?> metadata, Sort sort) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(metadata.getTableName());
        sql.append(sort.toSql());
        return sql.toString();
    }

    /**
     * Generates INSERT INTO table (cols) VALUES (:vals)
     */
    public static String insert(EntityMetadata<?> metadata) {
        List<EntityMetadata.ColumnInfo> columns = metadata.getColumns();
        
        String columnNames = columns.stream()
            .map(EntityMetadata.ColumnInfo::columnName)
            .collect(Collectors.joining(", "));
        
        String paramNames = columns.stream()
            .map(c -> ":" + c.field().getName())
            .collect(Collectors.joining(", "));
        
        return String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            metadata.getTableName(),
            columnNames,
            paramNames
        );
    }

    /**
     * Generates UPDATE table SET col = :val WHERE id = :id
     */
    public static String update(EntityMetadata<?> metadata) {
        List<EntityMetadata.ColumnInfo> columns = metadata.getColumns();
        
        String setClauses = columns.stream()
            .filter(c -> !c.isId())
            .map(c -> c.columnName() + " = :" + c.field().getName())
            .collect(Collectors.joining(", "));
        
        return String.format(
            "UPDATE %s SET %s WHERE %s = :id",
            metadata.getTableName(),
            setClauses,
            metadata.getIdColumnName()
        );
    }

    /**
     * Generates DELETE FROM table WHERE id = :id
     */
    public static String deleteById(EntityMetadata<?> metadata) {
        return String.format(
            "DELETE FROM %s WHERE %s = :id",
            metadata.getTableName(),
            metadata.getIdColumnName()
        );
    }

    /**
     * Generates DELETE FROM table
     */
    public static String deleteAll(EntityMetadata<?> metadata) {
        return String.format("DELETE FROM %s", metadata.getTableName());
    }

    /**
     * Generates SELECT COUNT(*) FROM table
     */
    public static String count(EntityMetadata<?> metadata) {
        return String.format("SELECT COUNT(*) FROM %s", metadata.getTableName());
    }

    /**
     * Generates SELECT COUNT(*) FROM table WHERE id = :id
     */
    public static String existsById(EntityMetadata<?> metadata) {
        return String.format(
            "SELECT COUNT(*) FROM %s WHERE %s = :id",
            metadata.getTableName(),
            metadata.getIdColumnName()
        );
    }
}
