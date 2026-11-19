package com.lutiano.api.service.search;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;

/** A {@link SearchHandler} defined by one select (by id) and a row-to-document mapping; see {@link SearchHandlers}. */
public class QueryHandler implements SearchHandler {
  private final JdbcTemplate db;
  private final String type, byIdSql, table;
  private final Function<Map<String, Object>, SearchDocument> mapper;

  /** @param byIdSql a select returning the row for {@code id = ?}; {@code table} is the table whose ids are backfilled */
  public QueryHandler(JdbcTemplate db, String type, String table, String byIdSql, Function<Map<String, Object>, SearchDocument> mapper) {
    this.db = db; this.type = type; this.table = table; this.byIdSql = byIdSql; this.mapper = mapper;
  }

  @Override public String type() { return type; }

  @Override public Optional<SearchDocument> load(String id) {
    return db.queryForList(byIdSql, id).stream().findFirst().map(mapper);   // a mapper returning null means "not indexed"
  }

  @Override public List<String> idsAfter(String afterId, int limit) {
    return db.queryForList("select id from " + table + " where id > ? order by id limit ?", String.class, afterId == null ? "" : afterId, limit);
  }

  /** A MariaDB UTC timestamp as an ISO local date-time string, which the strict date mapping accepts. */
  public static String iso(Object ts) { return ts instanceof Timestamp t ? t.toLocalDateTime().toString() : null; }
}
