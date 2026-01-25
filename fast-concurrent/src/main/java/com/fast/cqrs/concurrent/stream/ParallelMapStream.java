package com.fast.cqrs.concurrent.stream;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Parallel map stream for chaining map operations.
 */
public class ParallelMapStream<T, R> {

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
