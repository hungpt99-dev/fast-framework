package com.fast.cqrs.concurrent.spring;

import com.fast.cqrs.concurrent.resilience.RejectPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Fast Concurrent.
 */
@ConfigurationProperties(prefix = "fast.concurrent.defaults")
public class FastConcurrentProperties {

    /**
     * Default number of permits for @ConcurrentSafe or unspecified limits.
     */
    private int permits = 10;

    /**
     * Default wait timeout in milliseconds.
     */
    private long waitTimeoutMs = 200;

    /**
     * Default rejection policy.
     */
    private RejectPolicy rejectPolicy = RejectPolicy.WAIT_TIMEOUT;

    public int getPermits() {
        return permits;
    }

    public void setPermits(int permits) {
        this.permits = permits;
    }

    public long getWaitTimeoutMs() {
        return waitTimeoutMs;
    }

    public void setWaitTimeoutMs(long waitTimeoutMs) {
        this.waitTimeoutMs = waitTimeoutMs;
    }

    public RejectPolicy getRejectPolicy() {
        return rejectPolicy;
    }

    private final ExecutorConfig io = new ExecutorConfig();
    private final ExecutorConfig cpu = new ExecutorConfig();

    public ExecutorConfig getIo() {
        return io;
    }

    public ExecutorConfig getCpu() {
        return cpu;
    }

    public static class ExecutorConfig {
        private int coreSize = 10;
        private int maxSize = 100;
        private int queueCapacity = 1000;
        private int keepAliveSeconds = 60;

        public int getCoreSize() { return coreSize; }
        public void setCoreSize(int coreSize) { this.coreSize = coreSize; }

        public int getMaxSize() { return maxSize; }
        public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        
        public int getKeepAliveSeconds() { return keepAliveSeconds; }
        public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
    }
}
