package com.fast.cqrs.cqrs;

/**
 * Exception thrown when CQRS dispatch rules are violated.
 * <p>
 * This exception indicates a programming error such as:
 * <ul>
 *   <li>Missing @Query or @Command annotation</li>
 *   <li>Both @Query and @Command present on same method</li>
 *   <li>Invalid controller method signature</li>
 * </ul>
 */
public class CqrsDispatchException extends RuntimeException {

    /**
     * Creates a new CqrsDispatchException with the given message.
     *
     * @param message the error message
     */
    public CqrsDispatchException(String message) {
        super(message);
    }

    /**
     * Creates a new CqrsDispatchException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public CqrsDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
