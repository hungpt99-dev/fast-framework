package com.fast.cqrs.modal.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Controls field-level exposure in Modal/DTO conversion.
 * <p>
 * Only fields annotated with {@code @ModalField} are included
 * in the conversion. Use the attributes to control behavior:
 * <ul>
 *   <li>{@code name} - rename the field in the output</li>
 *   <li>{@code ignore} - skip this field entirely</li>
 *   <li>{@code format} - apply formatting (for dates/numbers)</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * @ModalField(name = "user_name")
 * private String name;
 *
 * @ModalField(ignore = true)
 * private String password;
 *
 * @ModalField(format = "yyyy-MM-dd")
 * private LocalDate birthDate;
 * }</pre>
 *
 * @see Modal
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ModalField {

    /**
     * Optional name for the field in the modal output.
     * <p>
     * If empty, the original field name is used.
     *
     * @return the field name override
     */
    String name() default "";

    /**
     * Whether to ignore this field in conversion.
     * <p>
     * Use this for sensitive fields that should never be exposed.
     *
     * @return true to skip this field
     */
    boolean ignore() default false;

    /**
     * Optional format pattern for the field value.
     * <p>
     * Supported formats:
     * <ul>
     *   <li>Date patterns: "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"</li>
     *   <li>Number patterns: "#,###.##", "0.00"</li>
     * </ul>
     *
     * @return the format pattern, or empty for no formatting
     */
    String format() default "";
}
