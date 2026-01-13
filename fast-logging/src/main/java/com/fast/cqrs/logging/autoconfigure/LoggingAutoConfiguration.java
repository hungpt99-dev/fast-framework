package com.fast.cqrs.logging.autoconfigure;

import com.fast.cqrs.logging.aspect.LoggableAspect;
import com.fast.cqrs.logging.aspect.TraceLogAspect;
import com.fast.cqrs.logging.audit.AuditLogger;
import com.fast.cqrs.logging.audit.Slf4jAuditLogger;
import com.fast.cqrs.logging.exception.FrameworkExceptionHandler;
import com.fast.cqrs.logging.filter.TraceIdFilter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.Ordered;

/**
 * Auto-configuration for the logging framework.
 * <p>
 * Provides default beans for tracing, logging aspects, exception handling,
 * and audit logging.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@EnableAspectJAutoProxy
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TraceIdFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setName("traceIdFilter");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean
    public TraceLogAspect traceLogAspect() {
        return new TraceLogAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public LoggableAspect loggableAspect() {
        return new LoggableAspect();
    }

    @Bean
    @ConditionalOnMissingBean
    public FrameworkExceptionHandler frameworkExceptionHandler() {
        return new FrameworkExceptionHandler();
    }

    /**
     * Default audit logger using SLF4J.
     * <p>
     * Users can provide their own AuditLogger bean (e.g., database-backed)
     * to override this default.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditLogger auditLogger() {
        return new Slf4jAuditLogger();
    }
}

