package com.fast.cqrs.concurrent.executor;

import com.fast.cqrs.concurrent.spring.FastConcurrentProperties;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Spring-managed wrapper for {@link ExecutorRegistry}.
 * <p>
 * This bean provides:
 * <ul>
 *   <li>Instance-based access for dependency injection (testable)</li>
 *   <li>Proper lifecycle management via {@link DisposableBean}</li>
 *   <li>Integration with Spring's application context</li>
 * </ul>
 * <p>
 * For legacy/static usage, the underlying {@link ExecutorRegistry} static methods
 * remain available.
 * <p>
 * Example:
 * <pre>{@code
 * @Autowired
 * private ExecutorRegistryBean executorRegistry;
 * 
 * ExecutorService exec = executorRegistry.get("io");
 * }</pre>
 */
@Component
public class ExecutorRegistryBean implements DisposableBean {

    /**
     * Creates the bean and optionally configures from properties.
     */
    public ExecutorRegistryBean(@Nullable FastConcurrentProperties properties) {
        if (properties != null) {
            ExecutorRegistry.configure(properties);
        }
    }

    /**
     * Gets an executor by name.
     */
    public ExecutorService get(String name) {
        return ExecutorRegistry.get(name);
    }

    /**
     * Gets executor or default virtual threads.
     */
    public ExecutorService getOrDefault(String name) {
        return ExecutorRegistry.getOrDefault(name);
    }

    /**
     * Checks if executor exists.
     */
    public boolean exists(String name) {
        return ExecutorRegistry.exists(name);
    }

    /**
     * Registers an executor with auto-sizing.
     */
    public void register(String name, ExecutorType type) {
        ExecutorRegistry.register(name, type);
    }

    /**
     * Registers an executor with explicit size.
     */
    public void register(String name, ExecutorType type, int maxThreads) {
        ExecutorRegistry.register(name, type, maxThreads);
    }

    /**
     * Registers a custom executor.
     */
    public void register(String name, ExecutorService executor) {
        ExecutorRegistry.register(name, executor);
    }

    /**
     * Gets executor stats.
     */
    @Nullable
    public ExecutorRegistry.ExecutorStats stats(String name) {
        return ExecutorRegistry.stats(name);
    }

    @Override
    public void destroy() {
        ExecutorRegistry.shutdownAll();
        try {
            ExecutorRegistry.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            ExecutorRegistry.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
