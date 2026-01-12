package com.fast.cqrs.concurrent.stream;

import com.fast.cqrs.concurrent.context.ContextSnapshot;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Service-safe parallel stream with bounded concurrency and per-item controls.
 * <p>
 * Features:
 * <ul>
 * <li>Bounded parallel execution</li>
 * <li>Per-item timeout and retry</li>
 * <li>Backpressure support</li>
 * <li>Ordered and unordered execution</li>
 * <li>Context propagation</li>
 * <li>Partial result collection</li>
 * </ul>
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * List<EnrichedUser> enriched = ParallelStream.from(users)
 *         .parallel(10)
 *         .map(this::enrichUser)
 *         .timeoutPerItem(200, TimeUnit.MILLISECONDS)
 *         .retryPerItem(2)
 *         .collect();
 * }</pre>
 */
public class ParallelStream<T> {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final List<T> source;
    private int parallelism = Runtime.getRuntime().availableProcessors();
    private Duration itemTimeout;
    private int itemRetries = 0;
    private Duration itemRetryDelay = Duration.ofMillis(100);
    private boolean ordered = true;
    private boolean propagateContext = true;
    private boolean skipOnError = false;
    private Consumer<Throwable> errorHandler;

    private ParallelStream(List<T> source) {
        this.source = new ArrayList<>(source);
    }

    /**
     * Creates a parallel stream from a collection.
     */
    public static <T> ParallelStream<T> from(Collection<T> source) {
        return new ParallelStream<>(new ArrayList<>(source));
    }

    /**
     * Creates a parallel stream from varargs.
     */
    @SafeVarargs
    public static <T> ParallelStream<T> of(T... elements) {
        return new ParallelStream<>(Arrays.asList(elements));
    }

