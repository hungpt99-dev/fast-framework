package com.fast.cqrs.logging.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SLF4J-based implementation of {@link AuditLogger}.
 * <p>
 * Writes audit events to a dedicated audit logger category ("AUDIT").
 * Events are logged in a structured format suitable for parsing by
 * log aggregation tools.
 */
public class Slf4jAuditLogger implements AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    @Override
    public void log(AuditEvent event) {
        String message = formatEvent(event);
        
        if (event.success()) {
            auditLog.info(message);
        } else {
            auditLog.warn(message);
        }
    }

    private String formatEvent(AuditEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("action=").append(event.action());
        sb.append(" actor=").append(event.actor() != null ? event.actor() : "SYSTEM");
        
        if (event.resource() != null) {
            sb.append(" resource=").append(event.resource());
        }
        if (event.resourceId() != null) {
            sb.append(" resourceId=").append(event.resourceId());
        }
        if (event.traceId() != null) {
            sb.append(" traceId=").append(event.traceId());
        }
        
        sb.append(" success=").append(event.success());
        sb.append(" timestamp=").append(event.timestamp());
        
        return sb.toString();
    }
}
