package com.fast.cqrs.cqrs.gateway;

/**
 * Exception thrown by {@link CommandGateway} operations.
 */
public class CommandGatewayException extends RuntimeException {
    
    public CommandGatewayException(String message) {
        super(message);
    }
    
    public CommandGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
