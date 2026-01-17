package com.fast.cqrs.processor.sql;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry helper that prefers APT-generated implementations over runtime proxies.
 * <p>
 * When registering a repository bean, this helper first checks if a generated
 * implementation class exists (e.g., {@code OrderRepository_FastImpl}). If found,
 * it registers the generated class directly. Otherwise, it falls back to the
 * runtime proxy approach.
 * <p>
 * This provides a seamless migration path:
 * <ul>
 *   <li>During development: Generated classes provide compile-time performance</li>
 *   <li>Fallback: Runtime proxies still work for non-processed interfaces</li>
 * </ul>
 */
public class GeneratedRepositoryRegistry {

    private static final Logger log = LoggerFactory.getLogger(GeneratedRepositoryRegistry.class);
    
    /**
     * The suffix added to interface names for generated implementations.
     */
    public static final String GENERATED_SUFFIX = "_FastImpl";

    /**
     * Checks if a generated implementation exists for the given repository interface.
     *
     * @param repositoryInterface the repository interface class
     * @return true if a generated implementation exists on the classpath
     */
    public static boolean hasGeneratedImplementation(Class<?> repositoryInterface) {
        String implClassName = getGeneratedClassName(repositoryInterface);
        try {
            ClassUtils.forName(implClassName, repositoryInterface.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Gets the generated implementation class for the given repository interface.
     *
     * @param repositoryInterface the repository interface class
     * @return the generated implementation class
     * @throws ClassNotFoundException if no generated implementation exists
     */
    public static Class<?> getGeneratedImplementation(Class<?> repositoryInterface) 
            throws ClassNotFoundException {
        String implClassName = getGeneratedClassName(repositoryInterface);
        return ClassUtils.forName(implClassName, repositoryInterface.getClassLoader());
    }

    /**
     * Gets the fully qualified name of the generated implementation class.
     *
     * @param repositoryInterface the repository interface class
     * @return the fully qualified class name of the generated implementation
     */
    public static String getGeneratedClassName(Class<?> repositoryInterface) {
        return repositoryInterface.getName() + GENERATED_SUFFIX;
    }

    /**
     * Registers a repository bean, preferring the generated implementation if available.
     *
     * @param registry the bean definition registry
     * @param repositoryInterface the repository interface class
     * @param beanName the bean name to register
     * @param proxyFactoryBeanClass the fallback proxy factory bean class
     * @return true if a generated implementation was used, false if proxy was used
     */
    public static boolean registerWithGeneratedPreference(
            BeanDefinitionRegistry registry,
            Class<?> repositoryInterface,
            String beanName,
            Class<?> proxyFactoryBeanClass) {
        
        if (hasGeneratedImplementation(repositoryInterface)) {
            try {
                Class<?> implClass = getGeneratedImplementation(repositoryInterface);
                
                GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
                beanDefinition.setBeanClass(implClass);
                beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_CONSTRUCTOR);
                
                registry.registerBeanDefinition(beanName, beanDefinition);
                log.info("Registered APT-generated repository: {} -> {}", 
                        beanName, implClass.getSimpleName());
                return true;
                
            } catch (ClassNotFoundException e) {
                log.warn("Generated class not found, falling back to proxy: {}", e.getMessage());
            }
        }

        // Fallback to runtime proxy
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(proxyFactoryBeanClass);
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(repositoryInterface);
        beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        
        registry.registerBeanDefinition(beanName, beanDefinition);
        log.debug("Registered proxy-based repository: {} for interface: {}", 
                beanName, repositoryInterface.getSimpleName());
        return false;
    }

    /**
     * Creates a bean definition for the generated implementation.
     *
     * @param repositoryInterface the repository interface
     * @return the bean definition, or null if no generated implementation exists
     */
    public static BeanDefinition createGeneratedBeanDefinition(Class<?> repositoryInterface) {
        if (!hasGeneratedImplementation(repositoryInterface)) {
            return null;
        }

        try {
            Class<?> implClass = getGeneratedImplementation(repositoryInterface);
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(implClass);
            beanDefinition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_CONSTRUCTOR);
            return beanDefinition;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
