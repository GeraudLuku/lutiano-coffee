package com.lutiano.api.service.payout;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.EvidenceStorage;
import com.lutiano.api.proxy.ObjectStorage;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.staff.StaffRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.repository.payout.DestinationRepository;
import com.lutiano.api.repository.payout.PayoutRepository;
import com.lutiano.api.models.request.payout.PayoutPayRequest;
import com.lutiano.api.models.request.payout.RecordPayoutRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.PayoutMethod;
import com.lutiano.api.definitions.enums.PayoutStatus;

/**
 * Seller payouts. The company pays outside LUTIANO; this records, evidences and controls it. A payout is made against one
 * finalised period of one seller and may be partial. Payouts above the approval threshold (0 = all of them) wait for an
 * administrator other than the one who recorded them; a paid payout can later be reversed as a correction, which needs a
 * second reviewer under the same rule.
 *
 * <pre>
 *   record -> PENDING_APPROVAL -> APPROVED -> PAID -> (reversal) -> REVERSAL_PENDING -> REVERSED
 *              |  reject/cancel     | cancel    '--- reversal needing no review ----> REVERSED
 *              v                    v
 *          REJECTED / CANCELLED   CANCELLED
 * </pre>
 */
@Service
public class PayoutService {
  public static final String PENDING_APPROVAL = "PENDING_APPROVAL", APPROVED = "APPROVED", PAID = "PAID", REJECTED = "REJECTED",
      CANCELLED = "CANCELLED", REVERSAL_PENDING = "REVERSAL_PENDING", REVERSED = "REVERSED";
  public static final Set<String> STATUSES = Enums.names(PayoutStatus.class);
  public static final String CASH = "CASH";
  public static final Set<String> METHODS = Enums.names(PayoutMethod.class);

  public record EvidenceFile(String fileName, String contentType, byte[] bytes) {}

  private final PayoutRepository payouts;
  private final DestinationRepository destinations;
  private final StaffRepository staff;
  private final EvidenceStorage evidence;
  private final References references;
  private final AuditService audit;
  private final NotificationService notifications;

  public PayoutService(PayoutRepository payouts, DestinationRepository destinations, StaffRepository staff,
                       EvidenceStorage evidence, References references, AuditService audit, NotificationService notifications) {
    this.payouts = payouts; this.destinations = destinations; this.staff = staff; this.evidence = evidence; this.references = references; this.audit = audit; this.notifications = notifications;
  }

  // ----- queries -----

  /** Money owed to sellers across every finalised period, and the payouts waiting for someone. */
  public Map<String, Object> overview() {
    Map<String, Object> out = new LinkedHashMap<>(payouts.balanceTotals());
    out.putAll(payouts.workload(Periods.startOf(Periods.today().withDayOfMonth(1))));
    out.put("destinations_pending", destinations.pendingCount());
    out.put("approval_threshold_xaf", payouts.approvalThreshold());
    return out;
  }

  public PageResponse<Map<String, Object>> balances(String q, boolean owedOnly, PageRequest req) { return payouts.balances(q, owedOnly, req); }

  /** A seller's balance with their current payout numbers. */
  public Map<String, Object> sellerBalance(String sellerId) {
    Map<String, Object> out = new LinkedHashMap<>(payouts.balance(Validate.id(sellerId, "Seller")).orElseThrow(() -> Validate.notFound("Seller not found.")));
    out.put("destinations", destinations.current(sellerId));
    return out;
  }

  public PageResponse<Map<String, Object>> periods(String sellerId, boolean openOnly, PageRequest req) {
    return payouts.periods(Validate.id(sellerId, "Seller"), openOnly, req);
  }

