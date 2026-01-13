package com.fast.cqrs.web;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the context of an HTTP invocation.
 * <p>
 * This object captures all information about a controller method
 * invocation, including the method being called and its resolved arguments.
 * It is passed through the CQRS dispatch pipeline unchanged.
 *
 * @param method    the controller method being invoked
 * @param arguments the resolved method arguments
 */
public record HttpInvocationContext(
    Method method,
    Object[] arguments
) {
    /**
     * Creates a new invocation context.
     *
     * @param method    the controller method
     * @param arguments the method arguments
     */
    public HttpInvocationContext {
        Objects.requireNonNull(method, "Method cannot be null");
        arguments = arguments != null ? arguments.clone() : new Object[0];
    }

    /**
     * Returns a defensive copy of the arguments.
     *
     * @return copy of the arguments array
     */
    @Override
    public Object[] arguments() {
        return arguments.clone();
    }

    /**
     * Returns the first argument if present, useful for single-argument methods.
     *
     * @return the first argument, or null if no arguments
     */
    public Object firstArgument() {
        return arguments.length > 0 ? arguments[0] : null;
    }

    /**
     * Returns the argument at the specified index.
     *
     * @param index the argument index
     * @return the argument at the index
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public Object argumentAt(int index) {
        return arguments[index];
    }

    /**
     * Returns the number of arguments.
     *
     * @return the argument count
     */
    public int argumentCount() {
        return arguments.length;
    }

    @Override
    public String toString() {
        return "HttpInvocationContext{" +
               "method=" + method.getDeclaringClass().getSimpleName() + "." + method.getName() +
               ", arguments=" + Arrays.toString(arguments) +
               '}';
    }
}
