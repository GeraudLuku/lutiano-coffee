package com.lutiano.api.service.cash;

import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.cash.CashRepository;
import com.lutiano.api.models.request.cash.AcknowledgementRequest;
import com.lutiano.api.models.request.cash.OutflowRequest;
import com.lutiano.api.models.request.cash.RecountRequest;
import com.lutiano.api.models.request.cash.RemittanceRequest;
import com.lutiano.api.models.request.cash.ShiftReviewRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.OutflowCategory;

/**
 * A09 cash custody on top of seller cash shifts:
 * <ul>
 *   <li>Outflows: the seller requests cash for an expense; it leaves the drawer only when an administrator approves.</li>
 *   <li>Remittances: cash handed to a named manager or deposited at a bank leaves the drawer when recorded and stays
 *       OUTSTANDING until the recipient acknowledges the amount received (DISPUTED if it differs).</li>
 *   <li>Variance review: a closed shift whose count differs needs an administrator to accept it or ask for a recount.
 *       Every count is kept.</li>
 * </ul>
 */
@Service
public class CashService {
  public static final Set<String> OUTFLOW_CATEGORIES = Enums.names(OutflowCategory.class);
  public static final String HANDOVER = "HANDOVER", BANK_DEPOSIT = "BANK_DEPOSIT";


  private final CashRepository cash;
  private final CashLedger ledger;
  private final AuditService audit;

  private final References references;
  private final NotificationService notifications;

  public CashService(CashRepository cash, CashLedger ledger, AuditService audit, References references, NotificationService notifications) {
    this.cash = cash; this.ledger = ledger; this.audit = audit; this.references = references; this.notifications = notifications;
  }

  // ----- shifts -----

  public PageResponse<Map<String, Object>> shifts(Actor actor, String status, String reviewStatus, String q, PageRequest req) {
    return cash.shifts(status, reviewStatus, actor.isSeller() ? actor.id() : null, q, req);
  }

  /** A shift's ledger, newest entry first. */
  public PageResponse<Map<String, Object>> ledger(Actor actor, String id, PageRequest req) {
    shiftRow(actor, id);
    return cash.ledgerPage(id, req);
  }

  public Map<String, Object> shift(Actor actor, String id) {
    Map<String, Object> s = shiftRow(actor, id);
    s.put("counts", cash.counts(id));
    s.put("outflows", cash.outflows(null, id, null));
    s.put("remittances", cash.remittances(null, id, null, null));
    return s;
  }

  private Map<String, Object> shiftRow(Actor actor, String id) {
    Map<String, Object> s = cash.shift(Validate.id(id, "Cash shift")).orElseThrow(() -> Validate.notFound("Cash shift not found."));
    if (actor.isSeller() && !actor.id().equals(s.get("seller_id"))) throw Validate.notFound("Cash shift not found.");
    return s;
  }

  /** Accept the variance (RESOLVED) or ask the seller to count again (RECOUNT_REQUESTED). A note is always required. */
  @Transactional
  public Map<String, Object> review(Actor actor, String id, ShiftReviewRequest in) {
    Map<String, Object> s = cash.lockShift(Validate.id(id, "Cash shift")).orElseThrow(() -> Validate.notFound("Cash shift not found."));
    if (!"PENDING_REVIEW".equals(s.get("review_status"))) throw Validate.conflict("This shift is not waiting for a variance review.");
    String decision = Validate.oneOf(in.decision(), "Decision", Set.of("ACCEPT", "RECOUNT"));
    String note = Validate.text(in.note(), "Note", 500);
    String status = "ACCEPT".equals(decision) ? "RESOLVED" : "RECOUNT_REQUESTED";
    cash.review(id, status, actor.id(), note);
    audit.log(actor, "ACCEPT".equals(decision) ? "SHIFT_VARIANCE_ACCEPTED" : "SHIFT_RECOUNT_REQUESTED", "cash_shift", id, note);
    return shift(actor, id);
  }

