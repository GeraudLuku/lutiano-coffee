package com.lutiano.api.repository.notification;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.HashMap;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.definitions.enums.SearchType;

/** Event switches, texts and the delivery log, plus the staff queries notifications need. */
@Repository
public class NotificationRepository {
  private static final String DELIVERY_SELECT = "select d.* from notification_delivery d";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public NotificationRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- events and texts -----

  public void seedEvent(String key, boolean smsEnabled) { db.update("insert ignore into notification_event(event_key,sms_enabled) values(?,?)", key, smsEnabled); }

  public void seedTemplate(String key, String language, String body) {
    db.update("insert ignore into notification_template(event_key,language,body) values(?,?,?)", key, language, body);
  }

  public Map<String, Boolean> smsSwitches() {
    Map<String, Boolean> out = new HashMap<>();
    db.query("select event_key, sms_enabled from notification_event", rs -> { out.put(rs.getString(1), rs.getBoolean(2)); });
    return out;
  }

  public boolean smsEnabled(String key) {
    return db.queryForList("select sms_enabled from notification_event where event_key=?", Boolean.class, key).stream().findFirst().orElse(true);
  }

  public Optional<String> template(String key, String language) {
    return db.queryForList("select body from notification_template where event_key=? and language=?", String.class, key, language).stream().findFirst();
  }

  public List<Map<String, Object>> templates() { return db.queryForList("select event_key, language, body from notification_template"); }

  public void setSms(String key, boolean enabled, String actorId) {
    db.update("update notification_event set sms_enabled=?,updated_by=?,updated_at=UTC_TIMESTAMP() where event_key=?", enabled, actorId, key);
  }

  public void saveTemplate(String key, String language, String body, String actorId) {
    db.update("update notification_template set body=?,updated_by=?,updated_at=UTC_TIMESTAMP() where event_key=? and language=?", body, actorId, key, language);
  }

  // ----- deliveries -----

  public void insertDelivery(String id, String event, String recipientId, String recipientName, String phone, String language, String body,
                             String status, String error, String relatedType, String relatedId) {
    db.update("insert into notification_delivery(id,event_key,recipient_id,recipient_name,phone,language,body,status,error,related_type,related_id,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", id, event, recipientId, recipientName, phone, language, body, status, error, relatedType, relatedId);
    search.changed(SearchType.DELIVERY, id);
  }

  /** Records the outcome of an attempt. */
  public void markAttempt(String id, String status, String error) {
    db.update("update notification_delivery set status=?,error=?,attempts=attempts+1,sent_at=case when ? in ('SENT','LOGGED') then UTC_TIMESTAMP() else sent_at end where id=?",
        status, error, status, id);
    search.changed(SearchType.DELIVERY, id);
  }

  public Optional<Map<String, Object>> delivery(String id) { return db.queryForList(DELIVERY_SELECT + " where d.id=?", id).stream().findFirst(); }

  public PageResponse<Map<String, Object>> deliveries(Timestamp from, Timestamp to, String status, String event, String q, PageRequest req) {
    return filter(from, to, event, q).and("d.status=?", status).page(db, "order by d.created_at desc, d.id", req)
        .withSummary(filter(from, to, event, q).aggregate(db, "count(*) messages, coalesce(sum(status in ('SENT','LOGGED')),0) sent, coalesce(sum(status='FAILED'),0) failed," +
            " coalesce(sum(status='SKIPPED'),0) skipped, coalesce(sum(status='QUEUED'),0) queued"));
  }

  private static SqlFilter filter(Timestamp from, Timestamp to, String event, String q) {
    return new SqlFilter(DELIVERY_SELECT).and("d.created_at>=?", from).and("d.created_at<?", to).and("d.event_key=?", event)
        .search(q, "d.recipient_name", "d.phone", "d.body");
  }

  // ----- inbox -----

