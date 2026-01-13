package com.fast.cqrs.web;

import jakarta.validation.ConstraintViolation;

import java.util.Set;

/**
 * Exception thrown when validation constraints are violated.
 * <p>
 * Contains the set of constraint violations that caused the failure.
 */
public class ValidationException extends RuntimeException {

    private final Set<? extends ConstraintViolation<?>> violations;

    /**
     * Creates a new validation exception.
     *
     * @param message    the error message
     * @param violations the constraint violations
     */
    public ValidationException(String message, Set<? extends ConstraintViolation<?>> violations) {
        super(message);
        this.violations = violations;
    }

    /**
     * Gets the constraint violations that caused this exception.
     *
     * @return the set of constraint violations
     */
    public Set<? extends ConstraintViolation<?>> getViolations() {
        return violations;
    }
}
