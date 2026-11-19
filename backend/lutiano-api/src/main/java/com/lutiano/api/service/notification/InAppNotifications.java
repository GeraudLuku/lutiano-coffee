package com.lutiano.api.service.notification;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog.Event;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.notification.AppNotificationRepository;
import com.lutiano.api.repository.notification.DeviceTokenRepository;
import com.lutiano.api.proxy.PushSender;
import com.lutiano.api.proxy.PushTitles;
import com.lutiano.api.definitions.enums.CustomerNotificationCategory;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.NotificationCategory;

/**
 * What a seller sees in the app: every message the platform has for them (not only the ones sent by SMS), grouped in categories,
 * with read and cleared state and per-category preferences. Cash and payout categories cannot be switched off.
 */
@Service
public class InAppNotifications {
  public static final List<String> CATEGORIES = Enums.list(NotificationCategory.class);
  /** Money and cash: people must not miss them, so these stay on. */
  public static final Set<String> LOCKED = Set.of(NotificationCategory.EARNINGS.name(), NotificationCategory.OPERATIONS.name());
  /** The Customer app's categories (ORDERS carries order updates and cannot be switched off). */
  public static final List<String> CUSTOMER_CATEGORIES = Enums.list(CustomerNotificationCategory.class);
  public static final Set<String> CUSTOMER_LOCKED = Set.of(CustomerNotificationCategory.ORDERS.name());
  static final int KEEP_CLEARED_DAYS = 90, MAX_IDS = 200;

  private final AppNotificationRepository repo;
  private final PushSender push;
  private final DeviceTokenRepository devices;

  public InAppNotifications(AppNotificationRepository repo, PushSender push, DeviceTokenRepository devices) { this.repo = repo; this.push = push; this.devices = devices; }

  /** Called when a message is prepared for a seller: it becomes an in-app notification unless they switched the category off. */
  public void record(String recipientId, String eventKey, String text, String language, String relatedType, String relatedId) {
    Event event = NotificationCatalog.get(eventKey);
    if (event.oneTimeCode()) return;
    String category = NotificationCatalog.inAppCategory(eventKey);
    if (!LOCKED.contains(category) && repo.inAppOff(recipientId).contains(category)) return;
    String body = stripBrand(text);
    String id = repo.insert(recipientId, eventKey, category, null, body, ESSENTIAL.equals(event.category()), relatedType, relatedId);
    pushTo(recipientId, category, PushTitles.of(eventKey, language), body, id, relatedType, relatedId);
  }

  /** The same message on the person's phones, unless they switched push off for the category. */
  private void pushTo(String recipientId, String category, String title, String body, String notificationId, String relatedType, String relatedId) {
    if (!push.enabled() || repo.pushOff(recipientId).contains(category)) return;
    Map<String, String> data = new LinkedHashMap<>();
    data.put("notificationId", notificationId);
    if (relatedType != null) data.put("relatedType", relatedType);
    if (relatedId != null) data.put("relatedId", relatedId);
    push.notifyAfterCommit(recipientId, title, body, data);
  }

  private static final String ESSENTIAL = NotificationCatalog.ESSENTIAL;

  /** The SMS wording starts with the brand ("LUTIANO : …"); inside the app that is noise. */
  static String stripBrand(String text) { return text.replaceFirst("^LUTIANO\\s?:\\s*", ""); }

  /** An announcement from an administrator, shown to one seller (always, when it is important; otherwise unless they switched System off). */
  public void announce(String recipientId, String category, String title, String body, boolean important, String announcementId) {
    if (!important && repo.inAppOff(recipientId).contains(category)) return;
    String id = repo.insert(recipientId, "ANNOUNCEMENT", category, title, body, important, "announcement", announcementId);
    pushTo(recipientId, category, title, body.length() > 200 ? body.substring(0, 197) + "…" : body, id, "announcement", announcementId);
  }

  // ----- reads -----

  /** [filter] is UNREAD or IMPORTANT; [days] limits to the last N days (1 to 365). */
  public PageResponse<Map<String, Object>> list(Actor me, String category, String filter, Integer days, PageRequest req) {
    String cat = category == null || category.isBlank() ? null : Validate.oneOf(category, "Category", Set.copyOf(categories(me)));
    String f = filter == null || filter.isBlank() ? null : Validate.oneOf(filter, "Filter", Set.of("UNREAD", "IMPORTANT"));
    Timestamp since = days == null ? null : Periods.startOf(Periods.today().minusDays(Validate.range(days, "Days", 1, 365) - 1L));
    return repo.list(me.id(), cat, "UNREAD".equals(f), "IMPORTANT".equals(f), since, req);
  }