  /** One person's own messages (never code messages), newest first; the summary counts the unread ones. */
  public PageResponse<Map<String, Object>> inbox(String personId, List<String> hiddenEvents, List<String> onlyEvents, PageRequest req) {
    String base = "select d.id,d.event_key,d.body,d.related_type,d.related_id,d.created_at,d.read_at from notification_delivery d" +
        " where d.recipient_id=? and d.event_key not in (" + String.join(",", hiddenEvents.stream().map(e -> "?").toList()) + ")" +
        (onlyEvents.isEmpty() ? "" : " and d.event_key in (" + String.join(",", onlyEvents.stream().map(e -> "?").toList()) + ")");
    List<Object> args = new ArrayList<>(List.of(personId));
    args.addAll(hiddenEvents);
    args.addAll(onlyEvents);
    Long total = db.queryForObject("select count(*) from (" + base + ") c", Long.class, args.toArray());
    Long unread = db.queryForObject("select count(*) from (" + base + " and d.read_at is null) c", Long.class, args.toArray());
    List<Object> pageArgs = new ArrayList<>(args);
    pageArgs.add(req.size());
    pageArgs.add(req.offset());
    return PageResponse.of(db.queryForList(base + " order by d.created_at desc, d.id limit ? offset ?", pageArgs.toArray()), req, total == null ? 0 : total)
        .withSummary(Map.of("unread", unread == null ? 0 : unread));
  }

  /** How many messages the person has per event (code messages excluded), for the topic counts. */
  public Map<String, Long> inboxCounts(String personId, List<String> hiddenEvents) {
    Map<String, Long> out = new HashMap<>();
    List<Object> args = new ArrayList<>(List.of(personId));
    args.addAll(hiddenEvents);
    db.query("select event_key, count(*) from notification_delivery where recipient_id=? and event_key not in (" + String.join(",", hiddenEvents.stream().map(e -> "?").toList()) + ") group by event_key",
        rs -> { out.put(rs.getString(1), rs.getLong(2)); }, args.toArray());
    return out;
  }

  /** Marks one message, or all of the person's unread messages when {@code id} is null, as read. */
  public void markRead(String personId, String id) {
    db.update("update notification_delivery set read_at=UTC_TIMESTAMP() where recipient_id=? and read_at is null and (? is null or id=?)", personId, id, id);
  }

  // ----- staff -----

  public Optional<Map<String, Object>> person(String id) {
    return db.queryForList("select id,full_name,phone,language,active,role from staff_user where id=?" +
        " union all select id,full_name,phone,language,(status='ACTIVE' and deleted_at is null) active,'CUSTOMER' role from customer where id=?", id, id).stream().findFirst();
  }

  /** Active staff who receive approval alerts, limited to {@code roles}, minus {@code excludeId} (the person who made the request). */
  public List<Map<String, Object>> approvers(List<String> roles, String excludeId) {
    String in = String.join(",", roles.stream().map(r -> "?").toList());
    List<Object> args = new ArrayList<>(roles);
    args.add(excludeId == null ? "" : excludeId);
    return db.queryForList("select id,full_name,phone,language from staff_user where active=true and approval_alerts=true and role in (" + in + ") and id<>? order by full_name", args.toArray());
  }

  /** Everyone who can be chosen to receive approval alerts. */
  public List<Map<String, Object>> approverCandidates() {
    return db.queryForList("select id,full_name,role,phone,language,approval_alerts from staff_user where active=true and role in ('ADMIN','INVENTORY_MANAGER') order by role, full_name");
  }

  public void setApprovers(List<String> ids) {
    db.update("update staff_user set approval_alerts=false where role in ('ADMIN','INVENTORY_MANAGER')");
    for (String id : ids) db.update("update staff_user set approval_alerts=true where id=? and role in ('ADMIN','INVENTORY_MANAGER')", id);
  }

  /** Sellers assigned to a sales location. */
  public List<Map<String, Object>> sellersAt(String operationId) {
    return db.queryForList("select u.id,u.full_name,u.phone,u.language,u.role from seller_operation x join staff_user u on u.id=x.seller_id where x.operation_id=? and u.active=true", operationId);
  }
}
