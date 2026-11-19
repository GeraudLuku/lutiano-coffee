package com.lutiano.api.service.notification;

import com.lutiano.api.security.Roles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog.Event;
import com.lutiano.api.proxy.SmsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Locale;
import com.lutiano.api.repository.notification.NotificationRepository;
import com.lutiano.api.definitions.enums.SmsStatus;
import com.lutiano.api.definitions.enums.NotificationAudience;

/**
 * What the rest of the platform calls to tell someone something. It looks up the event's text in the person's language,
 * records the delivery, and sends the SMS after the surrounding transaction commits, so a rolled-back action sends
 * nothing and a slow or failing SMS provider never delays or breaks the action. Nothing here throws to the caller.
 * <pre>notifications.send(PAYOUT_PAID, sellerId, Map.of("amount", "5 000 FCFA", ...), "payout", payoutId);</pre>
 */
@Service
public class NotificationService implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
  public static final String SENSITIVE_CODE = "••••••";

  /** A message ready to send: its stored delivery row and the real text (which may contain a code). */
  private record Prepared(String deliveryId, String phone, String text, boolean sensitive, boolean send) {}

  private final NotificationRepository repo;
  private final SmsUtil sms;
  private final TaskExecutor executor;
  private final InAppNotifications inApp;

  public NotificationService(NotificationRepository repo, SmsUtil sms, TaskExecutor applicationTaskExecutor, InAppNotifications inApp) {
    this.repo = repo; this.sms = sms; this.executor = applicationTaskExecutor; this.inApp = inApp;
  }

  /** Creates the rows for every event in the catalogue that is not in the database yet (new events appear without a migration). */
  @Override
  public void run(ApplicationArguments args) {
    for (Event e : NotificationCatalog.EVENTS) {
      repo.seedEvent(e.key(), !NotificationCatalog.IN_APP_ONLY.contains(e.key()));
      repo.seedTemplate(e.key(), "fr", e.fr());
      repo.seedTemplate(e.key(), "en", e.en());
    }
  }

  /** Tells one person (a staff member), after the current transaction commits. */
  public void send(String event, String recipientId, Map<String, String> params, String relatedType, String relatedId) {
    queue(event, () -> repo.person(recipientId).stream().map(p -> prepare(event, p, params, relatedType, relatedId)).toList());
  }

  /** Tells every staff member who receives approval alerts for this event (never the person who made the request). */
  public void notifyApprovers(String event, String requesterId, Map<String, String> params, String relatedType, String relatedId) {
    List<String> roles = NotificationCatalog.get(event).audience() == NotificationAudience.APPROVERS_FINANCE ? List.of("ADMIN") : List.of("ADMIN", "INVENTORY_MANAGER");
    queue(event, () -> repo.approvers(roles, requesterId).stream().map(p -> prepare(event, p, params, relatedType, relatedId)).toList());
  }

  /** Tells every seller assigned to a sales location. */
  public void notifySellersAt(String event, String operationId, Map<String, String> params, String relatedType, String relatedId) {
    queue(event, () -> repo.sellersAt(operationId).stream().map(p -> prepare(event, p, params, relatedType, relatedId)).toList());
  }

  /**
   * Sends a one-time code right now and says how it went (the caller needs to know). {@code phone} is where the code
   * goes, which for payout numbers is not the person's own number. The code is never stored.
   */
  public SmsUtil.Result sendCode(String event, String recipientId, String name, String phone, String language, Map<String, String> params) {
    try {
      Prepared p = prepare(event, name, recipientId, Phones.normalize(phone), language, params, null, null);
      if (!p.send()) return new SmsUtil.Result(SmsStatus.FAILED, "The code cannot be sent: the SMS channel is off or the number is invalid.");
      return deliver(p);
    } catch (RuntimeException e) {
      log.error("Could not send code {}: {}", event, e.toString());
      return new SmsUtil.Result(SmsStatus.FAILED, e.getMessage());
    }
  }

  /** Tries again to send a message that failed. The text is sent as stored; code messages are never resent. */
  public Map<String, Object> resend(String deliveryId) {
    Map<String, Object> d = repo.delivery(deliveryId).orElseThrow(() -> Validate.notFound("Message not found."));
    Event event = NotificationCatalog.get((String) d.get("event_key"));
    if (event.oneTimeCode()) throw Validate.conflict("Codes cannot be resent. Ask for a new code instead.");
    if (!"FAILED".equals(d.get("status"))) throw Validate.conflict("Only a failed message can be resent.");
    deliver(new Prepared(deliveryId, (String) d.get("phone"), (String) d.get("body"), false, true));
    return repo.delivery(deliveryId).orElseThrow();
  }

  // ----- internals -----

  /** Prepares the messages and sends them after commit; a problem is logged, never thrown, so the caller's action still completes. */
  private void queue(String event, Supplier<List<Prepared>> prepared) {
    try { dispatchAfterCommit(prepared.get()); }
    catch (RuntimeException e) { log.error("Could not queue {}: {}", event, e.toString()); }
  }

  private Prepared prepare(String event, Map<String, Object> person, Map<String, String> params, String relatedType, String relatedId) {
    Prepared p = prepare(event, (String) person.get("full_name"), (String) person.get("id"), Phones.normalize((String) person.get("phone")),
        (String) person.get("language"), withName(params, person), relatedType, relatedId);
    // sellers and customers also get it in the app, whatever happens to the SMS
    if ((Roles.SELLER.equals(person.get("role")) || Roles.CUSTOMER.equals(person.get("role"))) && !NotificationCatalog.get(event).oneTimeCode()) {
      Map<String, String> shown = withName(params, person);
      String lang = "en".equals(person.get("language")) ? "en" : "fr";
      String template = repo.template(event, lang).orElse("en".equals(lang) ? NotificationCatalog.get(event).en() : NotificationCatalog.get(event).fr());
      try { inApp.record((String) person.get("id"), event, NotificationCatalog.render(template, shown), lang, relatedType, relatedId); }
      catch (RuntimeException e) { log.error("Could not record the in-app notification {}: {}", event, e.toString()); }
    }
    return p;
  }

  private Prepared prepare(String eventKey, String name, String recipientId, String phone, String language, Map<String, String> params, String relatedType, String relatedId) {
    Event event = NotificationCatalog.get(eventKey);
    String lang = "en".equals(language) ? "en" : "fr";
    String template = repo.template(eventKey, lang).orElse("en".equals(lang) ? event.en() : event.fr());
    String text = NotificationCatalog.render(template, params);
    Map<String, String> shown = new LinkedHashMap<>(params);
    if (event.oneTimeCode()) shown.put("code", SENSITIVE_CODE);
    String stored = event.oneTimeCode() ? NotificationCatalog.render(template, shown) : text;
    String id = UUID.randomUUID().toString();
    String skip = !event.oneTimeCode() && !repo.smsEnabled(eventKey) ? "SMS is switched off for this event."
        : phone == null ? "No valid mobile number on file." : null;
    repo.insertDelivery(id, eventKey, recipientId, name, phone, lang, stored, skip == null ? "QUEUED" : "SKIPPED", skip, relatedType, relatedId);
    return new Prepared(id, phone, text, event.oneTimeCode(), skip == null);
  }

  private static Map<String, String> withName(Map<String, String> params, Map<String, Object> person) {
    Map<String, String> out = new LinkedHashMap<>(params);
    out.putIfAbsent("name", (String) person.get("full_name"));
    return out;
  }

  private void dispatchAfterCommit(List<Prepared> messages) {
    List<Prepared> toSend = messages.stream().filter(Prepared::send).toList();
    if (toSend.isEmpty()) return;
    Runnable task = () -> toSend.forEach(m -> executor.execute(() -> deliver(m)));
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override public void afterCommit() { task.run(); }
      });
    } else task.run();
  }

  private SmsUtil.Result deliver(Prepared m) {
    SmsUtil.Result r = sms.send(m.phone(), m.text(), m.sensitive());
    try { repo.markAttempt(m.deliveryId(), r.status().name(), r.status() == SmsStatus.SENT ? null : r.detail()); }
    catch (RuntimeException e) { log.error("Could not record the outcome of message {}: {}", m.deliveryId(), e.toString()); }
    return r;
  }

  /** "12 000 FCFA" for message texts. */
  public static String xaf(long amount) { return String.format(Locale.ROOT, "%,d FCFA", amount).replace(',', ' '); }
}
