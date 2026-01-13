package com.fast.cqrs.sql.dialect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Factory for detecting and creating the appropriate database dialect.
 */
public class DialectFactory {

    private static final Logger log = LoggerFactory.getLogger(DialectFactory.class);

    /**
     * Auto-detects the dialect from the DataSource.
     */
    public static DatabaseDialect detect(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String productName = metaData.getDatabaseProductName().toLowerCase();
            
            log.info("Detected database: {}", productName);
            
            if (productName.contains("mysql") || productName.contains("mariadb")) {
                return new MySqlDialect();
            } else if (productName.contains("postgresql")) {
                return new PostgreSqlDialect();
            } else if (productName.contains("oracle")) {
                return new OracleDialect();
            } else if (productName.contains("sql server") || productName.contains("sqlserver")) {
                return new SqlServerDialect();
            } else if (productName.contains("h2")) {
                return new H2Dialect();
            }
            
            log.warn("Unknown database '{}', using default H2 dialect", productName);
            return new H2Dialect();
            
        } catch (Exception e) {
            log.error("Failed to detect database dialect, using H2", e);
            return new H2Dialect();
        }
    }

    /**
     * Creates a dialect by name.
     */
    public static DatabaseDialect forName(String name) {
        return switch (name.toLowerCase()) {
            case "mysql", "mariadb" -> new MySqlDialect();
            case "postgresql", "postgres" -> new PostgreSqlDialect();
            case "oracle" -> new OracleDialect();
            case "sqlserver", "mssql" -> new SqlServerDialect();
            case "h2" -> new H2Dialect();
            default -> throw new IllegalArgumentException("Unknown dialect: " + name);
        };
    }
}
