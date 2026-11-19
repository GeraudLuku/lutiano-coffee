package com.lutiano.api.service.loyalty;

import com.lutiano.api.repository.common.References;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.operation.QrCodeService;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.models.request.loyalty.CustomerRequest;
import com.lutiano.api.definitions.enums.CustomerStatus;
import com.lutiano.api.utils.Enums;

/**
 * Minimal loyalty members (A05 Customer Management builds on this). A customer is identified at checkout only by their
 * loyalty QR token, which the customer controls; a phone number typed by a seller never awards points.
 */
@Service
public class CustomerService {
  public static final Set<String> STATUSES = Enums.names(CustomerStatus.class);


  private final PointsRepository points;
  private final LoyaltyRepository config;
  private final AuditService audit;
  private final QrCodeService qr;
  private final References references;

  public CustomerService(PointsRepository points, LoyaltyRepository config, AuditService audit, QrCodeService qr, References references) {
    this.points = points; this.config = config; this.audit = audit; this.qr = qr; this.references = references;
  }

  public PageResponse<Map<String, Object>> search(Candidates text, String tier, String status, PageRequest req) { return points.customers(text, tier, status, req); }

  /** The member's points ledger, newest first. */
  public PageResponse<Map<String, Object>> ledger(String id, PageRequest req) { find(id); return config.ledger(id, null, null, null, req); }

  /** Point batches, latest expiry first. */
  public PageResponse<Map<String, Object>> batches(String id, PageRequest req) { find(id); return points.batches(id, req); }

  public PageResponse<Map<String, Object>> redemptions(String id, PageRequest req) { find(id); return points.redemptions(id, null, null, null, req); }

  public Map<String, Object> detail(String id) {
    Map<String, Object> c = find(id);
    int notice = Rows.asInt(config.settings().get("expiry_notice_days"));
    c.put("expiring_soon", points.expiringSoon(id, Timestamp.from(ZonedDateTime.now(Periods.DOUALA).plusDays(notice).toInstant())));
    c.put("expiry_notice_days", notice);
    c.put("rewards_redeemed", points.completedRedemptions(id));
    c.put("loyalty_token", points.loyaltyToken(id));
    return c;
  }

  @Transactional
  public Map<String, Object> create(Actor actor, CustomerRequest in) {
    String phone = phone(in.phone(), null);
    String id = UUID.randomUUID().toString();
    String code = references.plain("CU", "customer_code_seq");
    points.insertCustomer(id, code, Validate.text(in.fullName(), "Full name", 150), phone, Boolean.TRUE.equals(in.phoneVerified()), UUID.randomUUID().toString(), "BRONZE");
    audit.log(actor, "CUSTOMER_CREATED", "customer", id, code);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> update(Actor actor, String id, CustomerRequest in) {
    find(id);
    points.updateCustomer(id, Validate.text(in.fullName(), "Full name", 150), phone(in.phone(), id), Boolean.TRUE.equals(in.phoneVerified()),
        Validate.oneOf(in.status(), "Status", STATUSES));
    audit.log(actor, "CUSTOMER_UPDATED", "customer", id, in.status());
    return detail(id);
  }

  /** A new QR token invalidates the old one (lost or shared QR). */
  @Transactional
  public Map<String, Object> rotateToken(Actor actor, String id) {
    find(id);
    points.setToken(id, UUID.randomUUID().toString());
    audit.log(actor, "CUSTOMER_QR_REISSUED", "customer", id);
    return detail(id);
  }

  public byte[] qrPng(String id) { find(id); return qr.customerPng(points.loyaltyToken(id)); }

  /** The active customer behind a loyalty QR token, used when a seller scans it at checkout. */
  public Optional<Map<String, Object>> byToken(String token) {
    if (token == null || token.isBlank()) return Optional.empty();
    String raw = token.trim().startsWith(QrCodeService.CUSTOMER_PREFIX) ? token.trim().substring(QrCodeService.CUSTOMER_PREFIX.length()) : token.trim();
    Map<String, Object> c = points.customerByToken(raw).orElseThrow(() -> Validate.bad("This loyalty QR code is not valid. Ask the customer to refresh it."));
    if (!"ACTIVE".equals(c.get("status"))) throw Validate.conflict("This customer's loyalty account is suspended.");
    return Optional.of(c);
  }

  private Map<String, Object> find(String id) {
    return points.customer(Validate.id(id, "Customer")).orElseThrow(() -> Validate.notFound("Customer not found."));
  }

  private String phone(String raw, String exceptId) {
    String p = Validate.text(raw, "Phone", 30).replaceAll("[\\s-]", "");
    if (!p.matches("^\\+?[0-9]{8,15}$")) throw Validate.bad("Enter a valid phone number, e.g. +237 6XX XX XX XX.");
    if (points.phoneTaken(p, exceptId)) throw Validate.conflict("Another customer already uses this phone number.");
    return p;
  }

}
