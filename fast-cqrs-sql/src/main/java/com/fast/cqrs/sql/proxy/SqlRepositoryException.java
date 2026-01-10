package com.fast.cqrs.sql.proxy;

/**
 * Exception thrown for SQL repository configuration errors.
 */
public class SqlRepositoryException extends RuntimeException {

    public SqlRepositoryException(String message) {
        super(message);
    }

    public SqlRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
