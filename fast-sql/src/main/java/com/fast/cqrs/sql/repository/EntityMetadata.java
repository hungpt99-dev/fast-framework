package com.fast.cqrs.sql.repository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extracts and caches metadata about entity classes.
 * <p>
 * Used to generate SQL statements for CRUD operations.
 */
public class EntityMetadata<T> {

    private static final ConcurrentMap<Class<?>, EntityMetadata<?>> CACHE = new ConcurrentHashMap<>();

    private final Class<T> entityClass;
    private final String tableName;
    private final String idColumnName;
    private final Field idField;
    private final List<ColumnInfo> columns;

    private EntityMetadata(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = extractTableName(entityClass);
        this.columns = extractColumns(entityClass);
        this.idField = findIdField(entityClass);
        this.idColumnName = extractIdColumnName(idField);
    }

    /**
     * Gets or creates metadata for an entity class.
     */
    @SuppressWarnings("unchecked")
    public static <T> EntityMetadata<T> forClass(Class<T> entityClass) {
        return (EntityMetadata<T>) CACHE.computeIfAbsent(entityClass, EntityMetadata::new);
    }

    private String extractTableName(Class<?> clazz) {
        Table tableAnnotation = clazz.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return tableAnnotation.value();
        }
        return toSnakeCase(clazz.getSimpleName());
    }

    private List<ColumnInfo> extractColumns(Class<?> clazz) {
        List<ColumnInfo> cols = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            Column columnAnnotation = field.getAnnotation(Column.class);
            String columnName = columnAnnotation != null 
                    ? columnAnnotation.value() 
                    : toSnakeCase(field.getName());
            boolean isId = field.isAnnotationPresent(Id.class);
            cols.add(new ColumnInfo(field, columnName, isId));
        }
        return cols;
    }

    private Field findIdField(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        // Default to field named "id"
        try {
            Field field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("No @Id field found in " + clazz.getSimpleName());
        }
    }

    private String extractIdColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        return columnAnnotation != null ? columnAnnotation.value() : toSnakeCase(field.getName());
    }

    private String toSnakeCase(String camelCase) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) result.append('_');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    // Getters
    public Class<T> getEntityClass() { return entityClass; }
    public String getTableName() { return tableName; }
    public String getIdColumnName() { return idColumnName; }
    public Field getIdField() { return idField; }
    public List<ColumnInfo> getColumns() { return columns; }

    /**
     * Gets the ID value from an entity.
     */
    public Object getIdValue(T entity) {
        try {
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to get ID value", e);
        }
    }

    /**
     * Column metadata.
     */
    public record ColumnInfo(Field field, String columnName, boolean isId) {
        public Object getValue(Object entity) {
            try {
                return field.get(entity);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to get field value", e);
            }
        }

        public void setValue(Object entity, Object value) {
            try {
                field.set(entity, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Failed to set field value", e);
            }
        }
    }
}