  /** The seller counts again after a manager asked for it. A matching recount resolves the review. */
  @Transactional
  public Map<String, Object> recount(Actor seller, String id, RecountRequest in) {
    Map<String, Object> s = cash.lockShift(Validate.id(id, "Cash shift")).orElseThrow(() -> Validate.notFound("Cash shift not found."));
    if (!seller.id().equals(s.get("seller_id"))) throw Validate.notFound("Cash shift not found.");
    if (!"RECOUNT_REQUESTED".equals(s.get("review_status"))) throw Validate.conflict("No recount was requested for this shift.");
    if (in.countedCash() == null || in.countedCash() < 0) throw Validate.bad("Enter the counted cash (0 or more).");
    long expected = Rows.asLong(s.get("expected_cash")), counted = in.countedCash(), variance = counted - expected;
    String reason = Validate.optional(in.reason(), "Reason", 500);
    if (variance != 0 && reason == null) throw Validate.bad("A reason is required when counted cash differs from expected cash.");
    cash.recount(id, counted, reason, variance == 0 ? "RESOLVED" : "PENDING_REVIEW");
    if (variance == 0) cash.review(id, "RESOLVED", null, "Recount matched the expected cash.");
    cash.insertCount(id, "RECOUNT", expected, counted, reason, seller.id());
    audit.log(seller, "SHIFT_RECOUNTED", "cash_shift", id, "expected " + expected + ", counted " + counted);
    return shift(seller, id);
  }

  // ----- outflows -----

  @Transactional
  public Map<String, Object> requestOutflow(Actor seller, OutflowRequest in) {
    Map<String, Object> shift = cash.openShiftOf(seller.id()).orElseThrow(() -> Validate.conflict("Open a cash shift first."));
    long amount = Validate.positive(in.amountXaf(), "Amount");
    String category = Validate.oneOf(in.category(), "Category", OUTFLOW_CATEGORIES);
    String description = Validate.text(in.description(), "Description", 500);
    String id = UUID.randomUUID().toString(), reference = references.dated("CO", "cash_outflow_seq");
    cash.insertOutflow(id, reference, (String) shift.get("id"), seller.id(), amount, category, description);
    audit.log(seller, "CASH_OUTFLOW_REQUESTED", "cash_outflow", id, reference + " " + amount + " XAF " + category);
    notifications.notifyApprovers(NotificationCatalog.APPROVAL_CASH_OUTFLOW, seller.id(), Map.of("reference", reference, "amount", NotificationService.xaf(amount), "by", seller.fullName()), "cash_outflow", id);
    return outflow(id);
  }

  /** Cash leaves the drawer now; the shift must still be open and hold the amount. */
  @Transactional
  public Map<String, Object> approveOutflow(Actor actor, String id, String note) {
    Map<String, Object> o = lockOutflow(id);
    String text = Validate.optional(note, "Note", 500);
    long amount = Rows.asLong(o.get("amount_xaf"));
    ledger.post((String) o.get("shift_id"), CashLedger.OUTFLOW, -amount, "OUTFLOW", id, actor.id(), (String) o.get("category"));
    cash.decideOutflow(id, "APPROVED", actor.id(), text);
    audit.log(actor, "CASH_OUTFLOW_APPROVED", "cash_outflow", id, o.get("reference") + " " + amount + " XAF");
    return outflow(id);
  }

  @Transactional
  public Map<String, Object> rejectOutflow(Actor actor, String id, String note) {
    Map<String, Object> o = lockOutflow(id);
    String text = Validate.text(note, "Reason", 500);
    cash.decideOutflow(id, "REJECTED", actor.id(), text);
    audit.log(actor, "CASH_OUTFLOW_REJECTED", "cash_outflow", id, o.get("reference") + ": " + text);
    return outflow(id);
  }

  @Transactional
  public Map<String, Object> cancelOutflow(Actor actor, String id) {
    Map<String, Object> o = lockOutflow(id);
    if (actor.isSeller() && !actor.id().equals(o.get("seller_id"))) throw Validate.notFound("Outflow not found.");
    cash.decideOutflow(id, "CANCELLED", actor.id(), null);
    audit.log(actor, "CASH_OUTFLOW_CANCELLED", "cash_outflow", id, (String) o.get("reference"));
    return outflow(id);
  }

  public PageResponse<Map<String, Object>> outflows(Actor actor, String status, PageRequest req) {
    return cash.outflowPage(status, actor.isSeller() ? actor.id() : null, req);
  }

  // ----- remittances -----

