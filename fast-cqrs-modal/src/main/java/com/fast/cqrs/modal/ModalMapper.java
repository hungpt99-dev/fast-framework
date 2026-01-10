package com.fast.cqrs.modal;

import com.fast.cqrs.modal.annotation.Modal;
import com.fast.cqrs.modal.ModalMetadataCache.FieldMetadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for converting Modal-annotated entities to Map representations.
 * <p>
 * This class provides reflection-based conversion of entities to
 * {@code Map<String, Object>} for use as DTOs, event payloads, or
 * HTTP responses.
 * <p>
 * Features:
 * <ul>
 *   <li>Only includes fields annotated with {@code @ModalField}</li>
 *   <li>Supports nested entities annotated with {@code @Modal}</li>
 *   <li>Handles collections automatically</li>
 *   <li>Supports field renaming and formatting</li>
 *   <li>Caches metadata for performance</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * User user = new User("u123", "Alice", "secret");
 * Map<String, Object> dto = ModalMapper.toMap(user);
 * }</pre>
 *
 * @see Modal
 * @see com.fast.cqrs.modal.annotation.ModalField
 */
public final class ModalMapper {

    private ModalMapper() {
        // Utility class
    }

    /**
     * Converts an entity to a Map representation.
     *
     * @param entity the entity to convert
     * @return the map representation, or null if entity is null
     * @throws ModalMappingException if conversion fails
     */
    public static Map<String, Object> toMap(Object entity) {
        if (entity == null) {
            return null;
        }

        if (!entity.getClass().isAnnotationPresent(Modal.class)) {
            throw new ModalMappingException(
                "Class " + entity.getClass().getName() + " is not annotated with @Modal"
            );
        }

        return convertToMap(entity);
    }

    /**
     * Converts an entity to a Map, returning null if not a Modal class.
     * <p>
     * Unlike {@link #toMap(Object)}, this method does not throw if the
     * entity is not annotated with @Modal.
     *
     * @param entity the entity to convert
     * @return the map representation, or null
     */
    public static Map<String, Object> toMapOrNull(Object entity) {
        if (entity == null || !entity.getClass().isAnnotationPresent(Modal.class)) {
            return null;
        }
        return convertToMap(entity);
    }

    private static Map<String, Object> convertToMap(Object entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<FieldMetadata> metadata = ModalMetadataCache.getMetadata(entity.getClass());

        for (FieldMetadata field : metadata) {
            try {
                Object value = field.field().get(entity);
                value = processValue(value, field.format());
                result.put(field.outputName(), value);
            } catch (IllegalAccessException e) {
                throw new ModalMappingException(
                    "Failed to access field: " + field.field().getName(), e
                );
            }
        }

        return result;
    }

    private static Object processValue(Object value, String format) {
        if (value == null) {
            return null;
        }

        // Apply formatting if specified
        if (!format.isEmpty()) {
            value = FieldFormatter.format(value, format);
        }

        // Handle nested Modal entities
        if (value.getClass().isAnnotationPresent(Modal.class)) {
            return convertToMap(value);
        }

        // Handle collections
        if (value instanceof Collection<?> collection) {
            return processCollection(collection);
        }

        return value;
    }

    private static List<Object> processCollection(Collection<?> collection) {
        List<Object> result = new ArrayList<>();
        
        for (Object item : collection) {
            if (item != null && item.getClass().isAnnotationPresent(Modal.class)) {
                result.add(convertToMap(item));
            } else {
                result.add(item);
            }
        }
        
        return result;
    }
}
