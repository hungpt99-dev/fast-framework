package com.fast.cqrs.modal;

/**
 * Exception thrown when Modal mapping fails.
 */
public class ModalMappingException extends RuntimeException {

    /**
     * Creates a new exception with the given message.
     *
     * @param message the error message
     */
    public ModalMappingException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public ModalMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
