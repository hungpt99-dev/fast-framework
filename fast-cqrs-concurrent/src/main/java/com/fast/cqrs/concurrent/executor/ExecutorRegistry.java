package com.fast.cqrs.concurrent.executor;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Central registry for named executors.
 * <p>
 * Features:
 * <ul>
 * <li>Named executor management</li>
 * <li>CPU/IO/Blocking separation</li>
 * <li>Auto-sizing based on workload type</li>
 * <li>Graceful shutdown</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * ExecutorRegistry.register("db-io", ExecutorType.IO, 50);
 * ExecutorRegistry.register("compute", ExecutorType.CPU);
 * 
 * ExecutorService dbExec = ExecutorRegistry.get("db-io");
 * }</pre>
 */
public final class ExecutorRegistry {

    private static final Map<String, ManagedExecutor> executors = new ConcurrentHashMap<>();

    // Built-in executors
    public static final String VIRTUAL = "virtual";
    public static final String CPU = "cpu";
    public static final String IO = "io";

    static {
        // Register defaults
        register(VIRTUAL, ExecutorType.VIRTUAL);
        register(CPU, ExecutorType.CPU);
        register(IO, ExecutorType.IO, 100);
    }

    private ExecutorRegistry() {
    }

    /**
     * Registers an executor with auto-sizing.
     */
    public static void register(String name, ExecutorType type) {
        register(name, type, defaultSize(type));
    }

    /**
     * Registers an executor with explicit size.
     */
    public static void register(String name, ExecutorType type, int maxThreads) {
        ExecutorService executor = createExecutor(type, maxThreads);
        executors.put(name, new ManagedExecutor(name, type, executor, maxThreads));
    }

    /**
     * Registers a custom executor.
     */
    public static void register(String name, ExecutorService executor) {
        executors.put(name, new ManagedExecutor(name, ExecutorType.CUSTOM, executor, -1));
    }

    /**
     * Gets an executor by name.
     */
    public static ExecutorService get(String name) {
        ManagedExecutor managed = executors.get(name);
        if (managed == null) {
            throw new IllegalArgumentException("Unknown executor: " + name);
        }
        return managed.executor;
    }

    /**
     * Gets executor or default virtual threads.
     */
    public static ExecutorService getOrDefault(String name) {
        ManagedExecutor managed = executors.get(name);
        return managed != null ? managed.executor : get(VIRTUAL);
    }

    /**
     * Checks if executor exists.
     */
    public static boolean exists(String name) {
        return executors.containsKey(name);
    }

    /**
     * Gets executor stats.
     */
    public static ExecutorStats stats(String name) {
        ManagedExecutor managed = executors.get(name);
        if (managed == null)
            return null;

        if (managed.executor instanceof ThreadPoolExecutor tpe) {
            return new ExecutorStats(
                    name,
                    managed.type,
                    tpe.getActiveCount(),
                    tpe.getPoolSize(),
                    managed.maxThreads,
                    tpe.getQueue().size(),
                    tpe.getCompletedTaskCount());
        }
        return new ExecutorStats(name, managed.type, -1, -1, -1, -1, -1);
    }

    /**
     * Shuts down all executors gracefully.
     */
    public static void shutdownAll() {
        for (ManagedExecutor managed : executors.values()) {
            managed.executor.shutdown();
        }
    }

    /**
     * Shuts down all executors immediately.
     */
    public static void shutdownNow() {
        for (ManagedExecutor managed : executors.values()) {
            managed.executor.shutdownNow();
        }
    }

    private static ExecutorService createExecutor(ExecutorType type, int maxThreads) {
        return switch (type) {
            case VIRTUAL -> Executors.newVirtualThreadPerTaskExecutor();
            case CPU -> new ThreadPoolExecutor(
                    Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(1000),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            case IO, BLOCKING -> new ThreadPoolExecutor(
                    10, maxThreads,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(maxThreads * 2),
                    new ThreadPoolExecutor.CallerRunsPolicy());
            case CUSTOM -> throw new IllegalArgumentException("Use register(name, executor) for custom");
        };
    }

    private static int defaultSize(ExecutorType type) {
        return switch (type) {
            case VIRTUAL -> Integer.MAX_VALUE;
            case CPU -> Runtime.getRuntime().availableProcessors();
            case IO, BLOCKING -> 100;
            case CUSTOM -> -1;
        };
    }

    private record ManagedExecutor(
            String name,
            ExecutorType type,
            ExecutorService executor,
            int maxThreads) {
    }

    public record ExecutorStats(
            String name,
            ExecutorType type,
            int activeThreads,
            int poolSize,
            int maxPoolSize,
            int queueSize,
            long completedTasks) {
    }
}
