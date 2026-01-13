package com.fast.cqrs.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interceptor for validating method arguments annotated with {@code @Valid}.
 * <p>
 * This interceptor uses Bean Validation (Jakarta Validation) to validate
 * objects before they are processed by handlers.
 * <p>
 * Validation is optional - if jakarta.validation is not on the classpath,
 * this interceptor is effectively a no-op.
 *
 * @see Valid
 */
public class ValidationInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ValidationInterceptor.class);

    private static final boolean VALIDATION_AVAILABLE;
    private static final Validator VALIDATOR;

    static {
        boolean available = false;
        Validator validator = null;
        try {
            Class.forName("jakarta.validation.Validation");
            ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
            validator = factory.getValidator();
            available = true;
        } catch (Exception e) {
            // Bean Validation not available
        }
        VALIDATION_AVAILABLE = available;
        VALIDATOR = validator;
    }

    /**
     * Checks if Bean Validation is available on the classpath.
     *
     * @return true if Bean Validation is available
     */
    public static boolean isValidationAvailable() {
        return VALIDATION_AVAILABLE;
    }

    /**
     * Validates method arguments that are annotated with {@code @Valid}.
     * <p>
     * If validation fails, a {@link ValidationException} is thrown containing
     * all constraint violations.
     *
     * @param method the method being invoked
     * @param args   the method arguments
     * @throws ValidationException if any validation constraint is violated
     */
    public static void validate(Method method, Object[] args) {
        if (!VALIDATION_AVAILABLE || args == null || args.length == 0) {
            return;
        }

        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            if (i >= args.length || args[i] == null) {
                continue;
            }

            // Check if parameter has @Valid annotation
            if (hasValidAnnotation(parameters[i])) {
                validateObject(args[i], parameters[i].getName());
            }
        }
    }

    private static boolean hasValidAnnotation(Parameter parameter) {
        for (Annotation annotation : parameter.getAnnotations()) {
            if (annotation.annotationType().getName().endsWith(".Valid")) {
                return true;
            }
        }
        return false;
    }

    private static void validateObject(Object obj, String paramName) {
        Set<ConstraintViolation<Object>> violations = VALIDATOR.validate(obj);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining(", "));

            log.warn("Validation failed for {}: {}", paramName, message);
            throw new ValidationException("Validation failed: " + message, violations);
        }

        log.debug("Validation passed for {}", paramName);
    }
}
