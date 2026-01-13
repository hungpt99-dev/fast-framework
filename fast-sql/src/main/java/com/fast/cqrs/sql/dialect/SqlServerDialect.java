package com.fast.cqrs.sql.dialect;

/**
 * SQL Server dialect.
 */
public class SqlServerDialect implements DatabaseDialect {
    
    @Override
    public String getName() {
        return "SQLServer";
    }

    @Override
    public String paginate(String sql, int limit, int offset) {
        // SQL Server 2012+ supports OFFSET/FETCH
        // Requires ORDER BY clause
        if (!sql.toUpperCase().contains("ORDER BY")) {
            sql = sql + " ORDER BY (SELECT NULL)";
        }
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String quote(String identifier) {
        return "[" + identifier + "]";
    }
}
