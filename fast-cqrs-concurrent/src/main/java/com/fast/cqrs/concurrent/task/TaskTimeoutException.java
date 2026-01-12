package com.fast.cqrs.concurrent.task;

/**
 * Exception thrown when a task times out.
 */
public class TaskTimeoutException extends RuntimeException {

    public TaskTimeoutException(String message) {
        super(message);
    }

    public TaskTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
