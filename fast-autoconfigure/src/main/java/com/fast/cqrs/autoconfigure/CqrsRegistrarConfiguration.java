package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.CommandBus;
import com.fast.cqrs.cqrs.DefaultCommandBus;
import com.fast.cqrs.cqrs.DefaultQueryBus;
import com.fast.cqrs.cqrs.QueryBus;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.QueryHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class imported by {@link EnableCqrs}.
 * <p>
 * Registers the core CQRS infrastructure beans.
 * <p>
 * <b>Note:</b> This configuration is simplified for GraalVM native-image compatibility.
 * Controller proxies and CqrsDispatcher are removed. All dispatch is handled
 * at compile-time by the annotation processor.
 */
@Configuration
public class CqrsRegistrarConfiguration {

    /**
     * Creates the CommandBus bean.
     * <p>
     * The CommandBus is still available for programmatic use via CommandGateway,
     * but generated controllers use direct handler injection.
     *
     * @param handlers all CommandHandler beans in the application
     * @return the command bus
     */
    @Bean
    @ConditionalOnMissingBean(CommandBus.class)
    public CommandBus commandBus(List<CommandHandler<?>> handlers) {
        return new DefaultCommandBus(handlers);
    }

    /**
     * Creates the QueryBus bean.
     * <p>
     * The QueryBus is still available for programmatic use via QueryGateway,
     * but generated controllers use direct handler injection.
     *
     * @param handlers all QueryHandler beans in the application
     * @return the query bus
     */
    @Bean
    @ConditionalOnMissingBean(QueryBus.class)
    public QueryBus queryBus(List<QueryHandler<?, ?>> handlers) {
        return new DefaultQueryBus(handlers);
    }

    /**
     * Creates the CommandGateway bean.
     */
    @Bean
    @ConditionalOnMissingBean(com.fast.cqrs.cqrs.gateway.CommandGateway.class)
    public com.fast.cqrs.cqrs.gateway.CommandGateway commandGateway(CommandBus commandBus) {
        return new com.fast.cqrs.cqrs.gateway.DefaultCommandGateway(commandBus);
    }

    /**
     * Creates the QueryGateway bean.
     */
    @Bean
    @ConditionalOnMissingBean(com.fast.cqrs.cqrs.gateway.QueryGateway.class)
    public com.fast.cqrs.cqrs.gateway.QueryGateway queryGateway(QueryBus queryBus) {
        return new com.fast.cqrs.cqrs.gateway.DefaultQueryGateway(queryBus);
    }

    /**
     * Creates the EventBus bean.
     */
    @Bean
    @ConditionalOnMissingBean(com.fast.cqrs.cqrs.EventBus.class)
    public com.fast.cqrs.cqrs.EventBus eventBus(List<com.fast.cqrs.cqrs.EventHandler<?>> handlers) {
        return new com.fast.cqrs.cqrs.DefaultEventBus(handlers);
    }
}
