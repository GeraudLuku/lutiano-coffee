package com.lutiano.api.service.notification;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.service.staff.SellerRoles;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.notification.AnnouncementRepository;
import com.lutiano.api.models.request.notification.AnnouncementRequest;
import com.lutiano.api.definitions.enums.AnnouncementAudience;
import com.lutiano.api.utils.Enums;

/** A message from an administrator to all sellers, a seller role, the sellers of one sales operation, or the customers who agreed to receive offers. It appears in their app. */
@Service
public class AnnouncementService {
  static final Set<String> AUDIENCES = Enums.names(AnnouncementAudience.class);


  private final AnnouncementRepository announcements;
  private final InAppNotifications inApp;
  private final OperationRepository operations;
  private final AuditService audit;

  public AnnouncementService(AnnouncementRepository announcements, InAppNotifications inApp, OperationRepository operations, AuditService audit) {
    this.announcements = announcements; this.inApp = inApp; this.operations = operations; this.audit = audit;
  }

  @Transactional
  public Map<String, Object> send(Actor admin, AnnouncementRequest in) {
    String title = Validate.text(in.title(), "Title", 150), body = Validate.text(in.body(), "Message", 2000);
    String audience = Validate.oneOf(in.audience(), "Audience", AUDIENCES);
    String role = null, operationId = null;
    if ("ROLE".equals(audience)) role = Validate.oneOf(in.sellerRole(), "Seller role", SellerRoles.CHANNEL.keySet());
    if ("OPERATION".equals(audience)) {
      operationId = Validate.id(in.operationId(), "Sales operation");
      if (operations.findById(operationId).isEmpty()) throw Validate.notFound("Sales operation not found.");
    }
    boolean important = Boolean.TRUE.equals(in.important());
    List<String> recipients = announcements.recipients(audience, role, operationId);
    if (recipients.isEmpty()) throw Validate.conflict("CUSTOMERS".equals(audience) ? "No customer has agreed to receive offers yet." : "No active seller matches this audience.");
    String id = UUID.randomUUID().toString();
    announcements.insert(id, title, body, audience, role, operationId, important, recipients.size(), admin.id());
    for (String r : recipients) inApp.announce(r, "CUSTOMERS".equals(audience) ? "PROMOTIONS" : "SYSTEM", title, body, important, id);
    audit.log(admin, "ANNOUNCEMENT_SENT", "announcement", id, title + " to " + recipients.size() + ("CUSTOMERS".equals(audience) ? " customers" : " sellers") + " (" + audience + ")");
    return find(id);
  }

  public Map<String, Object> find(String id) { return announcements.find(id); }

  /** Sent announcements, newest first. */
  public PageResponse<Map<String, Object>> list(PageRequest req) { return announcements.page(req); }
}
