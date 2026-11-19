package com.lutiano.api.service.staff;

import com.lutiano.api.utils.Rows;
import java.util.LinkedHashMap;
import com.lutiano.api.utils.Lists;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.ImageStorage;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.repository.staff.StaffRepository;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.models.request.staff.StaffRequest;

/**
 * Staff accounts and seller management (A04): account lifecycle, seller role/channel, location assignments
 * with history, deactivation with a reason, and signing an account out of every device.
 */
@Service
public class StaffService {

  private final StaffRepository staff;
  private final OperationRepository operations;
  private final SellerDocumentService documents;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  private final ImageStorage images;
  private final boolean enforceEligibility;

  public StaffService(StaffRepository staff, OperationRepository operations, SellerDocumentService documents,
                      PasswordEncoder encoder, AuditService audit, ImageStorage images, AppProperties.Sellers sellers) {
    this.staff = staff; this.operations = operations; this.documents = documents;
    this.encoder = encoder; this.audit = audit; this.images = images; this.enforceEligibility = sellers.enforceEligibility();
  }

  /** Summary: totals by role over every account. */
  public PageResponse<Map<String, Object>> list(Candidates text, String role, String active, PageRequest req) {
    List<Map<String, Object>> all = staff.findAll();
    List<Map<String, Object>> rows = all.stream().filter(u -> Lists.inCandidates(text, u, "email", "phone")
        && Lists.is(role, u.get("role")) && Lists.is(active, u.get("active"))).toList();
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("total", all.size());
    for (String r : List.of("ADMIN", "INVENTORY_MANAGER", "SELLER")) summary.put(r, Lists.count(all, u -> r.equals(u.get("role"))));
    return PageResponse.slice(rows, req).withSummary(summary).withTruncated(text != null && text.truncated());
  }

  public Map<String, Object> get(String id) {
    return staff.findById(id).orElseThrow(() -> Validate.notFound("Staff member not found."));
  }

  /** Sellers with their locations, 30-day performance, last sale, open shift and eligibility. */
  public PageResponse<Map<String, Object>> sellers(Candidates text, String sellerRole, String active, String eligible, PageRequest req) {
    List<Map<String, Object>> all = staff.sellers(Periods.startOf(Periods.today().minusDays(29)));
    for (Map<String, Object> r : all) r.put("eligibility", documents.eligibility((String) r.get("id")));
    List<Map<String, Object>> rows = all.stream().filter(s -> Lists.inCandidates(text, s, "email", "phone")
        && Lists.is(sellerRole, s.get("seller_role")) && Lists.is(active, s.get("active"))
        && Lists.is(eligible, ((Map<?, ?>) s.get("eligibility")).get("eligible"))).toList();
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("active", Lists.count(all, s -> Rows.truthy(s.get("active"))));
    summary.put("eligible", Lists.count(all, s -> Rows.truthy(s.get("active")) && Rows.truthy(((Map<?, ?>) s.get("eligibility")).get("eligible"))));
    summary.put("attention", Lists.count(all, s -> Rows.truthy(s.get("active")) && (!Rows.truthy(((Map<?, ?>) s.get("eligibility")).get("eligible"))
        || ((Number) s.get("mismatched_operations")).intValue() > 0 || ((Number) s.get("operation_count")).intValue() == 0)));
    summary.put("open_shifts", Lists.count(all, s -> ((Number) s.get("open_shift")).intValue() > 0));
    return PageResponse.slice(rows, req).withSummary(summary).withTruncated(text != null && text.truncated());
  }

  /** Who assigned or removed the seller's locations, newest first. */
  public PageResponse<Map<String, Object>> assignmentHistory(String id, PageRequest req) { get(id); return staff.assignmentHistory(id, req); }

