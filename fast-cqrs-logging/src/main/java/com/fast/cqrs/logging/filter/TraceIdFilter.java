package com.fast.cqrs.logging.filter;

import com.fast.cqrs.logging.FrameworkLoggers;
import com.fast.cqrs.logging.context.TraceContext;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet filter that manages trace context per request.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Generate or extract trace ID</li>
 *   <li>Store trace ID in MDC</li>
 *   <li>Log HTTP request lifecycle</li>
 *   <li>Clear MDC after request completion</li>
 * </ul>
 */
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        long startTime = System.currentTimeMillis();

        try {
            // Extract or generate trace ID
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.isBlank()) {
                traceId = TraceContext.generateTraceId();
            } else {
                TraceContext.setTraceId(traceId);
            }

            // Set request path in MDC
            TraceContext.setRequestPath(httpRequest.getRequestURI());

            // Add trace ID to response header
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            // Continue filter chain
            chain.doFilter(request, response);

        } finally {
            // Log HTTP lifecycle
            long duration = System.currentTimeMillis() - startTime;
            logHttpRequest(httpRequest, httpResponse, duration);

            // Clear MDC
            TraceContext.clear();
        }
    }

    private void logHttpRequest(HttpServletRequest request, HttpServletResponse response, long duration) {
        FrameworkLoggers.HTTP.info("{} {} {} {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration
        );
    }
}
