package com.fast.cqrs.sql.dialect;

/**
 * MySQL dialect.
 */
public class MySqlDialect implements DatabaseDialect {
    
    @Override
    public String getName() {
        return "MySQL";
    }

    @Override
    public String paginate(String sql, int limit, int offset) {
        return sql + " LIMIT " + limit + " OFFSET " + offset;
    }

    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }
}
