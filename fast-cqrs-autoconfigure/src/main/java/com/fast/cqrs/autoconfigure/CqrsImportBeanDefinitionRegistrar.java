package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.annotation.HttpController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Registrar that imports the {@link HttpControllerRegistrar} with the configured packages.
 */
public class CqrsImportBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(CqrsImportBeanDefinitionRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, 
                                        BeanDefinitionRegistry registry) {
        
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
            importingClassMetadata.getAnnotationAttributes(EnableCqrs.class.getName())
        );

        if (attributes == null) {
            return;
        }

        Set<String> basePackages = getBasePackages(importingClassMetadata, attributes);
        
        log.info("Configuring CQRS framework with base packages: {}", basePackages);

        // Register the HttpControllerRegistrar as a bean
        HttpControllerRegistrar registrar = new HttpControllerRegistrar(basePackages);
        registrar.postProcessBeanDefinitionRegistry(registry);
    }

    private Set<String> getBasePackages(AnnotationMetadata metadata, AnnotationAttributes attributes) {
        Set<String> basePackages = new HashSet<>();

        // Add explicitly defined packages
        String[] packages = attributes.getStringArray("basePackages");
        basePackages.addAll(Arrays.asList(packages));

        // Add packages from basePackageClasses
        Class<?>[] basePackageClasses = attributes.getClassArray("basePackageClasses");
        for (Class<?> clazz : basePackageClasses) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        // If no packages specified, use the package of the annotated class
        if (basePackages.isEmpty()) {
            String className = metadata.getClassName();
            basePackages.add(ClassUtils.getPackageName(className));
        }

        return basePackages;
    }
}
