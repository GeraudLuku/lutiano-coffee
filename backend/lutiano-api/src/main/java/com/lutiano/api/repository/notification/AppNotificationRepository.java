package com.lutiano.api.repository.notification;

import java.util.LinkedHashMap;
import java.util.HashMap;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A seller's in-app notifications and what they chose to receive. */
@Repository
public class AppNotificationRepository {
  private static final String SELECT = "select n.id, n.event_key, n.category, n.title, n.body, n.important, n.related_type, n.related_id, n.created_at, n.read_at from app_notification n";
  private final JdbcTemplate db;

  public AppNotificationRepository(JdbcTemplate db) { this.db = db; }

  public String insert(String recipientId, String eventKey, String category, String title, String body, boolean important, String relatedType, String relatedId) {
    String id = UUID.randomUUID().toString();
    db.update("insert into app_notification(id,recipient_id,event_key,category,title,body,important,related_type,related_id,created_at) values(?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, recipientId, eventKey, category, title, body, important, relatedType, relatedId);
    return id;
  }

  /**
   * The person's notifications that are not cleared, newest first. [category] and [unreadOnly] / [importantOnly] narrow the list,
   * [since] limits it to a period. The summary counts what is not cleared, whatever the filter: all, unread, important, and
   * total and unread per category.
   */
  public PageResponse<Map<String, Object>> list(String personId, String category, boolean unreadOnly, boolean importantOnly, Timestamp since, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("n.recipient_id=?", personId).and("n.cleared_at is null").and("n.category=?", category).and("n.created_at>=?", since);
    if (unreadOnly) f.and("n.read_at is null");
    if (importantOnly) f.and("n.important=true");
    Map<String, Object> summary = new LinkedHashMap<>(db.queryForMap(
        "select count(*) total, coalesce(sum(read_at is null),0) unread, coalesce(sum(important=true),0) important from app_notification where recipient_id=? and cleared_at is null", personId));
    Map<String, Map<String, Object>> categories = new LinkedHashMap<>();
    for (Map<String, Object> r : db.queryForList("select category, count(*) total, coalesce(sum(read_at is null),0) unread from app_notification where recipient_id=? and cleared_at is null group by category", personId))
      categories.put((String) r.get("category"), Map.of("total", r.get("total"), "unread", r.get("unread")));
    summary.put("categories", categories);
    return f.page(db, "order by n.created_at desc, n.id", req).withSummary(summary);
  }

  public Optional<Map<String, Object>> find(String id, String personId) {
    return db.queryForList(SELECT + " where n.id=? and n.recipient_id=? and n.cleared_at is null", id, personId).stream().findFirst();
  }

  public long unread(String personId) {
    Long n = db.queryForObject("select count(*) from app_notification where recipient_id=? and cleared_at is null and read_at is null", Long.class, personId);
    return n == null ? 0 : n;
  }

  /** Marks the given notifications (or all of the person's unread ones when [ids] is empty) as read; returns how many changed. */
  public int markRead(String personId, List<String> ids) {
    SqlFilter f = new SqlFilter("update app_notification set read_at=UTC_TIMESTAMP()").and("recipient_id=?", personId).and("read_at is null").andIn("id", ids);
    return db.update(f.sql(""), f.args());
  }

  /** Hides the given notifications; with no ids, hides every one the person has already read. Returns how many were hidden. */
  public int clear(String personId, List<String> ids) {
    SqlFilter f = new SqlFilter("update app_notification set cleared_at=UTC_TIMESTAMP()").and("recipient_id=?", personId).and("cleared_at is null").andIn("id", ids);
    if (ids == null || ids.isEmpty()) f.and("read_at is not null");
    return db.update(f.sql(""), f.args());
  }

  /** Removes notifications cleared more than [days] days ago. */
  public int purgeCleared(int days) {
    return db.update("delete from app_notification where cleared_at is not null and cleared_at < date_sub(UTC_TIMESTAMP(), interval ? day)", days);
  }

  // ----- preferences -----

  /** The categories the person has switched off for the app (everything else is on). */
  public List<String> inAppOff(String personId) {
    return db.queryForList("select category from notification_preference where user_id=? and in_app=false", String.class, personId);
  }

  /** The categories the person has switched push off for. */
  public List<String> pushOff(String personId) {
    return db.queryForList("select category from notification_preference where user_id=? and push=false", String.class, personId);
  }

  public Map<String, Map<String, Object>> preferences(String personId) {
    Map<String, Map<String, Object>> out = new HashMap<>();
    for (Map<String, Object> r : db.queryForList("select category, in_app, push from notification_preference where user_id=?", personId)) out.put((String) r.get("category"), r);
    return out;
  }

  public void savePreference(String personId, String category, boolean inApp, boolean push) {
    db.update("insert into notification_preference(user_id,category,in_app,push) values(?,?,?,?) on duplicate key update in_app=values(in_app), push=values(push)", personId, category, inApp, push);
  }
}
