package com.fast.cqrs.concurrent.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Centralized virtual thread executor manager.
 * <p>
 * Provides a shared virtual thread executor with proper lifecycle management.
 * Virtual thread executors are lightweight but this provides centralized
 * shutdown for graceful application termination.
 * <p>
 * Usage:
 * <pre>{@code
 * @Autowired
 * VirtualExecutorManager executorManager;
 * 
 * executorManager.getExecutor().submit(() -> {
 *     // Task runs on virtual thread
 * });
 * }</pre>
 */
@Component
public class VirtualExecutorManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(VirtualExecutorManager.class);

    private final ExecutorService executor;

    public VirtualExecutorManager() {
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        log.debug("Virtual thread executor manager initialized");
    }

    /**
     * Returns the shared virtual thread executor.
     */
    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Submits a task for execution.
     */
    public void submit(Runnable task) {
        executor.submit(task);
    }

    /**
     * Submits a task and returns a Future.
     */
    public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public void destroy() {
        log.debug("Shutting down virtual thread executor manager");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("Virtual thread executor did not terminate gracefully, forcing shutdown");
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("Virtual thread executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.debug("Virtual thread executor manager shutdown complete");
    }

    /**
     * Static accessor for non-Spring contexts.
     * <p>
     * Note: For Spring applications, prefer injecting VirtualExecutorManager.
     */
    private static final ExecutorService STATIC_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    static {
        // Register shutdown hook for static executor
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            STATIC_EXECUTOR.shutdown();
            try {
                STATIC_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                STATIC_EXECUTOR.shutdownNow();
            }
        }, "virtual-executor-shutdown"));
    }

    /**
     * Returns the static virtual thread executor for non-Spring contexts.
     */
    public static ExecutorService getStaticExecutor() {
        return STATIC_EXECUTOR;
    }
}
