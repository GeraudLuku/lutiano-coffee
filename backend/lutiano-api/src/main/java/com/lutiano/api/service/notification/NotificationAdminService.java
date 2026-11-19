package com.lutiano.api.service.notification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Lists;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog.Event;
import com.lutiano.api.security.Actor;
import com.lutiano.api.proxy.SmsUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import com.lutiano.api.repository.notification.NotificationRepository;
import com.lutiano.api.models.request.notification.NotificationEventRequest;
import com.lutiano.api.definitions.enums.DeliveryStatus;
import com.lutiano.api.utils.Enums;

/** What administrators see and change in the Notifications page: the delivery log, event switches and texts, approval alerts. */
@Service
public class NotificationAdminService {
  public static final Set<String> STATUSES = Enums.names(DeliveryStatus.class);
  private static final int MAX_TEXT = 480;


  private final NotificationRepository repo;
  private final NotificationService notifications;
  private final SmsUtil sms;
  private final AuditService audit;

  public NotificationAdminService(NotificationRepository repo, NotificationService notifications, SmsUtil sms, AuditService audit) {
    this.repo = repo; this.notifications = notifications; this.sms = sms; this.audit = audit;
  }

  public PageResponse<Map<String, Object>> deliveries(LocalDate from, LocalDate to, String status, String event, String q, PageRequest req) {
    if (status != null && !status.isBlank()) Validate.oneOf(status, "Status", STATUSES);
    if (event != null && !event.isBlank() && !NotificationCatalog.exists(event)) throw Validate.bad("Unknown event.");
    PageResponse<Map<String, Object>> page = repo.deliveries(Periods.startOrNull(from), Periods.endOrNull(to), status, event, q, req);
    page.items().forEach(d -> {
      d.put("event_name", NotificationCatalog.get((String) d.get("event_key")).name());
      d.put("phone_masked", Phones.mask((String) d.get("phone")));
      d.put("resendable", "FAILED".equals(d.get("status")) && !NotificationCatalog.get((String) d.get("event_key")).oneTimeCode());
      d.remove("phone");
    });
    return page;
  }

  public Map<String, Object> resend(Actor actor, String id) {
    Map<String, Object> d = notifications.resend(Validate.id(id, "Message"));
    audit.log(actor, "NOTIFICATION_RESENT", "notification", id, (String) d.get("event_key"));
    return d;
  }

  /** One page of events with their channel switch and both texts (searchable by name). */
  public PageResponse<Map<String, Object>> events(String q, PageRequest req) {
    return PageResponse.slice(allEvents().stream().filter(e -> Lists.matches(q, e, "name", "key")).toList(), req);
  }

