package com.lutiano.api.service.search;

import com.lutiano.api.utils.Phones;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Phone numbers and emails are never indexed, so a search that looks like one is answered by an exact lookup in MariaDB.
 * Only administrators may do this (it is how staff and customers are found by contact), and it is rate limited by the caller.
 */
@Component
public class ContactLookup {
  private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private final JdbcTemplate db;
  private final SearchRateLimiter limiter;

  public ContactLookup(JdbcTemplate db, SearchRateLimiter limiter) { this.db = db; this.limiter = limiter; }

  /** Exact matches for a phone or email, as search result rows; empty when the text is neither or the caller is not an administrator. */
  public List<Map<String, Object>> find(SearchPrincipal who, String text) {
    if (!who.isAdmin()) return List.of();
    String phone = Phones.normalize(text);
    boolean email = EMAIL.matcher(text).matches();
    if (phone == null && !email) return List.of();
    limiter.check("contact", who.id(), 20);
    List<Map<String, Object>> out = new ArrayList<>();
    if (email) for (Map<String, Object> r : db.queryForList("select id, full_name, area from staff_user where email = ?", text.toLowerCase()))
      out.add(row("STAFF", r));
    if (phone != null) {
      for (Map<String, Object> r : db.queryForList("select id, full_name, area from staff_user where phone = ?", phone)) out.add(row("STAFF", r));
      for (Map<String, Object> r : db.queryForList("select id, full_name, code area from customer where phone = ?", phone)) out.add(row("CUSTOMER", r));
    }
    return out;
  }

  private static Map<String, Object> row(String type, Map<String, Object> r) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", type); m.put("id", r.get("id")); m.put("title", r.get("full_name")); m.put("subtitle", r.get("area"));
    m.put("reference", null); m.put("highlight", null);
    return m;
  }
}
