package com.fast.cqrs.util;

/**
 * JSON utility methods.
 * Supports both Jackson 2.x and Jackson 3.x (Spring Boot 4).
 * Jackson 3.x is automatically detected and preferred when available.
 */
public final class JsonUtil {

    private static final Object MAPPER;
    private static final boolean IS_JACKSON_3;

    static {
        Object mapper = null;
        boolean jackson3 = false;
        
        // Try Jackson 3.x first (Spring Boot 4)
        try {
            Class<?> jsonMapperClass = Class.forName("tools.jackson.databind.json.JsonMapper");
            Object builder = jsonMapperClass.getMethod("builder").invoke(null);
            mapper = builder.getClass().getMethod("build").invoke(builder);
            jackson3 = true;
        } catch (Exception | NoClassDefFoundError e) {
            // Jackson 3 not available, try Jackson 2
            try {
                mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                ((com.fasterxml.jackson.databind.ObjectMapper) mapper).findAndRegisterModules();
            } catch (NoClassDefFoundError e2) {
                // Jackson not available
            }
        }
        MAPPER = mapper;
        IS_JACKSON_3 = jackson3;
    }

    private JsonUtil() {}

    /**
     * Checks if Jackson is available.
     */
    public static boolean isAvailable() {
        return MAPPER != null;
    }

    /**
     * Checks if using Jackson 3.x.
     */
    public static boolean isJackson3() {
        return IS_JACKSON_3;
    }

    /**
     * Converts object to JSON string.
     */
    public static String toJson(Object obj) {
        if (MAPPER == null) {
            throw new IllegalStateException("Jackson is not on classpath");
        }
        try {
            if (IS_JACKSON_3) {
                return (String) MAPPER.getClass().getMethod("writeValueAsString", Object.class).invoke(MAPPER, obj);
            } else {
                return ((com.fasterxml.jackson.databind.ObjectMapper) MAPPER).writeValueAsString(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Converts object to pretty JSON string.
     */
    public static String toPrettyJson(Object obj) {
        if (MAPPER == null) {
            throw new IllegalStateException("Jackson is not on classpath");
        }
        try {
            if (IS_JACKSON_3) {
                Object writer = MAPPER.getClass().getMethod("writerWithDefaultPrettyPrinter").invoke(MAPPER);
                return (String) writer.getClass().getMethod("writeValueAsString", Object.class).invoke(writer, obj);
            } else {
                return ((com.fasterxml.jackson.databind.ObjectMapper) MAPPER).writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Parses JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> type) {
        if (MAPPER == null) {
            throw new IllegalStateException("Jackson is not on classpath");
        }
        try {
            if (IS_JACKSON_3) {
                return type.cast(MAPPER.getClass().getMethod("readValue", String.class, Class.class).invoke(MAPPER, json, type));
            } else {
                return ((com.fasterxml.jackson.databind.ObjectMapper) MAPPER).readValue(json, type);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    /**
     * Converts object to Map.
     */
    @SuppressWarnings("unchecked")
    public static java.util.Map<String, Object> toMap(Object obj) {
        if (MAPPER == null) {
            throw new IllegalStateException("Jackson is not on classpath");
        }
        try {
            if (IS_JACKSON_3) {
                return (java.util.Map<String, Object>) MAPPER.getClass()
                    .getMethod("convertValue", Object.class, Class.class)
                    .invoke(MAPPER, obj, java.util.Map.class);
            } else {
                return ((com.fasterxml.jackson.databind.ObjectMapper) MAPPER).convertValue(obj, java.util.Map.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to Map", e);
        }
    }
}