  /** Cash leaves the drawer when the seller records it; it stays OUTSTANDING until the recipient acknowledges it. */
  @Transactional
  public Map<String, Object> recordRemittance(Actor seller, RemittanceRequest in) {
    Map<String, Object> shift = cash.openShiftOf(seller.id()).orElseThrow(() -> Validate.conflict("Open a cash shift first."));
    long amount = Validate.positive(in.amountXaf(), "Amount");
    String method = Validate.oneOf(in.method(), "Method", Set.of(HANDOVER, BANK_DEPOSIT));
    String recipient = null, bankReference = null;
    if (HANDOVER.equals(method)) {
      recipient = Validate.id(in.recipientId(), "Recipient");
      if (!cash.isRecipient(recipient)) throw Validate.bad("Choose an active manager or administrator to receive the cash.");
    } else bankReference = Validate.text(in.bankReference(), "Deposit slip reference", 80);
    String note = Validate.optional(in.note(), "Note", 500);
    long inDrawer = Rows.asLong(shift.get("expected_cash"));
    if (amount > inDrawer) throw Validate.conflict("The drawer holds only " + inDrawer + " XAF, not enough for " + amount + " XAF.");
    String id = UUID.randomUUID().toString(), reference = references.dated("RM", "cash_remittance_seq");
    String shiftId = (String) shift.get("id");
    cash.insertRemittance(id, reference, shiftId, seller.id(), amount, method, recipient, bankReference, note);
    ledger.post(shiftId, CashLedger.REMITTANCE, -amount, "REMITTANCE", id, seller.id(), HANDOVER.equals(method) ? "Handover" : "Bank deposit");
    audit.log(seller, "CASH_REMITTED", "cash_remittance", id, reference + " " + amount + " XAF " + method);
    if (recipient != null) notifications.send(NotificationCatalog.CASH_HANDOVER, recipient, Map.of("amount", NotificationService.xaf(amount), "from", seller.fullName(), "reference", reference), "cash_remittance", id);
    return remittance(id);
  }

  /**
   * Only the named recipient can acknowledge a handover; an administrator acknowledges bank deposits.
   * A different amount marks the remittance DISPUTED and needs a note.
   */
  @Transactional
  public Map<String, Object> acknowledge(Actor actor, String id, AcknowledgementRequest in) {
    Map<String, Object> r = cash.lockRemittance(Validate.id(id, "Remittance")).orElseThrow(() -> Validate.notFound("Remittance not found."));
    if (!"OUTSTANDING".equals(r.get("status"))) throw Validate.conflict("This remittance has already been acknowledged.");
    if (HANDOVER.equals(r.get("method")) ? !actor.id().equals(r.get("recipient_id")) : !actor.is(Roles.ADMIN))
      throw Validate.forbidden(HANDOVER.equals(r.get("method")) ? "Only the person who received the cash can acknowledge it." : "An administrator must confirm bank deposits.");
    if (in.receivedXaf() == null || in.receivedXaf() < 0) throw Validate.bad("Enter the amount received.");
    long amount = Rows.asLong(r.get("amount_xaf")), received = in.receivedXaf();
    String note = received == amount ? Validate.optional(in.note(), "Note", 500) : Validate.text(in.note(), "Explanation of the difference", 500);
    String status = received == amount ? "ACKNOWLEDGED" : "DISPUTED";
    cash.acknowledge(id, status, actor.id(), received, note);
    audit.log(actor, received == amount ? "REMITTANCE_ACKNOWLEDGED" : "REMITTANCE_DISPUTED", "cash_remittance", id, r.get("reference") + ": received " + received + " of " + amount + " XAF");
    return remittance(id);
  }

  public PageResponse<Map<String, Object>> remittances(Actor actor, String status, boolean mine, PageRequest req) {
    return cash.remittancePage(status, actor.isSeller() ? actor.id() : null, mine && !actor.isSeller() ? actor.id() : null, req);
  }

  public List<Map<String, Object>> recipients() { return cash.recipients(); }

  public Map<String, Object> summary() { return cash.summary(); }

  /** The signed-in seller's own cash items that need attention. */
  public Map<String, Object> overview(Actor seller) { return cash.sellerOverview(seller.id()); }

  // ----- helpers -----

  private Map<String, Object> lockOutflow(String id) {
    return Validate.status(cash.lockOutflow(Validate.id(id, "Outflow")).orElseThrow(() -> Validate.notFound("Outflow not found.")),
        "outflow", "changed", "REQUESTED");
  }

  private Map<String, Object> outflow(String id) { return cash.outflow(id).orElseThrow(); }
  private Map<String, Object> remittance(String id) { return cash.remittance(id).orElseThrow(); }


}
