package com.lutiano.api.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;

/** Builds "where" clauses from optional filters without string-concatenating values. */
public final class SqlFilter {
  private final StringBuilder sql;
  private final List<Object> args = new ArrayList<>();
  private boolean truncated;

  public SqlFilter(String baseSelect) { this.sql = new StringBuilder(baseSelect).append(" where 1=1"); }

  public SqlFilter and(String condition, Object value) {
    if (value != null && !(value instanceof String s && s.isBlank())) { sql.append(" and ").append(condition); args.add(value); }
    return this;
  }

  /** Rows whose {@code column} is one of {@code values}; skipped when there are none. */
  public SqlFilter andIn(String column, Collection<?> values) {
    if (values == null || values.isEmpty()) return this;
    sql.append(" and ").append(column).append(" in (").append(String.join(",", Collections.nCopies(values.size(), "?"))).append(")");
    args.addAll(values);
    return this;
  }

  /** Free-text search: rows where any of the columns contains {@code q} (skipped when q is blank). */
  public SqlFilter search(String q, String... columns) {
    return and("concat_ws(' '," + String.join(",", columns) + ") like ?", q == null || q.isBlank() ? null : "%" + q.trim() + "%");
  }

  /**
   * Free-text search answered by the search service: rows whose id is among its candidates, or whose phone/email/code column
   * (not indexed) equals the typed number, address or code exactly; column names ending in "phone" or "email" are matched as such. Skipped when {@code c} is null
   * (blank text); matches nothing when the service found nothing.
   */
  public SqlFilter searchIds(Candidates c, String idColumn, String... contactColumns) {
    if (c == null) return this;
    List<String> parts = new ArrayList<>();
    List<Object> vals = new ArrayList<>();
    if (!c.ids().isEmpty()) {
      parts.add(idColumn + " in (" + String.join(",", Collections.nCopies(c.ids().size(), "?")) + ")");
      vals.addAll(c.ids());
    }
    String phone = Phones.normalize(c.q()), email = c.q().contains("@") ? c.q().trim().toLowerCase() : null;
    for (String col : contactColumns) {
      if (phone != null && col.endsWith("phone")) { parts.add(col + " = ?"); vals.add(phone); }
      else if (email != null && col.endsWith("email")) { parts.add(col + " = ?"); vals.add(email); }
      else if (!col.endsWith("phone") && !col.endsWith("email")) { parts.add(col + " = ?"); vals.add(c.q()); }   // other exact codes, e.g. a provider reference
    }
    sql.append(" and (").append(parts.isEmpty() ? "1=0" : String.join(" or ", parts)).append(")");
    args.addAll(vals);
    truncated |= c.truncated();
    return this;
  }

  /** A fixed condition with no parameter, e.g. "x is null". */
  public SqlFilter and(String condition) {
    sql.append(" and ").append(condition);
    return this;
  }

  public String sql(String suffix) { return sql + " " + suffix; }

  /** Every matching row (for short lists that are not paginated). */
  public List<Map<String, Object>> list(JdbcTemplate db, String orderBy) {
    return db.queryForList(sql + " " + orderBy, args.toArray());
  }

  /** Runs the filtered query for one page: a count over every match, then the rows of the page in {@code orderBy} order. */
  public PageResponse<Map<String, Object>> page(JdbcTemplate db, String orderBy, PageRequest req) {
    Long total = db.queryForObject("select count(*) from (" + sql + ") counted", Long.class, args.toArray());
    List<Object> pageArgs = new ArrayList<>(args);
    pageArgs.add(req.size());
    pageArgs.add(req.offset());
    return PageResponse.of(db.queryForList(sql + " " + orderBy + " limit ? offset ?", pageArgs.toArray()), req, total == null ? 0 : total).withTruncated(truncated);
  }

  /** An aggregate over every match, e.g. "count(*) n, sum(total_xaf) value", for a page summary. */
  public Map<String, Object> aggregate(JdbcTemplate db, String selectList) {
    return db.queryForMap("select " + selectList + " from (" + sql + ") filtered", args.toArray());
  }
  public Object[] args() { return args.toArray(); }
}
