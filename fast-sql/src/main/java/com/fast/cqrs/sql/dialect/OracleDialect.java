package com.fast.cqrs.sql.dialect;

/**
 * Oracle dialect.
 */
public class OracleDialect implements DatabaseDialect {
    
    @Override
    public String getName() {
        return "Oracle";
    }

    @Override
    public String paginate(String sql, int limit, int offset) {
        // Oracle 12c+ supports OFFSET/FETCH
        return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
    }

    @Override
    public String quote(String identifier) {
        return "\"" + identifier + "\"";
    }
}
