package com.fast.cqrs.cqrs.gateway;

/**
 * Exception thrown by {@link QueryGateway} operations.
 */
public class QueryGatewayException extends RuntimeException {
    
    public QueryGatewayException(String message) {
        super(message);
    }
    
    public QueryGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
