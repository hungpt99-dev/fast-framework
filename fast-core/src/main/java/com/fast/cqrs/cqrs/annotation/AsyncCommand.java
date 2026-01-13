package com.fast.cqrs.cqrs.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a command for asynchronous (fire-and-forget) execution.
 * <p>
 * The command will be queued and executed in a separate thread.
 * The method returns immediately without waiting for completion.
 * <p>
 * Example:
 * <pre>{@code
 * @AsyncCommand
 * @Command
 * void sendEmailNotification(@RequestBody SendEmailCmd cmd);
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AsyncCommand {
    
    /**
     * Executor bean name to use.
     * Default: uses the default async executor.
     */
    String executor() default "";
}
