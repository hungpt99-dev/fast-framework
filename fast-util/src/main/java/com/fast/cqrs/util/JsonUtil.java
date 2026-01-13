package com.fast.cqrs.util;

/**
 * JSON utility methods.
 * Requires Jackson on classpath.
 */
public final class JsonUtil {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER;

    static {
        com.fasterxml.jackson.databind.ObjectMapper mapper = null;
        try {
            mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules();
        } catch (NoClassDefFoundError e) {
            // Jackson not available
        }
        MAPPER = mapper;
    }

    private JsonUtil() {}

    /**
     * Checks if Jackson is available.
     */
    public static boolean isAvailable() {
        return MAPPER != null;
    }

    /**
     * Converts object to JSON string.
     */
    public static String toJson(Object obj) {
        if (MAPPER == null) {
            throw new IllegalStateException("Jackson is not on classpath");
        }
        try {
            return MAPPER.writeValueAsString(obj);
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
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
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
            return MAPPER.readValue(json, type);
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
        return MAPPER.convertValue(obj, java.util.Map.class);
    }
}
