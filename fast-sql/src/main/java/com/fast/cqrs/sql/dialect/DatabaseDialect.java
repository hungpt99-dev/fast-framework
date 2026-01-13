package com.fast.cqrs.sql.dialect;

/**
 * Database dialect for generating database-specific SQL.
 * <p>
 * Different databases have different syntax for pagination, identity columns, etc.
 */
public interface DatabaseDialect {
    
    /**
     * Returns the dialect name.
     */
    String getName();

    /**
     * Generates pagination clause (LIMIT/OFFSET, ROWNUM, etc.)
     */
    String paginate(String sql, int limit, int offset);

    /**
     * Quotes an identifier (table/column name).
     */
    String quote(String identifier);

    /**
     * Returns SQL for checking if a row exists.
     */
    default String existsSql(String tableName, String idColumn) {
        return String.format("SELECT COUNT(*) FROM %s WHERE %s = :id", tableName, idColumn);
    }

    /**
     * Returns SQL for counting all rows.
     */
    default String countSql(String tableName) {
        return String.format("SELECT COUNT(*) FROM %s", tableName);
    }

    /**
     * Returns the parameter prefix (: for named, ? for positional).
     */
    default String paramPrefix() {
        return ":";
    }
}
