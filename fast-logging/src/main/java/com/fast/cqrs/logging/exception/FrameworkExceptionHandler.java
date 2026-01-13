package com.fast.cqrs.logging.exception;

import com.fast.cqrs.logging.FrameworkLoggers;
import com.fast.cqrs.logging.context.TraceContext;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Global exception handler that logs unhandled exceptions exactly once.
 * <p>
 * This is the single owner of exception logging in the framework.
 * No other layer should log exceptions.
 */
@ControllerAdvice
public class FrameworkExceptionHandler {

    /**
     * Handles all unhandled exceptions.
     * <p>
     * Logs the exception and returns a generic error response.
     */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, Object>> handleException(Throwable ex) {
        String traceId = TraceContext.getTraceId();

        // Log exception exactly once
        FrameworkLoggers.EXCEPTION.error("Unhandled exception [traceId={}]: {}", 
            traceId, ex.getMessage(), ex);

        // Build error response
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        body.put("message", "An unexpected error occurred");
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Handles IllegalArgumentException as Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String traceId = TraceContext.getTraceId();

        FrameworkLoggers.EXCEPTION.warn("Bad request [traceId={}]: {}", traceId, ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", ex.getMessage());
        body.put("traceId", traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
