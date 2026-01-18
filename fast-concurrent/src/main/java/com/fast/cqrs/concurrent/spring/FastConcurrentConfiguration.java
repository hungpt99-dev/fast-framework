package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.executor.ExecutorRegistry;
import com.fast.cqrs.concurrent.resilience.ConcurrencyRegistry;
import com.fast.cqrs.concurrent.resilience.KeyedSemaphoreRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FastConcurrentProperties.class)
public class FastConcurrentConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConcurrencyRegistry concurrencyRegistry(FastConcurrentProperties properties, ApplicationContext ctx) {
        // Configure executor pools
        ExecutorRegistry.configure(properties);

        MeterRegistry meterRegistry = ctx.getBeanProvider(MeterRegistry.class).getIfAvailable();
        return new ConcurrencyRegistry(properties, meterRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    public KeyedSemaphoreRegistry keyedSemaphoreRegistry(ApplicationContext ctx) {
        MeterRegistry meterRegistry = ctx.getBeanProvider(MeterRegistry.class).getIfAvailable();
        return new KeyedSemaphoreRegistry(meterRegistry);
    }

    @Bean
    public ConcurrencyAspect concurrencyAspect(ConcurrencyRegistry concurrencyRegistry,
                                               KeyedSemaphoreRegistry keyedSemaphoreRegistry,
                                               FastConcurrentProperties properties,
                                               ApplicationContext applicationContext) {
        return new ConcurrencyAspect(concurrencyRegistry, keyedSemaphoreRegistry, properties, applicationContext);
    }

    @Bean
    public ExecutionTimeoutAspect executionTimeoutAspect() {
        return new ExecutionTimeoutAspect();
    }

    @Bean
    public UseExecutorAspect useExecutorAspect() {
        return new UseExecutorAspect();
    }
}