  /** Profile, assignments and recent activity; sellers also get performance, stock, history, documents and eligibility. */
  public Map<String, Object> detail(String id) {
    Map<String, Object> user = get(id);
    Timestamp since = Periods.startOf(Periods.today().minusDays(29));
    List<Map<String, Object>> ops = staff.operationsOf(id);
    String sellerRole = (String) user.get("seller_role");
    for (Map<String, Object> o : ops) o.put("channel_matches", SellerRoles.matches(sellerRole, (String) o.get("type")));
    user.put("operations", ops);
    user.put("performance", staff.performance(id, since));
    if (Roles.SELLER.equals(user.get("role"))) {
      user.put("performanceByOperation", staff.performanceByOperation(id, since));
      user.put("openShift", staff.openShift(id).orElse(null));
      user.put("assignedStock", staff.assignedStock(id));
      user.put("incomingStock", staff.incomingStock(id));
      user.put("documents", documents.list(id));
      user.put("eligibility", documents.eligibility(id));
      user.put("eligibilityEnforced", enforceEligibility);
    }
    return user;
  }

  @Transactional
  public Map<String, Object> create(Actor actor, StaffRequest in) {
    String email = Validate.email(in.email());
    String name = Validate.text(in.fullName(), "Full name", 150);
    String role = Validate.oneOf(in.role(), "Role", Roles.ALL);
    Validate.password(in.password());
    String id = UUID.randomUUID().toString();
    staff.insert(id, email, encoder.encode(in.password()), name, Validate.optional(in.phone(), "Phone", 30), role, sellerRole(role, in.sellerRole()),
        Validate.optional(in.address(), "Address", 255), Validate.optional(in.area(), "Area", 120), language(in.language()));
    audit.log(actor, "STAFF_CREATED", "staff_user", id, name + " (" + role + ")");
    return get(id);
  }

  /**
   * Updates profile and role. Leaving the SELLER role removes all assignments; changing seller role removes
   * assignments to locations of another channel. Every removal is logged.
   */
  @Transactional
  public Map<String, Object> update(Actor actor, String id, StaffRequest in) {
    Map<String, Object> before = get(id);
    String previousRole = (String) before.get("role");
    String role = Validate.oneOf(in.role(), "Role", Roles.ALL);
    if (actor.id().equals(id) && !role.equals(previousRole)) throw Validate.bad("You cannot change your own role.");
    String newSellerRole = sellerRole(role, in.sellerRole());
    staff.update(id, Validate.email(in.email()), Validate.text(in.fullName(), "Full name", 150), Validate.optional(in.phone(), "Phone", 30),
        role, newSellerRole, Validate.optional(in.address(), "Address", 255), Validate.optional(in.area(), "Area", 120), language(in.language()));
    // A new phone must be confirmed again before it can receive sign-in codes.
    if (!Objects.equals(Phones.normalize((String) before.get("phone")), Phones.normalize(in.phone()))) staff.clearMfa(id);
    for (Map<String, Object> o : staff.operationsOf(id)) {
      String opId = (String) o.get("id");
      if (!Roles.SELLER.equals(role)) removeAssignment(actor, id, opId, "Role changed to " + role);
      else if (!SellerRoles.matches(newSellerRole, (String) o.get("type")) && !Objects.equals(newSellerRole, before.get("seller_role")))
        removeAssignment(actor, id, opId, "Seller role changed to " + newSellerRole);
    }
    String details = role.equals(previousRole) ? null : "role " + previousRole + " -> " + role;
    if (!Objects.equals(newSellerRole, before.get("seller_role")) && newSellerRole != null)
      details = (details == null ? "" : details + "; ") + "seller role " + before.get("seller_role") + " -> " + newSellerRole;
    audit.log(actor, "STAFF_UPDATED", "staff_user", id, details);
    return get(id);
  }

  /** Deactivation needs a reason, is blocked while a cash shift is open, and signs the account out everywhere. */
  @Transactional
  public void deactivate(Actor actor, String id, String reason) {
    if (actor.id().equals(id)) throw Validate.bad("You cannot deactivate your own account.");
    get(id);
    String why = Validate.text(reason, "Reason", 500);
    if (staff.hasOpenShift(id)) throw Validate.conflict("This seller has an open cash shift. Ask them to count and close it before deactivating the account.");
    staff.deactivate(id, actor.id(), why);
    audit.log(actor, "STAFF_DEACTIVATED", "staff_user", id, why);
  }

  public void reactivate(Actor actor, String id) {
    if (staff.reactivate(id) != 1) throw Validate.notFound("Staff member not found.");
    audit.log(actor, "STAFF_REACTIVATED", "staff_user", id);
  }

