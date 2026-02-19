package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.EventHandler;
import com.fast.cqrs.cqrs.QueryHandler;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * AOT processor that registers reflection hints for all CQRS handlers.
 * This ensures that DefaultCommandBus, DefaultQueryBus, and DefaultEventBus
 * can inspect handler methods (preHandle, postHandle, onError) at runtime
 * even in a native image.
 */
public class CqrsAotProcessor implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        Set<Class<?>> handlers = new HashSet<>();
        
        // Find all CommandHandlers
        String[] commandHandlerNames = beanFactory.getBeanNamesForType(CommandHandler.class, true, false);
        for (String name : commandHandlerNames) {
            addClass(handlers, beanFactory.getType(name));
        }
        
        // Find all QueryHandlers
        String[] queryHandlerNames = beanFactory.getBeanNamesForType(QueryHandler.class, true, false);
        for (String name : queryHandlerNames) {
            addClass(handlers, beanFactory.getType(name));
        }

        // Find all EventHandlers
        String[] eventHandlerNames = beanFactory.getBeanNamesForType(EventHandler.class, true, false);
        for (String name : eventHandlerNames) {
            addClass(handlers, beanFactory.getType(name));
        }
        
        if (handlers.isEmpty()) {
            return null;
        }

        return (generationContext, beanFactoryInitializationCode) -> {
            RuntimeHints hints = generationContext.getRuntimeHints();
            for (Class<?> handlerClass : handlers) {
                // Register method invocation and declarion access
                hints.reflection().registerType(handlerClass, 
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.INTROSPECT_DECLARED_METHODS);
            }
        };
    }
    
    private void addClass(Set<Class<?>> handlers, Class<?> clazz) {
        if (clazz != null && !clazz.isInterface()) {
            handlers.add(clazz);
        }
    }
}
