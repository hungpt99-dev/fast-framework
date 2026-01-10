package com.fast.cqrs.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result type for operations that can succeed or fail.
 * Alternative to throwing exceptions for expected failures.
 *
 * @param <T> the success value type
 */
public sealed interface Result<T> {

    /**
     * Creates a success result.
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a failure result.
     */
    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    /**
     * Creates a failure result with error code.
     */
    static <T> Result<T> failure(String code, String message) {
        return new Failure<>(code + ": " + message);
    }

    /**
     * Wraps an operation that might throw.
     */
    static <T> Result<T> of(ThrowingSupplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e.getMessage());
        }
    }

    boolean isSuccess();
    boolean isFailure();
    T getValue();
    String getError();

    /**
     * Gets the value or throws IllegalStateException.
     */
    default T getOrThrow() {
        if (isSuccess()) {
            return getValue();
        }
        throw new IllegalStateException(getError());
    }

    /**
     * Gets the value or default.
     */
    default T getOrElse(T defaultValue) {
        return isSuccess() ? getValue() : defaultValue;
    }

    /**
     * Maps the success value.
     */
    default <R> Result<R> map(Function<T, R> mapper) {
        if (isSuccess()) {
            return success(mapper.apply(getValue()));
        }
        return failure(getError());
    }

    /**
     * Flat maps the success value.
     */
    default <R> Result<R> flatMap(Function<T, Result<R>> mapper) {
        if (isSuccess()) {
            return mapper.apply(getValue());
        }
        return failure(getError());
    }

    /**
     * Executes action on success.
     */
    default Result<T> onSuccess(Consumer<T> action) {
        if (isSuccess()) {
            action.accept(getValue());
        }
        return this;
    }

    /**
     * Executes action on failure.
     */
    default Result<T> onFailure(Consumer<String> action) {
        if (isFailure()) {
            action.accept(getError());
        }
        return this;
    }

    /**
     * Converts to Optional.
     */
    default Optional<T> toOptional() {
        return isSuccess() ? Optional.ofNullable(getValue()) : Optional.empty();
    }

    record Success<T>(T value) implements Result<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public boolean isFailure() { return false; }
        @Override public T getValue() { return value; }
        @Override public String getError() { return null; }
    }

    record Failure<T>(String error) implements Result<T> {
        @Override public boolean isSuccess() { return false; }
        @Override public boolean isFailure() { return true; }
        @Override public T getValue() { return null; }
        @Override public String getError() { return error; }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
