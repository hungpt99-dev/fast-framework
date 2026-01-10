package com.fast.cqrs.logging.autoconfigure;

import com.fast.cqrs.logging.aspect.LoggableAspect;
import com.fast.cqrs.logging.aspect.TraceLogAspect;
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
}
