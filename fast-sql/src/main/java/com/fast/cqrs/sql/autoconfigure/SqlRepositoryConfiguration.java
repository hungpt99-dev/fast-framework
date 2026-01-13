package com.fast.cqrs.sql.autoconfigure;

import com.fast.cqrs.sql.proxy.SqlRepositoryProxyFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for SQL Repository infrastructure beans.
 */
@Configuration
public class SqlRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlRepositoryProxyFactory sqlRepositoryProxyFactory(NamedParameterJdbcTemplate jdbcTemplate) {
        return new SqlRepositoryProxyFactory(jdbcTemplate);
    }
}
