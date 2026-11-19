package com.lutiano.api.utils;

import java.util.List;
import java.util.Map;

/** Helpers for the Map rows returned by JdbcTemplate.queryForList. */
public final class Rows {
  private Rows() {}

  /** {@code s} cut to at most {@code max} characters (null stays null), for free text stored in fixed-width columns. */
  public static String clip(String s, int max) { return s == null || s.length() <= max ? s : s.substring(0, max); }

  public static long asLong(Object value) { return value == null ? 0 : ((Number) value).longValue(); }
  public static int asInt(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

  /** A BOOLEAN column (MariaDB may return Boolean or 0/1 for aggregates). */
  public static boolean truthy(Object value) { return Boolean.TRUE.equals(value) || (value instanceof Number n && n.intValue() == 1); }

  public static <T> T first(List<T> rows, String notFoundMessage) {
    if (rows.isEmpty()) throw Validate.notFound(notFoundMessage);
    return rows.get(0);
  }

  public static Map<String, Object> one(List<Map<String, Object>> rows, String notFoundMessage) { return first(rows, notFoundMessage); }
}
