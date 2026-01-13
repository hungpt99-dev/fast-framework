package com.fast.cqrs.sql.dialect;

/**
 * H2 dialect (for testing).
 */
public class H2Dialect implements DatabaseDialect {
    
    @Override
    public String getName() {
        return "H2";
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
