package com.fast.cqrs.cqrs.interceptor;

import com.fast.cqrs.cqrs.annotation.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interceptor that collects metrics based on {@link Metrics} annotation.
 */
public class MetricsInterceptor implements CommandInterceptor {

    private static final Logger log = LoggerFactory.getLogger(MetricsInterceptor.class);
    
    private final Map<String, MetricData> metrics = new ConcurrentHashMap<>();

    @Override
    public Object intercept(InterceptorContext context, InterceptorChain chain) throws Exception {
        Method method = context.getMethod();
        Metrics metricsAnnotation = method.getAnnotation(Metrics.class);
        
        if (metricsAnnotation == null) {
            return chain.proceed(context);
        }

        String metricName = metricsAnnotation.name().isEmpty() 
            ? method.getDeclaringClass().getSimpleName() + "." + method.getName()
            : metricsAnnotation.name();

        long startTime = System.currentTimeMillis();
        boolean success = true;
        
        try {
            return chain.proceed(context);
        } catch (Exception e) {
            success = false;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordMetric(metricName, duration, success);
        }
    }

    private void recordMetric(String name, long duration, boolean success) {
        MetricData data = metrics.computeIfAbsent(name, k -> new MetricData());
        data.record(duration, success);
        
        log.debug("Metric [{}]: count={}, avgTime={}ms, errorRate={}%", 
                  name, data.count.get(), data.getAverageTime(), data.getErrorRate());
    }

    /**
     * Gets metrics for a specific name.
     */
    public MetricData getMetrics(String name) {
        return metrics.get(name);
    }

    /**
     * Gets all collected metrics.
     */
    public Map<String, MetricData> getAllMetrics() {
        return Map.copyOf(metrics);
    }

    @Override
    public int getOrder() {
        return 50; // Run very early
    }

    /**
     * Aggregated metric data.
     */
    public static class MetricData {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = 0;

        void record(long duration, boolean success) {
            count.incrementAndGet();
            totalTime.addAndGet(duration);
            if (!success) errorCount.incrementAndGet();
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
        }

        public long getCount() { return count.get(); }
        public long getTotalTime() { return totalTime.get(); }
        public long getMinTime() { return minTime == Long.MAX_VALUE ? 0 : minTime; }
        public long getMaxTime() { return maxTime; }
        public long getErrorCount() { return errorCount.get(); }
        
        public double getAverageTime() {
            long c = count.get();
            return c > 0 ? (double) totalTime.get() / c : 0;
        }
        
        public double getErrorRate() {
            long c = count.get();
            return c > 0 ? (double) errorCount.get() / c * 100 : 0;
        }
    }
}
