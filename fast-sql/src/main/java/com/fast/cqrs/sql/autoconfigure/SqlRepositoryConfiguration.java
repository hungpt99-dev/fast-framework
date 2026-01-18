package com.fast.cqrs.sql.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuration for SQL Repository infrastructure beans.
 * <p>
 * <b>Note:</b> This configuration is simplified for GraalVM native-image compatibility.
 * SQL repository proxies are removed. All repositories must be generated at compile-time
 * by the annotation processor.
 */
@Configuration
public class SqlRepositoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
}
