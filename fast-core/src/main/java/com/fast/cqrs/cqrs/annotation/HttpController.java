package com.fast.cqrs.cqrs.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as an HTTP controller in the CQRS framework.
 * <p>
 * This annotation replaces {@code @RestController} and is specifically designed
 * for interface-only controller definitions. Controllers annotated with this
 * annotation will be implemented at runtime using dynamic proxies.
 * <p>
 * Example:
 * <pre>{@code
 * @HttpController
 * @RequestMapping("/orders")
 * public interface OrderController {
 *
 *     @Query
 *     @GetMapping("/{id}")
 *     OrderDto get(@PathVariable String id);
 *
 *     @Command
 *     @PostMapping
 *     void create(@RequestBody CreateOrderCmd cmd);
 * }
 * }</pre>
 *
 * @see Query
 * @see Command
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface HttpController {

    /**
     * The value may indicate a suggestion for a logical component name,
     * to be turned into a Spring bean in case of an autodetected component.
     *
     * @return the suggested component name, if any (or empty String otherwise)
     */
    @AliasFor(annotation = Controller.class)
    String value() default "";
}
