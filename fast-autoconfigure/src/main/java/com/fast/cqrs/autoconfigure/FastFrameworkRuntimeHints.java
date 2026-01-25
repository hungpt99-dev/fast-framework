package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.sql.mapper.ResultMapper;
import com.fast.cqrs.sql.repository.CrudExecutor;
import com.fast.cqrs.sql.repository.EntityMetadata;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM Native Image runtime hints for Fast Framework.
 * <p>
 * Registers reflection metadata for classes that use runtime reflection.
 * This is primarily for the runtime fallback path - the annotation processor
 * generates zero-reflection implementations at compile time for optimal
 * native image performance.
 * <p>
 * <b>Recommended:</b> Use the annotation processor (fast-processor) for
 * GraalVM native images to avoid reflection entirely.
 *
 * @see org.springframework.aot.hint.RuntimeHintsRegistrar
 */
public class FastFrameworkRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // SQL module reflection hints
        registerSqlModuleHints(hints);
        
        // Concurrent module reflection hints
        registerConcurrentModuleHints(hints);
    }

    private void registerSqlModuleHints(RuntimeHints hints) {
        // EntityMetadata uses reflection for field access
        // Spring Framework 7: INVOKE_* categories are the non-deprecated ones
        hints.reflection().registerType(EntityMetadata.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        hints.reflection().registerType(EntityMetadata.ColumnInfo.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        // CrudExecutor uses BeanPropertyRowMapper (reflection-based)
        hints.reflection().registerType(CrudExecutor.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        // ResultMapper uses BeanPropertyRowMapper
        hints.reflection().registerType(ResultMapper.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_DECLARED_METHODS);

        // Spring JDBC reflection-based mappers
        try {
            Class<?> beanPropertyRowMapper = Class.forName(
                    "org.springframework.jdbc.core.BeanPropertyRowMapper");
            hints.reflection().registerType(beanPropertyRowMapper,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);

            Class<?> beanPropertySqlParameterSource = Class.forName(
                    "org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource");
            hints.reflection().registerType(beanPropertySqlParameterSource,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        } catch (ClassNotFoundException e) {
            // Spring JDBC not on classpath, skip
        }
    }

    private void registerConcurrentModuleHints(RuntimeHints hints) {
        // ConcurrencyAspect uses reflection for fallback method invocation
        try {
            Class<?> concurrencyAspect = Class.forName(
                    "com.fast.cqrs.concurrent.spring.ConcurrencyAspect");
            hints.reflection().registerType(concurrencyAspect,
                    MemberCategory.INVOKE_DECLARED_METHODS);
        } catch (ClassNotFoundException e) {
            // Concurrent module not on classpath, skip
        }
    }
}
