package com.fast.cqrs.dx.convention;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates naming conventions at startup using ArchUnit.
 * <p>
 * Logs warnings for convention violations.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * @Autowired
 * NamingConventionValidator validator;
 * 
 * validator.validate("com.example");
 * if (validator.hasViolations()) {
 *     // Handle violations
 * }
 * }</pre>
 */
@Component
public class NamingConventionValidator {

    private static final Logger log = LoggerFactory.getLogger(NamingConventionValidator.class);

    private final List<String> violations = new ArrayList<>();
    private boolean strictMode = false;

    /**
     * Sets strict mode. When enabled, validation failures throw exceptions.
     */
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    /**
     * Validates all convention rules for the given base package.
     *
     * @param basePackage the base package to scan
     */
    public void validate(String basePackage) {
        log.info("Validating conventions for package: {}", basePackage);
        violations.clear();

        try {
            JavaClasses classes = new ClassFileImporter().importPackages(basePackage);

            // Validate using feature-based rules
            for (ArchRule rule : FastConventionRules.FEATURE_BASED_RULES) {
                validateRule(rule, classes);
            }

            if (violations.isEmpty()) {
                log.info("✅ All naming conventions are valid");
            } else {
                log.warn("⚠️ Found {} convention violations", violations.size());
                if (strictMode) {
                    throw new ConventionViolationException(
                            "Strict mode enabled. " + violations.size() + " convention violations found.");
                }
            }
        } catch (Exception e) {
            if (e instanceof ConventionViolationException) {
                throw (ConventionViolationException) e;
            }
            log.debug("Could not scan package {}: {}", basePackage, e.getMessage());
        }
    }

    private void validateRule(ArchRule rule, JavaClasses classes) {
        EvaluationResult result = rule.evaluate(classes);
        if (result.hasViolation()) {
            result.getFailureReport().getDetails().forEach(detail -> {
                violations.add(detail);
                log.warn("Convention violation: {}", detail);
            });
        }
    }

    /**
     * Gets all violations.
     */
    public List<String> getViolations() {
        return new ArrayList<>(violations);
    }

    /**
     * Returns true if there are violations.
     */
    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    /**
     * Clears all violations.
     */
    public void clear() {
        violations.clear();
    }

    /**
     * Exception thrown when conventions are violated in strict mode.
     */
    public static class ConventionViolationException extends RuntimeException {
        public ConventionViolationException(String message) {
            super(message);
        }
    }
}
