package com.fast.cqrs.sql.autoconfigure;

import com.fast.cqrs.sql.annotation.SqlRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Scans for {@code @SqlRepository} interfaces and verifies APT-generated implementations exist.
 * <p>
 * This registrar does NOT create dynamic proxies. All repository implementations must be
 * generated at compile-time by the annotation processor. This design ensures:
 * <ul>
 *   <li>Zero reflection overhead at runtime</li>
 *   <li>Full GraalVM native-image compatibility</li>
 *   <li>Compile-time safety - missing implementations are detected early</li>
 * </ul>
 * <p>
 * The APT-generated implementations are picked up by Spring's component scanning
 * since they are annotated with {@code @Repository}.
 */
public class SqlRepositoryImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SqlRepositoryImportRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {

        Set<String> basePackages = getBasePackages(importingClassMetadata);
        log.info("Scanning for @SqlRepository interfaces in packages: {}", basePackages);

        ClassPathScanningCandidateComponentProvider scanner = createScanner();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition candidate : candidates) {
                verifyGeneratedImplementation(candidate);
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isInterface();
                }
            };

        scanner.addIncludeFilter(new AnnotationTypeFilter(SqlRepository.class));
        return scanner;
    }

    /**
     * Verifies that an APT-generated implementation exists for the repository interface.
     * <p>
     * The generated class is registered via Spring's component scanning (@Repository),
     * so we don't need to manually register it here. We just verify it exists.
     */
    private void verifyGeneratedImplementation(BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (!StringUtils.hasText(className)) {
            return;
        }

        String generatedClassName = className + "_FastImpl";
        try {
            Class<?> generatedClass = ClassUtils.forName(generatedClassName, getClass().getClassLoader());
            log.debug("Found APT-generated repository: {}", generatedClass.getSimpleName());
        } catch (ClassNotFoundException e) {
            // No generated implementation found - this is an error in AOT-only mode
            log.error("No APT-generated implementation found for @SqlRepository: {}. " +
                      "Ensure the fast-processor annotation processor is configured in your build. " +
                      "Dynamic proxy fallback is disabled for GraalVM native-image compatibility.", 
                      className);
            throw new IllegalStateException(
                "Missing APT-generated implementation for " + className + "_FastImpl. " +
                "Add fast-processor as an annotation processor to your build configuration.");
        }
    }

    private Set<String> getBasePackages(AnnotationMetadata metadata) {
        Set<String> basePackages = new HashSet<>();
        AnnotationAttributes attributes = null;

        // Try getting attributes from @EnableFast
        if (metadata.hasAnnotation("com.fast.cqrs.autoconfigure.EnableFast")) {
            attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes("com.fast.cqrs.autoconfigure.EnableFast")
            );
        } else if (metadata.hasAnnotation(EnableSqlRepositories.class.getName())) {
            attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableSqlRepositories.class.getName())
            );
        }

        if (attributes != null) {
            String[] packages = attributes.getStringArray("basePackages");
            basePackages.addAll(Arrays.asList(packages));

            if (attributes.containsKey("basePackageClasses")) {
                Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
                for (Class<?> clazz : basePackageClasses) {
                    basePackages.add(ClassUtils.getPackageName(clazz));
                }
            }
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }

        return basePackages;
    }
}
