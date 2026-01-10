package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.annotation.HttpController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Scans for {@code @HttpController} interfaces and registers them as Spring beans.
 * <p>
 * This post-processor scans the classpath for interfaces annotated with
 * {@link HttpController} and creates dynamic proxy beans for each one.
 */
public class HttpControllerRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(HttpControllerRegistrar.class);

    private Environment environment;
    private final Set<String> basePackages;

    /**
     * Creates a new registrar with the given base packages.
     *
     * @param basePackages the packages to scan
     */
    public HttpControllerRegistrar(Set<String> basePackages) {
        this.basePackages = basePackages != null ? basePackages : new HashSet<>();
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
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
            new ClassPathScanningCandidateComponentProvider(false, environment) {
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

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // No post-processing of bean factory needed
    }
}
