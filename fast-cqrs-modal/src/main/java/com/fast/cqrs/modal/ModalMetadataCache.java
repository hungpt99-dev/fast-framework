package com.fast.cqrs.modal;

import com.fast.cqrs.modal.annotation.ModalField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches field metadata for Modal classes to reduce reflection overhead.
 * <p>
 * The cache stores information about which fields to include and their
 * configuration (name, format) for each Modal-annotated class.
 */
public final class ModalMetadataCache {

    private static final Map<Class<?>, List<FieldMetadata>> CACHE = new ConcurrentHashMap<>();

    private ModalMetadataCache() {
        // Utility class
    }

    /**
     * Gets the cached metadata for a class.
     *
     * @param clazz the class to get metadata for
     * @return list of field metadata, empty if not a Modal class
     */
    public static List<FieldMetadata> getMetadata(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, ModalMetadataCache::computeMetadata);
    }

    private static List<FieldMetadata> computeMetadata(Class<?> clazz) {
        List<FieldMetadata> metadata = new ArrayList<>();
        
        for (Field field : getAllFields(clazz)) {
            ModalField annotation = field.getAnnotation(ModalField.class);
            if (annotation != null && !annotation.ignore()) {
                field.setAccessible(true);
                String outputName = annotation.name().isEmpty() 
                    ? field.getName() 
                    : annotation.name();
                metadata.add(new FieldMetadata(field, outputName, annotation.format()));
            }
        }
        
        return Collections.unmodifiableList(metadata);
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        
        return fields;
    }

    /**
     * Clears the metadata cache.
     * <p>
     * Useful for testing or when classes are reloaded.
     */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * Metadata record for a single field.
     *
     * @param field      the field reference
     * @param outputName the name to use in output
     * @param format     the format pattern
     */
    public record FieldMetadata(
        Field field,
        String outputName,
        String format
    ) {}
}