  public PageResponse<Map<String, Object>> search(LocalDate from, LocalDate to, String status, String method, String sellerId, Candidates text, PageRequest req) {
    if (status != null && !status.isBlank()) Validate.oneOf(status, "Status", STATUSES);
    if (method != null && !method.isBlank()) Validate.oneOf(method, "Method", METHODS);
    return payouts.search(Periods.startOrNull(from), Periods.endOrNull(to), status, method, sellerId, text, req);
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> p = find(id);
    p.put("events", payouts.events(id));
    p.put("evidence", payouts.evidence(id));
    return p;
  }

  public long approvalThreshold() { return payouts.approvalThreshold(); }

  public long setApprovalThreshold(Actor actor, Long xaf) {
    if (xaf == null || xaf < 0) throw Validate.bad("The threshold must be zero or more (whole FCFA). Zero means every payout needs a second approver.");
    long before = payouts.approvalThreshold();
    payouts.setApprovalThreshold(xaf, actor.id());
    audit.log(actor, "PAYOUT_THRESHOLD_CHANGED", "payout_setting", null, before + " -> " + xaf + " XAF");
    return xaf;
  }

  // ----- workflow -----

  @Transactional
  public Map<String, Object> record(Actor actor, RecordPayoutRequest in) {
    String sellerId = Validate.id(in.sellerId(), "Seller"), periodId = Validate.id(in.periodId(), "Period");
    String method = Validate.oneOf(in.method(), "Method", METHODS);
    Map<String, Object> statement = payouts.lockStatement(sellerId, periodId)
        .orElseThrow(() -> Validate.conflict("This seller has no finalised earnings for that period."));
    long remaining = payouts.remaining(sellerId, periodId, Rows.asLong(statement.get("payable_xaf")));
    if (remaining <= 0) throw Validate.conflict("Nothing is left to pay for this period: it is fully paid, or the remaining amount is already in payouts.");
    long amount = Validate.positive(in.amountXaf(), "Amount");
    if (amount > remaining) throw Validate.bad("The payout cannot exceed " + remaining + " XAF, what is still owed for this period.");
    Map<String, Object> destination = null;
    if (!CASH.equals(method)) {
      destination = destinations.verified(sellerId, method).orElseThrow(() -> Validate.conflict(
          "This seller has no verified " + label(method) + " number. Add one under Payout numbers and have another administrator verify it."));
    }
    boolean needsApproval = amount > payouts.approvalThreshold();
    String id = UUID.randomUUID().toString(), reference = references.dated("PO", "payout_reference_seq");
    payouts.insert(id, reference, sellerId, periodId, amount, method, destination, needsApproval ? PENDING_APPROVAL : APPROVED, needsApproval,
        Validate.optional(in.note(), "Note", 500), actor.id());
    event(id, "RECORDED", null, needsApproval ? PENDING_APPROVAL : APPROVED, actor, amount + " XAF by " + label(method));
    audit.log(actor, "PAYOUT_RECORDED", "payout", id, reference + " " + amount + " XAF to " + seller(sellerId));
    if (needsApproval) askApprovers(NotificationCatalog.APPROVAL_PAYOUT, actor, reference, amount, id);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> p = lockIn(id, "approved", PENDING_APPROVAL);
    if (actor.id().equals(p.get("requested_by"))) throw Validate.forbidden("Another administrator must approve a payout that you recorded.");
    String text = Validate.optional(note, "Note", 500);
    payouts.decide(id, APPROVED, actor.id(), text);
    event(id, "APPROVED", PENDING_APPROVAL, APPROVED, actor, text);
    audit.log(actor, "PAYOUT_APPROVED", "payout", id, (String) p.get("reference"));
    tellSeller(NotificationCatalog.PAYOUT_APPROVED, p);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String note) {
    Map<String, Object> p = lockIn(id, "rejected", PENDING_APPROVAL);
    if (actor.id().equals(p.get("requested_by"))) throw Validate.forbidden("Another administrator must decide a payout that you recorded. You can cancel it instead.");
    String reason = Validate.text(note, "Reason", 500);
    payouts.decide(id, REJECTED, actor.id(), reason);
    event(id, "REJECTED", PENDING_APPROVAL, REJECTED, actor, reason);
    audit.log(actor, "PAYOUT_REJECTED", "payout", id, p.get("reference") + ": " + reason);
    return detail(id);
  }

