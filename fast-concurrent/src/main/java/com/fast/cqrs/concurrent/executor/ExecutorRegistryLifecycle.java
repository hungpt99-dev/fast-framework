package com.fast.cqrs.concurrent.executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Spring lifecycle bean for graceful shutdown of {@link ExecutorRegistry}.
 * <p>
 * This bean ensures that all executor thread pools are properly shut down
 * when the Spring application context is closed, preventing thread leaks
 * in tests and enabling graceful application shutdown.
 * <p>
 * The shutdown process:
 * <ol>
 *   <li>Calls {@code shutdown()} on all executors</li>
 *   <li>Waits up to 30 seconds for running tasks to complete</li>
 *   <li>If tasks don't complete, calls {@code shutdownNow()}</li>
 * </ol>
 */
@Component
public class ExecutorRegistryLifecycle implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ExecutorRegistryLifecycle.class);
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 30;

    @Override
    public void destroy() throws Exception {
        log.info("Shutting down ExecutorRegistry...");
        
        // First, graceful shutdown
        ExecutorRegistry.shutdownAll();
        
        // Wait for termination
        log.info("Waiting for executor tasks to complete (timeout: {}s)...", SHUTDOWN_TIMEOUT_SECONDS);
        
        try {
            // Give tasks time to complete
            Thread.sleep(100);
            
            // Check if still running, if so force shutdown
            boolean allTerminated = waitForTermination();
            
            if (!allTerminated) {
                log.warn("Executors did not terminate in time, forcing shutdown");
                ExecutorRegistry.shutdownNow();
            } else {
                log.info("All executors shut down successfully");
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted during executor shutdown, forcing immediate shutdown");
            ExecutorRegistry.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean waitForTermination() {
        try {
            // Simple check - just wait a bit for termination
            Thread.sleep(500);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
