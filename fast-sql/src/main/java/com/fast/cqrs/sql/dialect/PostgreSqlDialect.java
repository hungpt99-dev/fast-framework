package com.fast.cqrs.sql.dialect;

/**
 * PostgreSQL dialect.
 */
public class PostgreSqlDialect implements DatabaseDialect {
    
    @Override
    public String getName() {
        return "PostgreSQL";
    }

    @Override
    public String paginate(String sql, int limit, int offset) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier + "\"";
    }
}
