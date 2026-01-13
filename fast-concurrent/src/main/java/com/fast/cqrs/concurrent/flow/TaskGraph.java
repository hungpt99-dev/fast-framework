package com.fast.cqrs.concurrent.flow;

import com.fast.cqrs.concurrent.context.ContextSnapshot;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Task graph with dependencies (DAG - Directed Acyclic Graph).
 * <p>
 * Executes tasks respecting dependencies and in optimal parallel order.
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * FlowResult result = TaskGraph.of()
 *         .task("user", () -> loadUser(userId))
 *         .task("orders", () -> loadOrders(userId))
 *         .task("profile", graph -> buildProfile(
 *                 graph.get("user"),
 *                 graph.get("orders")))
 *         .dependsOn("user", "orders")
 *         .execute();
 * }</pre>
 */
public class TaskGraph {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Map<String, TaskNode> nodes = new LinkedHashMap<>();
    private Duration timeout;
    private boolean propagateContext = true;

    private TaskGraph() {
    }

    public static TaskGraph of() {
        return new TaskGraph();
    }

    /**
     * Adds an independent task.
     */
    public <T> TaskGraph task(String name, Supplier<T> supplier) {
        nodes.put(name, new TaskNode(name, g -> supplier.get(), Set.of()));
        return this;
    }

    /**
     * Adds a dependent task.
     */
    public <T> DependencyBuilder task(String name, Function<TaskGraph.ResultAccessor, T> function) {
        return new DependencyBuilder(name, function);
    }

    /**
     * Sets timeout.
     */
    public TaskGraph timeout(Duration duration) {
        this.timeout = duration;
        return this;
    }

    /**
     * Sets timeout.
     */
    public TaskGraph timeout(long amount, TimeUnit unit) {
        this.timeout = Duration.of(amount, unit.toChronoUnit());
        return this;
    }

    /**
     * Executes the graph.
     */
    public FlowResult execute() {
        validateNoCycles();

        long startTime = System.nanoTime();
        ContextSnapshot snapshot = propagateContext ? ContextSnapshot.capture() : null;

        Map<String, Object> results = new ConcurrentHashMap<>();
        Map<String, Throwable> errors = new ConcurrentHashMap<>();
        Map<String, CompletableFuture<?>> futures = new HashMap<>();

        ResultAccessor accessor = new ResultAccessorImpl(results);

        // Create futures respecting dependencies
        for (TaskNode node : topologicalSort()) {
            CompletableFuture<?>[] deps = node.dependencies.stream()
                    .map(futures::get)
                    .toArray(CompletableFuture[]::new);

            CompletableFuture<?> future = CompletableFuture.allOf(deps)
                    .thenApplyAsync(v -> {
                        try {
                            if (snapshot != null)
                                snapshot.restore();
                            Object result = node.function.apply(accessor);
                            results.put(node.name, result);
                            return result;
                        } catch (Exception e) {
                            errors.put(node.name, e);
                            throw new CompletionException(e);
                        } finally {
                            if (snapshot != null)
                                snapshot.clear();
                        }
                    }, EXECUTOR);

            futures.put(node.name, future);
        }

        // Wait for all
        try {
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                    .get(timeout != null ? timeout.toMillis() : Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            futures.values().forEach(f -> f.cancel(true));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ignored) {
            // Errors captured in map
        }

        long duration = System.nanoTime() - startTime;
        return new FlowResult(results, errors, duration);
    }

    private List<TaskNode> topologicalSort() {
        List<TaskNode> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (String name : nodes.keySet()) {
            visit(name, visited, visiting, sorted);
        }

        return sorted;
    }

    private void visit(String name, Set<String> visited, Set<String> visiting, List<TaskNode> sorted) {
        if (visited.contains(name))
            return;
        if (visiting.contains(name)) {
            throw new IllegalStateException("Cycle detected in task graph at: " + name);
        }

        visiting.add(name);
        TaskNode node = nodes.get(name);
        for (String dep : node.dependencies) {
            visit(dep, visited, visiting, sorted);
        }
        visiting.remove(name);
        visited.add(name);
        sorted.add(node);
    }

    private void validateNoCycles() {
        topologicalSort(); // Will throw if cycle exists
    }

    /**
     * Builder for adding dependencies.
     */
    public class DependencyBuilder {
        private final String name;
        private final Function<ResultAccessor, ?> function;

        DependencyBuilder(String name, Function<ResultAccessor, ?> function) {
            this.name = name;
            this.function = function;
        }

        public TaskGraph dependsOn(String... dependencies) {
            nodes.put(name, new TaskNode(name, function, Set.of(dependencies)));
            return TaskGraph.this;
        }
    }

    /**
     * Accessor for accessing completed task results.
     */
    public interface ResultAccessor {
        <T> T get(String taskName);
    }

    private record TaskNode(
            String name,
            Function<ResultAccessor, ?> function,
            Set<String> dependencies) {
    }

    private record ResultAccessorImpl(Map<String, Object> results) implements ResultAccessor {
        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String taskName) {
            return (T) results.get(taskName);
        }
    }
}