  public Map<String, Object> get(Actor me, String id) {
    return repo.find(Validate.id(id, "Notification"), me.id()).orElseThrow(() -> Validate.notFound("Notification not found."));
  }

  public Map<String, Object> unread(Actor me) { return Map.of("unread", repo.unread(me.id())); }

  // ----- changes -----

  /** Marks the given notifications as read; with no ids, all of them. */
  public Map<String, Object> markRead(Actor me, List<String> ids) { return Map.of("changed", repo.markRead(me.id(), checked(ids))); }

  /** Hides the given notifications; with no ids, every read one. */
  public Map<String, Object> clear(Actor me, List<String> ids) { return Map.of("changed", repo.clear(me.id(), checked(ids))); }

  private static List<String> checked(List<String> ids) {
    if (ids == null) return List.of();
    if (ids.size() > MAX_IDS) throw Validate.bad("Choose at most " + MAX_IDS + " notifications at a time.");
    List<String> out = new ArrayList<>();
    for (String id : ids) out.add(Validate.id(id, "Notification"));
    return out;
  }

  // ----- push devices -----

  /** Registers this person's phone for push notifications; whoever signs in on a phone owns its token. */
  public Map<String, Object> registerDevice(Actor me, String token, String platform) {
    String t = Validate.text(token, "Token", 512);
    String p = Validate.oneOf(platform == null ? "ANDROID" : platform.toUpperCase(), "Platform", Set.of("ANDROID", "IOS"));
    devices.register(me.id(), t, p);
    return Map.of("registered", true, "pushAvailable", push.enabled());
  }

  public Map<String, Object> unregisterDevice(Actor me, String token) {
    devices.unregister(me.id(), token);
    return Map.of("registered", false);
  }

  // ----- preferences -----

  private static List<String> categories(Actor me) { return Roles.CUSTOMER.equals(me.role()) ? CUSTOMER_CATEGORIES : CATEGORIES; }
  private static Set<String> locked(Actor me) { return Roles.CUSTOMER.equals(me.role()) ? CUSTOMER_LOCKED : LOCKED; }

  /** Every category with whether the app and push are on, and whether it can be switched off. */
  public List<Map<String, Object>> preferences(Actor me) {
    Map<String, Map<String, Object>> saved = repo.preferences(me.id());
    List<Map<String, Object>> out = new ArrayList<>();
    for (String c : categories(me)) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("category", c);
      row.put("in_app", locked(me).contains(c) || !saved.containsKey(c) || Boolean.TRUE.equals(saved.get(c).get("in_app")) || Integer.valueOf(1).equals(saved.get(c).get("in_app")));
      row.put("push", !saved.containsKey(c) || Boolean.TRUE.equals(saved.get(c).get("push")) || Integer.valueOf(1).equals(saved.get(c).get("push")));
      row.put("locked", locked(me).contains(c));
      out.add(row);
    }
    return out;
  }

  public List<Map<String, Object>> savePreference(Actor me, String category, Boolean inApp, Boolean push) {
    String c = Validate.oneOf(category, "Category", Set.copyOf(categories(me)));
    if (inApp == null && push == null) throw Validate.bad("Say what to change.");
    Map<String, Object> now = preferences(me).stream().filter(r -> c.equals(r.get("category"))).findFirst().orElseThrow();
    boolean app = inApp == null ? (boolean) now.get("in_app") : inApp, p = push == null ? (boolean) now.get("push") : push;
    if (locked(me).contains(c) && !app) throw Validate.conflict("This category carries money and cash messages, so it cannot be switched off.");
    repo.savePreference(me.id(), c, app, p);
    return preferences(me);
  }

  /** Cleared notifications are kept for 90 days, then removed. */
  @Scheduled(cron = "0 30 3 * * *", zone = "Africa/Douala")
  public void purge() { repo.purgeCleared(KEEP_CLEARED_DAYS); }

  static LocalDate today() { return Periods.today(); }
}
