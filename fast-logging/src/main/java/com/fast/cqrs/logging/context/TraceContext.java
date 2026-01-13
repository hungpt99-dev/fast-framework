package com.fast.cqrs.logging.context;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility for managing MDC trace context.
 * <p>
 * Provides access to standard MDC keys used by the framework:
 * <ul>
 *   <li>{@code traceId} - Unique ID per request</li>
 *   <li>{@code userId} - Optional user identifier</li>
 * </ul>
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_PATH = "requestPath";

    private TraceContext() {}

    /**
     * Gets the current trace ID from MDC.
     *
     * @return the trace ID, or null if not set
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    /**
     * Sets the trace ID in MDC.
     *
     * @param traceId the trace ID
     */
    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    /**
     * Generates and sets a new trace ID.
     *
     * @return the generated trace ID
     */
    public static String generateTraceId() {
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        setTraceId(traceId);
        return traceId;
    }

    /**
     * Gets the current user ID from MDC.
     *
     * @return the user ID, or null if not set
     */
    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    /**
     * Sets the user ID in MDC.
     *
     * @param userId the user ID
     */
    public static void setUserId(String userId) {
        if (userId != null) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Sets the request path in MDC.
     *
     * @param path the request path
     */
    public static void setRequestPath(String path) {
        if (path != null) {
            MDC.put(REQUEST_PATH, path);
        }
    }

    /**
     * Clears all framework MDC values.
     */
    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(REQUEST_PATH);
    }
}
