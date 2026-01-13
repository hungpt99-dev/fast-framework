package com.fast.cqrs.autoconfigure;

import com.fast.cqrs.cqrs.CommandBus;
import com.fast.cqrs.cqrs.DefaultCommandBus;
import com.fast.cqrs.cqrs.DefaultQueryBus;
import com.fast.cqrs.cqrs.QueryBus;
import com.fast.cqrs.cqrs.CqrsDispatcher;
import com.fast.cqrs.cqrs.CommandHandler;
import com.fast.cqrs.cqrs.QueryHandler;
import com.fast.cqrs.web.ControllerProxyFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class imported by {@link EnableCqrs}.
 * <p>
 * Registers the core CQRS infrastructure beans including buses,
 * dispatcher, and proxy factory.
 */
@Configuration
public class CqrsRegistrarConfiguration {

    /**
     * Creates the CommandBus bean.
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
     * Creates the CQRS dispatcher.
     *
     * @param commandBus the command bus
     * @param queryBus   the query bus
     * @return the dispatcher
     */
    @Bean
    @ConditionalOnMissingBean(CqrsDispatcher.class)
    public CqrsDispatcher cqrsDispatcher(CommandBus commandBus, QueryBus queryBus, org.springframework.context.ApplicationContext context) {
        CqrsDispatcher dispatcher = new CqrsDispatcher(commandBus, queryBus);
        dispatcher.setApplicationContext(context);
        return dispatcher;
    }

    /**
     * Creates the controller proxy factory.
     *
     * @param dispatcher the CQRS dispatcher
     * @return the proxy factory
     */
    @Bean
    @ConditionalOnMissingBean(ControllerProxyFactory.class)
    public ControllerProxyFactory controllerProxyFactory(CqrsDispatcher dispatcher) {
        return new ControllerProxyFactory(dispatcher);
    }
}
