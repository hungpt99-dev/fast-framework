package com.fast.cqrs.sql.autoconfigure;


import com.fast.cqrs.sql.annotation.SqlRepository;

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
 * Registrar that scans for @SqlRepository interfaces and registers proxy beans.
 */
public class SqlRepositoryImportRegistrar implements ImportBeanDefinitionRegistrar {

    private static final Logger log = LoggerFactory.getLogger(SqlRepositoryImportRegistrar.class);

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
                                        BeanDefinitionRegistry registry) {
        System.out.println("DEBUG: SqlRepositoryImportRegistrar running for " + importingClassMetadata.getClassName());

        Set<String> basePackages = getBasePackages(importingClassMetadata);
        System.out.println("DEBUG: Base packages: " + basePackages);
        log.info("Scanning for @SqlRepository interfaces in packages: {}", basePackages);

        ClassPathScanningCandidateComponentProvider scanner = createScanner();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition candidate : candidates) {
                registerRepositoryProxy(registry, candidate);
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

    private void registerRepositoryProxy(BeanDefinitionRegistry registry, BeanDefinition candidate) {
        String className = candidate.getBeanClassName();
        if (!StringUtils.hasText(className)) {
            return;
        }

        try {
            Class<?> repositoryInterface = ClassUtils.forName(className, getClass().getClassLoader());
            String beanName = generateBeanName(repositoryInterface);

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(SqlRepositoryProxyFactoryBean.class);
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(repositoryInterface);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
            beanDefinition.setDependsOn("sqlRepositoryProxyFactory");

            registry.registerBeanDefinition(beanName, beanDefinition);
            log.debug("Registered SQL repository proxy: {} for interface: {}", beanName, className);

        } catch (ClassNotFoundException e) {
            log.error("Failed to load repository interface: {}", className, e);
        }
    }

    private String generateBeanName(Class<?> repositoryInterface) {
        String simpleName = repositoryInterface.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
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
