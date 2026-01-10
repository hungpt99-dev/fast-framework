package com.fast.cqrs.modal.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as eligible for Modal/DTO conversion.
 * <p>
 * Classes annotated with {@code @Modal} can be converted to
 * {@code Map<String, Object>} using {@link com.fast.cqrs.modal.ModalMapper}.
 * Only fields annotated with {@link ModalField} will be included.
 * <p>
 * Example:
 * <pre>{@code
 * @Modal
 * public class User {
 *     @ModalField
 *     private String id;
 *
 *     @ModalField(name = "user_name")
 *     private String name;
 *
 *     @ModalField(ignore = true)
 *     private String password;
 * }
 * }</pre>
 *
 * @see ModalField
 * @see com.fast.cqrs.modal.ModalMapper
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Modal {

    /**
     * Optional alternative name for the modal.
     * <p>
     * This can be used for documentation or metadata purposes.
     *
     * @return the modal name, or empty string for default
     */
    String name() default "";
}
