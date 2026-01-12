package com.fast.cqrs.concurrent.task;

/**
 * Exception thrown when task execution fails.
 */
public class TaskExecutionException extends RuntimeException {

    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