    /**
     * Sets parallelism level.
     */
    public ParallelStream<T> parallel(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    /**
     * Sets per-item timeout.
     */
    public ParallelStream<T> timeoutPerItem(long amount, TimeUnit unit) {
        this.itemTimeout = Duration.of(amount, unit.toChronoUnit());
        return this;
    }

    /**
     * Sets per-item timeout.
     */
    public ParallelStream<T> timeoutPerItem(Duration duration) {
        this.itemTimeout = duration;
        return this;
    }

    /**
     * Sets per-item retry count.
     */
    public ParallelStream<T> retryPerItem(int retries) {
        this.itemRetries = retries;
        return this;
    }

    /**
     * Sets per-item retry with delay.
     */
    public ParallelStream<T> retryPerItem(int retries, Duration delay) {
        this.itemRetries = retries;
        this.itemRetryDelay = delay;
        return this;
    }

    /**
     * Enables ordered results (matches input order).
     */
    public ParallelStream<T> ordered() {
        this.ordered = true;
        return this;
    }

    /**
     * Enables unordered results (faster).
     */
    public ParallelStream<T> unordered() {
        this.ordered = false;
        return this;
    }

    /**
     * Skips items that fail (instead of failing the whole stream).
     */
    public ParallelStream<T> skipOnError() {
        this.skipOnError = true;
        return this;
    }

    /**
     * Sets error handler for failed items.
     */
    public ParallelStream<T> onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * Enables context propagation.
     */
    public ParallelStream<T> propagateContext(boolean propagate) {
        this.propagateContext = propagate;
        return this;
    }

    /**
     * Maps elements in parallel.
     */
    public <R> ParallelMapStream<T, R> map(Function<T, R> mapper) {
        return new ParallelMapStream<>(this, mapper);
    }

    /**
     * Filters elements in parallel.
     */
    public ParallelStream<T> filter(Predicate<T> predicate) {
        List<T> filtered = processParallel(source, item -> predicate.test(item) ? item : null)
                .stream()
                .filter(Objects::nonNull)
                .toList();
        return new ParallelStream<>(filtered);
    }

    /**
     * Processes each element (side effect).
     */
    public void forEach(Consumer<T> action) {
        processParallel(source, item -> {
            action.accept(item);
            return null;
        });
    }

    /**
     * Collects all elements.
     */
    public List<T> collect() {
        return new ArrayList<>(source);
    }

    <R> List<R> processParallel(List<T> items, Function<T, R> processor) {
        ContextSnapshot snapshot = propagateContext ? ContextSnapshot.capture() : null;
        Semaphore semaphore = new Semaphore(parallelism);

        Map<Integer, Future<R>> futures = new LinkedHashMap<>();

        for (int i = 0; i < items.size(); i++) {
            int index = i;
            T item = items.get(i);

            Future<R> future = EXECUTOR.submit(() -> {
                try {
                    semaphore.acquire();
                    if (snapshot != null)
                        snapshot.restore();

                    return processWithRetry(item, processor);
                } finally {
                    semaphore.release();
                    if (snapshot != null)
                        snapshot.clear();
                }
            });

            futures.put(index, future);
        }

        // Collect results
        List<R> results = ordered ? new ArrayList<>(Collections.nCopies(items.size(), null)) : new ArrayList<>();

        for (Map.Entry<Integer, Future<R>> entry : futures.entrySet()) {
            try {
                R result = itemTimeout != null
                        ? entry.getValue().get(itemTimeout.toMillis(), TimeUnit.MILLISECONDS)
                        : entry.getValue().get();

                if (ordered) {
                    results.set(entry.getKey(), result);
                } else {
                    results.add(result);
                }
            } catch (Exception e) {
                if (errorHandler != null)
                    errorHandler.accept(e);
                if (!skipOnError) {
                    throw new RuntimeException("Parallel stream processing failed at index " + entry.getKey(), e);
                }
            }
        }

        if (ordered) {
            results.removeIf(Objects::isNull);
        }

        return results;
    }

    private <R> R processWithRetry(T item, Function<T, R> processor) {
        Exception lastException = null;

        for (int attempt = 0; attempt <= itemRetries; attempt++) {
            try {
                return processor.apply(item);
            } catch (Exception e) {
                lastException = e;
                if (attempt < itemRetries) {
                    try {
                        Thread.sleep(itemRetryDelay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new RuntimeException("Processing failed after " + (itemRetries + 1) + " attempts", lastException);
    }

    // Getters for ParallelMapStream
    int getParallelism() {
        return parallelism;
    }

    Duration getItemTimeout() {
        return itemTimeout;
    }

    int getItemRetries() {
        return itemRetries;
    }

    Duration getItemRetryDelay() {
        return itemRetryDelay;
    }

    boolean isOrdered() {
        return ordered;
    }

    boolean isPropagateContext() {
        return propagateContext;
    }

    boolean isSkipOnError() {
        return skipOnError;
    }

    Consumer<Throwable> getErrorHandler() {
        return errorHandler;
    }
}

/**
 * Parallel map stream for chaining map operations.
 */
class ParallelMapStream<T, R> {

    private final ParallelStream<T> parent;
    private final Function<T, R> mapper;

    ParallelMapStream(ParallelStream<T> parent, Function<T, R> mapper) {
        this.parent = parent;
        this.mapper = mapper;
    }

    /**
     * Collects mapped results.
     */
    public List<R> collect() {
        return parent.processParallel(parent.collect(), mapper);
    }

    /**
     * Chains another map operation.
     */
    public <R2> ParallelMapStream<T, R2> map(Function<R, R2> nextMapper) {
        return new ParallelMapStream<>(parent, mapper.andThen(nextMapper));
    }

    /**
     * Filters mapped results.
     */
    public List<R> filter(Predicate<R> predicate) {
        return collect().stream().filter(predicate).toList();
    }

    /**
     * Reduces mapped results.
     */
    public Optional<R> reduce(BinaryOperator<R> accumulator) {
        return collect().stream().reduce(accumulator);
    }
}