  private List<Map<String, Object>> allEvents() {
    Map<String, Boolean> sms = repo.smsSwitches();
    Map<String, Map<String, String>> texts = new LinkedHashMap<>();
    repo.templates().forEach(t -> texts.computeIfAbsent((String) t.get("event_key"), k -> new LinkedHashMap<>()).put((String) t.get("language"), (String) t.get("body")));
    List<Map<String, Object>> out = new ArrayList<>();
    for (Event e : NotificationCatalog.EVENTS) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("key", e.key()); row.put("name", e.name()); row.put("category", e.category()); row.put("audience", e.audience().name());
      row.put("placeholders", e.placeholders()); row.put("locked", e.oneTimeCode());
      row.put("sms_enabled", e.oneTimeCode() || sms.getOrDefault(e.key(), true));
      row.put("fr", texts.getOrDefault(e.key(), Map.of()).getOrDefault("fr", e.fr()));
      row.put("en", texts.getOrDefault(e.key(), Map.of()).getOrDefault("en", e.en()));
      out.add(row);
    }
    return out;
  }

  @Transactional
  /** A person's own messages for the bell and the Inbox tab. */
  public PageResponse<Map<String, Object>> inbox(Actor actor, String topic, PageRequest req) {
    List<String> hidden = NotificationCatalog.EVENTS.stream().filter(Event::oneTimeCode).map(Event::key).toList();
    PageResponse<Map<String, Object>> page = repo.inbox(actor.id(), hidden, topic == null || topic.isBlank() ? List.of() : NotificationCatalog.keysOfTopic(topic), req);
    page.items().forEach(d -> {
      d.put("event_name", NotificationCatalog.get((String) d.get("event_key")).name());
      d.put("topic", NotificationCatalog.topic((String) d.get("event_key")));
    });
    Map<String, Object> summary = new LinkedHashMap<>(page.summary());
    Map<String, Long> topics = new LinkedHashMap<>();
    repo.inboxCounts(actor.id(), hidden).forEach((k, n) -> topics.merge(NotificationCatalog.topic(k), n, Long::sum));
    summary.put("topics", topics);
    return page.withSummary(summary);
  }

  public void markRead(Actor actor, String id) { repo.markRead(actor.id(), id == null || id.isBlank() ? null : Validate.id(id, "Message")); }

  @Transactional
  public void saveEvent(Actor actor, String key, NotificationEventRequest in) {
    if (!NotificationCatalog.exists(key)) throw Validate.notFound("Unknown event.");
    Event e = NotificationCatalog.get(key);
    String fr = text(e, in.fr(), "French text"), en = text(e, in.en(), "English text");
    boolean enabled = in.smsEnabled() == null || in.smsEnabled();
    if (e.oneTimeCode() && !enabled) throw Validate.bad("Code messages cannot be switched off: people could not sign in or verify numbers.");
    repo.setSms(key, enabled, actor.id());
    repo.saveTemplate(key, "fr", fr, actor.id());
    repo.saveTemplate(key, "en", en, actor.id());
    audit.log(actor, "NOTIFICATION_TEMPLATE_UPDATED", "notification", null, e.name() + (enabled ? "" : " (SMS off)"));
  }

  private static String text(Event e, String raw, String field) {
    String t = Validate.text(raw, field, MAX_TEXT);
    List<String> bad = NotificationCatalog.unknownPlaceholders(e, t);
    if (!bad.isEmpty()) throw Validate.bad("The " + field.toLowerCase() + " uses {" + bad.get(0) + "}, which this message does not provide. Allowed: "
        + e.placeholders().stream().map(p -> "{" + p + "}").toList() + ".");
    if (e.oneTimeCode() && !t.contains("{code}")) throw Validate.bad("The " + field.toLowerCase() + " must contain {code}.");
    return t;
  }

  /** How SMS is sent now and who receives approval alerts. */
  public Map<String, Object> settings() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("mode", sms.isLive() ? "live" : "dev");
    out.put("sender_id", sms.senderId());
    out.put("approvers", repo.approverCandidates().stream().map(p -> {
      Map<String, Object> row = new LinkedHashMap<>(p);
      row.put("phone_masked", Phones.mask(Phones.normalize((String) p.get("phone"))));
      row.put("has_phone", Phones.normalize((String) p.get("phone")) != null);
      row.remove("phone");
      return row;
    }).toList());
    return out;
  }

  @Transactional
  public void saveApprovers(Actor actor, List<String> staffIds) {
    List<String> ids = staffIds == null ? List.of() : staffIds.stream().map(i -> Validate.id(i, "Staff member")).toList();
    Set<String> withPhone = new HashSet<>();
    repo.approverCandidates().forEach(p -> { if (Phones.normalize((String) p.get("phone")) != null) withPhone.add((String) p.get("id")); });
    if (!withPhone.containsAll(ids)) throw Validate.bad("Everyone chosen needs a valid mobile number in their profile.");
    repo.setApprovers(ids);
    audit.log(actor, "APPROVAL_ALERTS_CHANGED", "notification", null, ids.size() + " people receive approval alerts");
  }
}
