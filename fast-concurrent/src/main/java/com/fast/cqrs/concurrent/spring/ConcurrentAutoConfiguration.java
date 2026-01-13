package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.executor.ExecutorRegistry;
import com.fast.cqrs.concurrent.executor.ExecutorType;
import com.fast.cqrs.concurrent.metrics.TaskMetrics;
import com.fast.cqrs.concurrent.event.TaskEventListener;

import java.util.Map;

/**
 * Auto-configuration for Fast Concurrent framework.
 * <p>
 * Enables configuration via application.yml:
 * 
 * <pre>{@code
 * fast.concurrent:
 *   default-mode: virtual-thread
 *   context-propagation: true
 *   executors:
 *     db:
 *       type: io
 *       max-threads: 50
 *     compute:
 *       type: cpu
 * }</pre>
 */
public class ConcurrentAutoConfiguration {

    /**
     * Configures the concurrent framework from properties.
     */
    public static void configure(ConcurrentProperties properties) {
        // Configure executors
        if (properties.executors() != null) {
            for (Map.Entry<String, ExecutorProperties> entry : properties.executors().entrySet()) {
                String name = entry.getKey();
                ExecutorProperties props = entry.getValue();

                ExecutorType type = switch (props.type().toLowerCase()) {
                    case "virtual", "virtual-thread" -> ExecutorType.VIRTUAL;
                    case "cpu" -> ExecutorType.CPU;
                    case "io" -> ExecutorType.IO;
                    case "blocking" -> ExecutorType.BLOCKING;
                    default -> ExecutorType.VIRTUAL;
                };

                if (props.maxThreads() > 0) {
                    ExecutorRegistry.register(name, type, props.maxThreads());
                } else {
                    ExecutorRegistry.register(name, type);
                }
            }
        }
    }

    /**
     * Returns a default metrics listener bean.
     */
    public static TaskEventListener metricsListener() {
        return TaskMetrics.listener();
    }

    /**
     * Shutdown hook for graceful executor shutdown.
     */
    public static void shutdown() {
        ExecutorRegistry.shutdownAll();
    }

    /**
     * Properties for concurrent framework configuration.
     */
    public record ConcurrentProperties(
            String defaultMode,
            boolean contextPropagation,
            Map<String, ExecutorProperties> executors) {
        public ConcurrentProperties {
            if (defaultMode == null)
                defaultMode = "virtual-thread";
            if (executors == null)
                executors = Map.of();
        }
    }

    /**
     * Properties for executor configuration.
     */
    public record ExecutorProperties(
            String type,
            int maxThreads,
            int queueSize,
            int keepAlive) {
        public ExecutorProperties {
            if (type == null)
                type = "virtual";
            if (maxThreads <= 0)
                maxThreads = -1;
            if (queueSize <= 0)
                queueSize = 1000;
            if (keepAlive <= 0)
                keepAlive = 60;
        }
    }
}
