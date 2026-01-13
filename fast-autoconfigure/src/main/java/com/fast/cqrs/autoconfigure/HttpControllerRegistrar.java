package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.annotation.HttpController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
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
 * Scans for {@code @HttpController} interfaces and registers them as Spring beans.
 * <p>
 * Implements {@link ImportBeanDefinitionRegistrar} to read annotation metadata
 * from {@code @EnableFast} or {@code @EnableCqrs}.
 */
public class HttpControllerRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(HttpControllerRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {

        Set<String> basePackages = getBasePackages(importingClassMetadata);
        log.info("Scanning for @HttpController interfaces in packages: {}", basePackages);

        ClassPathScanningCandidateComponentProvider scanner = createScanner();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
            
            for (BeanDefinition candidate : candidates) {
                registerControllerProxy(registry, candidate);
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider createScanner() {
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    // Accept interfaces (not just concrete classes)
                    return beanDefinition.getMetadata().isInterface();
                }
            };

        scanner.addIncludeFilter(new AnnotationTypeFilter(HttpController.class));
        return scanner;
    }

    private void registerControllerProxy(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (!StringUtils.hasText(className)) {
            return;
        }

        try {
            Class<?> controllerInterface = ClassUtils.forName(
                className, 
                getClass().getClassLoader()
            );

            String beanName = generateBeanName(controllerInterface);
            int duplicateCount = 0;
            String originalBeanName = beanName;
            while (registry.containsBeanDefinition(beanName)) {
                duplicateCount++;
                beanName = originalBeanName + duplicateCount;
            }
            
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(ControllerProxyFactoryBean.class);
            beanDefinition.getConstructorArgumentValues()
                .addGenericArgumentValue(controllerInterface);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            beanDefinition.setDependsOn("controllerProxyFactory");

            registry.registerBeanDefinition(beanName, beanDefinition);
            log.debug("Registered controller proxy bean: {} for interface: {}", 
                     beanName, className);

        } catch (ClassNotFoundException e) {
            log.error("Failed to load controller interface: {}", className, e);
        }
    }

    private String generateBeanName(Class<?> controllerInterface) {
        String simpleName = controllerInterface.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    private Set<String> getBasePackages(AnnotationMetadata metadata) {
        Set<String> basePackages = new HashSet<>();

        // Try getting attributes from @EnableFast
        AnnotationAttributes attributes = null;
        if (metadata.hasAnnotation(EnableFast.class.getName())) {
            attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableFast.class.getName())
            );
        } else if (metadata.hasAnnotation(EnableCqrs.class.getName())) {
            attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(EnableCqrs.class.getName())
            );
        }

        if (attributes != null) {
            String[] packages = attributes.getStringArray("basePackages");
            basePackages.addAll(Arrays.asList(packages));
            
            // Handle basePackageClasses if present (EnableCqrs has it, EnableFast currently doesn't)
            if (attributes.containsKey("basePackageClasses")) {
                 Class<?>[] classes = attributes.getClassArray("basePackageClasses");
                 for (Class<?> clazz : classes) {
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
