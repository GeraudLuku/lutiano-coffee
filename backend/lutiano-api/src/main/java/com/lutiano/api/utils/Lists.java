package com.lutiano.api.utils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/** Helpers for paging small entity lists (locations, products, staff) that are filtered in code. */
public final class Lists {
  private Lists() {}

  /** True when q is blank or any field contains it (case-insensitive). */
  public static boolean matches(String q, Map<String, Object> row, String... fields) {
    if (q == null || q.isBlank()) return true;
    String needle = q.trim().toLowerCase();
    for (String f : fields) if (Objects.toString(row.get(f), "").toLowerCase().contains(needle)) return true;
    return false;
  }

  /**
   * True when the search text is blank (candidates null), the row's id is among the search service's candidates, or one of the
   * row's phone/email fields equals the typed number or address exactly (those are never indexed).
   */
  public static boolean inCandidates(Candidates c, Map<String, Object> row, String... contactFields) {
    if (c == null || c.ids().contains(Objects.toString(row.get("id"), ""))) return true;
    String phone = Phones.normalize(c.q()), email = c.q().contains("@") ? c.q().trim().toLowerCase() : null;
    for (String f : contactFields) {
      String v = Objects.toString(row.get(f), "");
      if (phone != null && f.endsWith("phone") && v.equals(phone)) return true;
      if (email != null && f.endsWith("email") && v.equalsIgnoreCase(email)) return true;
    }
    return false;
  }

  /** Equality filter that is skipped when the wanted value is blank. Booleans compare as "true"/"false". */
  public static boolean is(String wanted, Object value) {
    if (wanted == null || wanted.isBlank()) return true;
    String v = value instanceof Number n && !(value instanceof Double) ? (n.intValue() == 1 ? "true" : n.intValue() == 0 ? "false" : n.toString()) : Objects.toString(value, "");
    return wanted.equalsIgnoreCase(v);
  }

  public static long count(List<Map<String, Object>> rows, Predicate<Map<String, Object>> p) { return rows.stream().filter(p).count(); }
}
