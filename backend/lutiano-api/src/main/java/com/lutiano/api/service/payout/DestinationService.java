package com.lutiano.api.service.payout;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.staff.StaffRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.lutiano.api.utils.Candidates;
import java.util.LinkedHashMap;
import com.lutiano.api.repository.payout.DestinationRepository;
import com.lutiano.api.models.request.payout.DestinationRequest;
import com.lutiano.api.definitions.enums.DestinationStatus;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.PayoutProvider;

/**
 * Mobile money numbers sellers are paid to. Adding a number sends a code to it by SMS; entering that code proves the
 * seller controls the number and verifies it, which replaces the seller's previous number for that provider.
 * Only verified numbers can be paid to.
 */
@Service
public class DestinationService {
  public static final String MTN_MOMO = "MTN_MOMO", ORANGE_MONEY = "ORANGE_MONEY";
  public static final Set<String> PROVIDERS = Enums.names(PayoutProvider.class);
  public static final Set<String> STATUSES = Enums.names(DestinationStatus.class);


  private final DestinationRepository destinations;
  private final StaffRepository staff;
  private final AuditService audit;
  private final OtpService otp;
  private final NotificationService notifications;

  public DestinationService(DestinationRepository destinations, StaffRepository staff, AuditService audit, OtpService otp, NotificationService notifications) {
    this.destinations = destinations; this.staff = staff; this.audit = audit; this.otp = otp; this.notifications = notifications;
  }

  public PageResponse<Map<String, Object>> search(String status, String provider, String sellerId, Candidates text, PageRequest req) {
    if (status != null && !status.isBlank()) Validate.oneOf(status, "Status", STATUSES);
    if (provider != null && !provider.isBlank()) Validate.oneOf(provider, "Provider", PROVIDERS);
    return destinations.search(status, provider, sellerId, text, req);
  }

  /** Adds the number and sends it a code. If the SMS cannot be sent the number is still added: the code can be sent again. */
  public Map<String, Object> add(Actor actor, DestinationRequest in) {
    String sellerId = Validate.id(in.sellerId(), "Seller");
    Map<String, Object> seller = staff.findById(sellerId).filter(s -> "SELLER".equals(s.get("role"))).orElseThrow(() -> Validate.notFound("Seller not found."));
    String provider = Validate.oneOf(in.provider(), "Provider", PROVIDERS);
    String phone = Phones.require(in.phone(), "Phone number");
    if (destinations.activeNumberExists(sellerId, provider, phone)) throw Validate.conflict("This number is already registered for " + seller.get("full_name") + ".");
    String id = UUID.randomUUID().toString();
    destinations.insert(id, sellerId, provider, phone, Validate.text(in.accountName(), "Account holder name", 150), Validate.optional(in.note(), "Note", 255), actor.id());
    destinations.replaceOthers(sellerId, provider, id, "PENDING");
    audit.log(actor, "PAYOUT_DESTINATION_ADDED", "staff_user", sellerId, provider + " " + phone);
    Map<String, Object> out = new LinkedHashMap<>(get(id));
    try { sendCode(get(id)); out.put("code_sent", true); }
    catch (ResponseStatusException e) { out.put("code_sent", false); out.put("code_error", e.getReason()); }
    return out;
  }

  /** Sends the seller's number a new code. */
  public void resendCode(Actor actor, String id) {
    Map<String, Object> d = Validate.status(get(id), "number", "verified", "PENDING");
    sendCode(d);
    audit.log(actor, "PAYOUT_DESTINATION_CODE_SENT", "staff_user", (String) d.get("seller_id"), d.get("provider") + " " + d.get("phone"));
  }

  private void sendCode(Map<String, Object> d) {
    String sellerId = (String) d.get("seller_id");
    Map<String, Object> seller = staff.findById(sellerId).orElseThrow(() -> Validate.notFound("Seller not found."));
    otp.start("payout-number", (String) d.get("id"), NotificationCatalog.OTP_PAYOUT_NUMBER, sellerId, (String) seller.get("full_name"), (String) d.get("phone"), (String) seller.get("language"), false);
  }

  @Transactional
  public Map<String, Object> verify(Actor actor, String id, String code) {
    Map<String, Object> d = Validate.status(lock(id), "number", "verified", "PENDING");
    otp.verify("payout-number", id, code);
    destinations.setStatus(id, "VERIFIED", actor.id(), null);
    destinations.replaceOthers((String) d.get("seller_id"), (String) d.get("provider"), id, "VERIFIED");
    audit.log(actor, "PAYOUT_DESTINATION_VERIFIED", "staff_user", (String) d.get("seller_id"), d.get("provider") + " " + d.get("phone"));
    tell(NotificationCatalog.PAYOUT_NUMBER_VERIFIED, d, id);
    return get(id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    Map<String, Object> d = Validate.status(lock(id), "number", "rejected", "PENDING");
    destinations.setStatus(id, "REJECTED", actor.id(), Validate.text(reason, "Reason", 255));
    audit.log(actor, "PAYOUT_DESTINATION_REJECTED", "staff_user", (String) d.get("seller_id"), d.get("provider") + " " + d.get("phone"));
    tell(NotificationCatalog.PAYOUT_NUMBER_REJECTED, d, id);
    return get(id);
  }

  private void tell(String event, Map<String, Object> d, String id) {
    notifications.send(event, (String) d.get("seller_id"), Map.of("provider", PayoutService.label((String) d.get("provider")), "phone", (String) d.get("phone")), "payout_destination", id);
  }

  public Map<String, Object> get(String id) {
    return destinations.find(Validate.id(id, "Number")).orElseThrow(() -> Validate.notFound("Payout number not found."));
  }

  private Map<String, Object> lock(String id) {
    return destinations.lock(Validate.id(id, "Number")).orElseThrow(() -> Validate.notFound("Payout number not found."));
  }
}
