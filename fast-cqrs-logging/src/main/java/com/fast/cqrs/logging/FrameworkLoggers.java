package com.fast.cqrs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Framework logger constants for consistent log categorization.
 * <p>
 * Use these loggers for framework-level logging to enable
 * easy filtering in log aggregation systems (ELK, etc.).
 */
public final class FrameworkLoggers {

    private FrameworkLoggers() {}

    /**
     * Logger for HTTP request lifecycle events.
     */
    public static final Logger HTTP = LoggerFactory.getLogger("FRAMEWORK_HTTP");

    /**
     * Logger for method trace/timing events.
     */
    public static final Logger TRACE = LoggerFactory.getLogger("FRAMEWORK_TRACE");

    /**
     * Logger for unhandled exceptions.
     */
    public static final Logger EXCEPTION = LoggerFactory.getLogger("FRAMEWORK_EXCEPTION");

    /**
     * Logger for business events (@Loggable).
     */
    public static final Logger BUSINESS = LoggerFactory.getLogger("FRAMEWORK_BIZ");
}
