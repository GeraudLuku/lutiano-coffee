package com.lutiano.api.service.account;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.EvidenceStorage;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.repository.account.SupportRepository;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.SupportStatus;
import com.lutiano.api.definitions.enums.SupportType;

/** Help requests from sellers and customers (a question, an issue, an idea, an account matter), with an optional file, answered by the team. */
@Service
public class SupportService {
  static final Set<String> TYPES = Enums.names(SupportType.class);
  static final Set<String> STATUSES = Enums.names(SupportStatus.class);

  private final SupportRepository requests;
  private final EvidenceStorage files;
  private final References references;
  private final AuditService audit;
  private final NotificationService notifications;

  public SupportService(SupportRepository requests, EvidenceStorage files, References references, AuditService audit, NotificationService notifications) {
    this.requests = requests; this.files = files; this.references = references; this.audit = audit; this.notifications = notifications;
  }

  @Transactional
  public Map<String, Object> create(Actor me, String type, String subject, String description, MultipartFile file) throws Exception {
    String t = Validate.oneOf(type, "Request type", TYPES);
    String s = Validate.text(subject, "Subject", 150), d = Validate.text(description, "Description", 2000);
    EvidenceStorage.Saved saved = file == null || file.isEmpty() ? null : files.save("support/" + me.id(), file, "attachment");
    String reference = references.yearly("SP", "support_request_seq");
    String id = requests.insert(reference, me.id(), Roles.CUSTOMER.equals(me.role()) ? "CUSTOMER" : "STAFF", t, s, d, saved == null ? null : saved.key(), saved == null ? null : saved.fileName(), saved == null ? null : saved.contentType());
    audit.log(me, "SUPPORT_REQUESTED", "support_request", id, reference + ": " + s);
    return requests.find(id).orElseThrow();
  }

  public PageResponse<Map<String, Object>> mine(Actor me, String status, PageRequest req) { return requests.ofUser(me.id(), validStatus(status), req); }

  /** One request; a seller can read only their own. */
  public Map<String, Object> get(Actor actor, String id) {
    Map<String, Object> r = requests.find(Validate.id(id, "Request")).orElseThrow(() -> Validate.notFound("Request not found."));
    if (!actor.is(Roles.ADMIN, Roles.INVENTORY_MANAGER) && !actor.id().equals(r.get("user_id"))) throw Validate.notFound("Request not found.");
    return r;
  }

  public record Attachment(String fileName, String contentType, byte[] bytes) {}

  /** The attached file of a request: the sender or staff. */
  public Attachment attachmentOf(Actor actor, String id) throws Exception {
    Map<String, Object> a = requests.attachment(Validate.id(id, "Request")).orElseThrow(() -> Validate.notFound("This request has no attachment."));
    if (!actor.is(Roles.ADMIN, Roles.INVENTORY_MANAGER) && !actor.id().equals(a.get("user_id"))) throw Validate.notFound("This request has no attachment.");
    var stored = files.load((String) a.get("attachment_key"), "attachment");
    return new Attachment((String) a.get("attachment_name"), (String) a.get("attachment_type"), stored.bytes());
  }

  // ----- the team -----

  /** requester: STAFF or CUSTOMER, to see one kind of sender (null for all). */
  public PageResponse<Map<String, Object>> all(String status, String requester, PageRequest req) {
    return requests.all(validStatus(status), requester == null || requester.isBlank() ? null : Validate.oneOf(requester, "Requester", Set.of("STAFF", "CUSTOMER")), req);
  }

  /** Sets the status and/or writes the reply the seller will read; a reply tells the seller. */
  @Transactional
  public Map<String, Object> reply(Actor admin, String id, String status, String reply) {
    Map<String, Object> r = get(admin, id);
    String st = status == null || status.isBlank() ? (String) r.get("status") : Validate.oneOf(status, "Status", STATUSES);
    String text = reply == null || reply.isBlank() ? null : Validate.text(reply, "Reply", 2000);
    if (text == null && st.equals(r.get("status"))) throw Validate.bad("Write a reply or change the status.");
    requests.reply(id, st, text, admin.id());
    audit.log(admin, "SUPPORT_UPDATED", "support_request", id, r.get("reference") + " → " + st + (text == null ? "" : " (replied)"));
    if (text != null) notifications.send(NotificationCatalog.SUPPORT_REPLIED, (String) r.get("user_id"), Map.of("reference", (String) r.get("reference")), "support_request", id);
    return requests.find(id).orElseThrow();
  }

  private static String validStatus(String status) { return status == null || status.isBlank() ? null : Validate.oneOf(status, "Status", STATUSES); }
}