  /** Withdraws a payout that has not been paid; its amount becomes available again. */
  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String note) {
    Map<String, Object> p = lockIn(id, "cancelled", PENDING_APPROVAL, APPROVED);
    String reason = Validate.optional(note, "Reason", 500);
    payouts.setStatus(id, CANCELLED);
    event(id, "CANCELLED", (String) p.get("status"), CANCELLED, actor, reason);
    audit.log(actor, "PAYOUT_CANCELLED", "payout", id, (String) p.get("reference"));
    return detail(id);
  }

  /** Confirms the money was handed over. Mobile money needs the provider's transaction reference, which can only be used once. */
  @Transactional
  public Map<String, Object> pay(Actor actor, String id, PayoutPayRequest in) {
    Map<String, Object> p = lockIn(id, "paid", APPROVED);
    String method = (String) p.get("method");
    String ref = null;
    if (!CASH.equals(method)) {
      ref = Validate.text(in == null ? null : in.providerReference(), "Provider transaction reference", 80);
      Map<String, Object> d = destinations.find((String) p.get("destination_id")).orElse(null);
      if (d == null || !"VERIFIED".equals(d.get("status")))
        throw Validate.conflict("The seller's " + label(method) + " number has changed since this payout was recorded. Cancel it and record it again.");
    }
    String note = Validate.optional(in == null ? null : in.note(), "Note", 500);
    try { payouts.markPaid(id, ref, actor.id(), note); }
    catch (DuplicateKeyException e) { throw Validate.conflict("That provider reference was already used for another payout."); }
    event(id, "PAID", APPROVED, PAID, actor, ref != null ? "Reference " + ref : note);
    audit.log(actor, "PAYOUT_PAID", "payout", id, p.get("reference") + " " + p.get("amount_xaf") + " XAF by " + label(method));
    tellSeller(NotificationCatalog.PAYOUT_PAID, p);
    return detail(id);
  }

  /**
   * Corrects a paid payout (it never reached the seller, or was recorded wrongly): its amount becomes payable again and a
   * correct payout can be recorded. A payout that needed approval needs a second reviewer for its reversal too.
   */
  @Transactional
  public Map<String, Object> requestReversal(Actor actor, String id, String reason) {
    Map<String, Object> p = lockIn(id, "reversed", PAID);
    String text = Validate.text(reason, "Reason", 500);
    if (Rows.truthy(p.get("requires_approval"))) {
      payouts.requestReversal(id, text, actor.id());
      event(id, "REVERSAL_REQUESTED", PAID, REVERSAL_PENDING, actor, text);
      audit.log(actor, "PAYOUT_REVERSAL_REQUESTED", "payout", id, p.get("reference") + ": " + text);
      askApprovers(NotificationCatalog.APPROVAL_PAYOUT_REVERSAL, actor, (String) p.get("reference"), Rows.asLong(p.get("amount_xaf")), id);
    } else {
      payouts.reverse(id, text, actor.id(), actor.id());
      event(id, "REVERSED", PAID, REVERSED, actor, text);
      audit.log(actor, "PAYOUT_REVERSED", "payout", id, p.get("reference") + ": " + text);
      tellSeller(NotificationCatalog.PAYOUT_REVERSED, p);
    }
    return detail(id);
  }

  @Transactional
  public Map<String, Object> approveReversal(Actor actor, String id, String note) {
    Map<String, Object> p = lockIn(id, "reversed", REVERSAL_PENDING);
    if (actor.id().equals(p.get("reversal_requested_by"))) throw Validate.forbidden("Another administrator must approve a reversal that you requested.");
    payouts.reverse(id, null, null, actor.id());
    event(id, "REVERSED", REVERSAL_PENDING, REVERSED, actor, Validate.optional(note, "Note", 500));
    audit.log(actor, "PAYOUT_REVERSED", "payout", id, (String) p.get("reference"));
    tellSeller(NotificationCatalog.PAYOUT_REVERSED, p);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> rejectReversal(Actor actor, String id, String note) {
    Map<String, Object> p = lockIn(id, "kept", REVERSAL_PENDING);
    if (actor.id().equals(p.get("reversal_requested_by"))) throw Validate.forbidden("Another administrator must decide a reversal that you requested.");
    String reason = Validate.text(note, "Reason", 500);
    payouts.clearReversal(id);
    event(id, "REVERSAL_REJECTED", REVERSAL_PENDING, PAID, actor, reason);
    audit.log(actor, "PAYOUT_REVERSAL_REJECTED", "payout", id, p.get("reference") + ": " + reason);
    return detail(id);
  }

  // ----- evidence -----

  @Transactional
  public Map<String, Object> addEvidence(Actor actor, String id, MultipartFile file) throws Exception {
    Map<String, Object> p = find(id);
    if (REJECTED.equals(p.get("status")) || CANCELLED.equals(p.get("status"))) throw Validate.conflict("Evidence cannot be added to a rejected or cancelled payout.");
    EvidenceStorage.Saved saved = evidence.save("payouts/" + id, file, "evidence");
    payouts.insertEvidence(saved.id(), id, saved.key(), saved.fileName(), saved.contentType(), saved.size(), actor.id());
    event(id, "EVIDENCE_ADDED", null, null, actor, saved.fileName());
    audit.log(actor, "PAYOUT_EVIDENCE_ADDED", "payout", id, p.get("reference") + ": " + saved.fileName());
    return detail(id);
  }

  public EvidenceFile evidence(String id, String evidenceId) throws Exception {
    Map<String, Object> row = payouts.evidenceFile(id, evidenceId).orElseThrow(() -> Validate.notFound("Evidence not found."));
    ObjectStorage.StoredObject obj = evidence.load((String) row.get("object_key"), "evidence");
    return new EvidenceFile((String) row.get("file_name"), (String) row.get("content_type"), obj.bytes());
  }

  // ----- helpers -----

  private Map<String, Object> find(String id) {
    return payouts.find(Validate.id(id, "Payout")).orElseThrow(() -> Validate.notFound("Payout not found."));
  }

  private Map<String, Object> lockIn(String id, String verb, String... allowed) {
    return Validate.status(payouts.lock(Validate.id(id, "Payout")).orElseThrow(() -> Validate.notFound("Payout not found.")), "payout", verb, allowed);
  }

  /** Tells the seller about their payout (amount, method and reference as saved on it). */
  private void tellSeller(String event, Map<String, Object> p) {
    notifications.send(event, (String) p.get("seller_id"), Map.of("amount", NotificationService.xaf(Rows.asLong(p.get("amount_xaf"))),
        "method", label((String) p.get("method")), "reference", (String) p.get("reference")), "payout", (String) p.get("id"));
  }

  private void askApprovers(String event, Actor actor, String reference, long amount, String id) {
    notifications.notifyApprovers(event, actor.id(), Map.of("reference", reference, "amount", NotificationService.xaf(amount), "by", actor.fullName()), "payout", id);
  }

  private String seller(String sellerId) { return staff.findById(sellerId).map(s -> (String) s.get("full_name")).orElse(sellerId); }

  static String label(String method) {
    return switch (method) { case CASH -> "cash"; case DestinationService.MTN_MOMO -> "MTN MoMo"; default -> "Orange Money"; };
  }

  private void event(String payoutId, String type, String from, String to, Actor actor, String details) {
    payouts.insertEvent(payoutId, type, from, to, actor.id(), details);
  }
}