  public String uploadImage(Actor actor, String id, MultipartFile file) throws Exception {
    if (staff.findById(id).isEmpty()) throw Validate.notFound("Staff member not found.");
    String url = images.save(ImageStorage.STAFF, id, file);
    staff.setImageUrl(id, url);
    audit.log(actor, "STAFF_IMAGE_UPDATED", "staff_user", id);
    return url;
  }

  /** Another administrator resets someone's two-step sign-in (lost phone, new number): they confirm their phone again at their next sign-in. */
  @Transactional
  public void resetMfa(Actor actor, String id) {
    if (actor.id().equals(id)) throw Validate.forbidden("Another administrator must reset your two-step sign-in.");
    if (!"ADMIN".equals(get(id).get("role"))) throw Validate.bad("Only administrators use two-step sign-in.");
    staff.clearMfa(id);
    staff.revokeSessions(id);
    audit.log(actor, "MFA_RESET", "staff_user", id);
  }

  private static String language(String value) { return Validate.language(value, "fr"); }

  /** Signs the account out of every device; it must sign in again. */
  public void revokeSessions(Actor actor, String id) {
    if (staff.revokeSessions(id) != 1) throw Validate.notFound("Staff member not found.");
    audit.log(actor, "SESSIONS_REVOKED", "staff_user", id);
  }

  /** A new temporary password also signs the account out everywhere. */
  @Transactional
  public void resetPassword(Actor actor, String id, String password) {
    Validate.password(password);
    if (staff.setPasswordHash(id, encoder.encode(password)) != 1) throw Validate.notFound("Staff member not found.");
    staff.revokeSessions(id);
    audit.log(actor, "PASSWORD_RESET", "staff_user", id);
  }

  /** Sellers may only be assigned to active locations of their own channel (and, if enforced, when eligible). */
  @Transactional
  public void assign(Actor actor, String sellerId, String operationId) {
    Map<String, Object> seller = get(sellerId);
    if (!Roles.SELLER.equals(seller.get("role"))) throw Validate.bad("Only sellers can be assigned to a sales operation.");
    if (!Boolean.TRUE.equals(seller.get("active"))) throw Validate.conflict("Reactivate this seller before assigning a location.");
    Map<String, Object> op = operations.findById(operationId).orElseThrow(() -> Validate.notFound("Sales location not found."));
    if (!Boolean.TRUE.equals(op.get("active"))) throw Validate.conflict("Sellers can only be assigned to active locations.");
    String sellerRole = (String) seller.get("seller_role");
    if (!SellerRoles.matches(sellerRole, (String) op.get("type")))
      throw Validate.conflict(op.get("name") + " is a " + op.get("type") + " location; this seller's role works " + SellerRoles.CHANNEL.get(sellerRole) + " locations.");
    if (enforceEligibility && !Boolean.TRUE.equals(documents.eligibility(sellerId).get("eligible")))
      throw Validate.conflict("This seller is not eligible yet: verify their required documents first.");
    if (staff.assign(sellerId, operationId)) {
      staff.logAssignment(sellerId, operationId, "ASSIGNED", actor.id(), null);
      audit.log(actor, "SELLER_ASSIGNED", "operation", operationId, "seller " + seller.get("full_name"));
    }
  }

  @Transactional
  public void unassign(Actor actor, String sellerId, String operationId) {
    removeAssignment(actor, sellerId, operationId, null);
  }

  public List<Map<String, Object>> assignments() { return staff.assignments(); }

  private void removeAssignment(Actor actor, String sellerId, String operationId, String note) {
    if (staff.unassign(sellerId, operationId)) {
      staff.logAssignment(sellerId, operationId, "UNASSIGNED", actor.id(), note);
      audit.log(actor, "SELLER_UNASSIGNED", "operation", operationId, "seller " + sellerId + (note == null ? "" : " (" + note + ")"));
    }
  }

  private static String sellerRole(String role, String sellerRole) {
    if (!Roles.SELLER.equals(role)) return null;
    return Validate.oneOf(sellerRole, "Seller role", SellerRoles.CHANNEL.keySet());
  }
}
