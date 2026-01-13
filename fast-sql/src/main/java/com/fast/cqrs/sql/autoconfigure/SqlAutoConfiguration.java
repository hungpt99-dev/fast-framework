package com.fast.cqrs.sql.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * Spring Boot auto-configuration for SQL Repository framework.
 * <p>
 * Automatically configures the repository infrastructure when
 * a DataSource is available.
 */
@AutoConfiguration
@ConditionalOnClass({DataSource.class, NamedParameterJdbcTemplate.class})
@Import(SqlRepositoryConfiguration.class)
public class SqlAutoConfiguration {
    // Bean definitions are in SqlRepositoryConfiguration
    // Repository scanning is triggered by @EnableSqlRepositories
}
