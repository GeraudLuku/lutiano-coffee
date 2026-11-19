package com.lutiano.api.service.refund;

import com.lutiano.api.service.earnings.CommissionService;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.loyalty.PointsService;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.inventory.AdjustmentService;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.proxy.EvidenceStorage;
import com.lutiano.api.proxy.ObjectStorage;
import com.lutiano.api.service.cash.ShiftService;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.utils.Candidates;
import java.sql.Timestamp;
import com.lutiano.api.repository.refund.RefundRepository;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.models.request.refund.RefundPayRequest;
import com.lutiano.api.models.request.refund.RefundRequest;
import com.lutiano.api.models.request.refund.StockReturnLineRequest;
import com.lutiano.api.models.request.refund.StockReturnRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.RefundReason;
import com.lutiano.api.definitions.enums.RefundStatus;

/**
 * A08 refunds. Request, approval and payment are separate steps:
 * <ul>
 *   <li>The total of requested, approved and paid refunds can never exceed the sale total.</li>
 *   <li>The requester cannot approve their own refund (lutiano.refunds.second-approver).</li>
 *   <li>Refunds are paid in cash from an open cash shift holding enough cash, or from a documented authorised advance
 *       whose unused cash is recorded as returned. Without a fund the refund stays APPROVED (unpaid).</li>
 *   <li>The recipient must acknowledge receiving the cash.</li>
 *   <li>Paying a refund never restores stock; returned goods are a separate CUSTOMER_RETURN adjustment that needs approval.</li>
 * </ul>
 */
@Service
public class RefundService {
  public static final String REQUESTED = "REQUESTED", APPROVED = "APPROVED", PAID = "PAID", REJECTED = "REJECTED", CANCELLED = "CANCELLED";
  public static final Set<String> STATUSES = Enums.names(RefundStatus.class);
  public static final Set<String> REASONS = Enums.names(RefundReason.class);
  public static final String SHIFT = "SHIFT", ADVANCE = "ADVANCE";

  public record EvidenceFile(String fileName, String contentType, byte[] bytes) {}

  private final RefundRepository refunds;
  private final PointsService loyalty;
  private final References references;
  private final CommissionService commissions;
  private final ShiftService shifts;
  private final AdjustmentService adjustments;
  private final EvidenceStorage evidence;
  private final AuditService audit;
  private final NotificationService notifications;
  private final boolean secondApprover;

  public RefundService(RefundRepository refunds, ShiftService shifts, AdjustmentService adjustments, EvidenceStorage evidence, AuditService audit, PointsService loyalty, References references, CommissionService commissions, NotificationService notifications,
                       AppProperties.Refunds policy) {
    this.refunds = refunds; this.shifts = shifts; this.adjustments = adjustments; this.evidence = evidence; this.audit = audit;
    this.notifications = notifications; this.secondApprover = policy.secondApprover(); this.loyalty = loyalty; this.references = references; this.commissions = commissions;
  }

  public boolean requiresSecondApprover() { return secondApprover; }

  // ----- queries -----

  public PageResponse<Map<String, Object>> search(LocalDate from, LocalDate to, String status, String operationId, String sellerId, Candidates text, PageRequest req) {
    if (status != null && !status.isBlank()) Validate.oneOf(status, "Status", STATUSES);
    return refunds.search(Periods.startOrNull(from), Periods.endOrNull(to), status, operationId, sellerId, text, req);
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> r = find(id);
    r.put("events", refunds.events(id));
    r.put("evidence", refunds.evidence(id));
    r.put("stock_returns", refunds.stockReturns(id));
    r.put("returnable", returnable((String) r.get("sale_id")));
    r.put("sale_refundable_xaf", Rows.asLong(r.get("sale_total_xaf")) - refunds.committedForSale((String) r.get("sale_id")));
    return r;
  }

  public List<Map<String, Object>> forSale(String saleId) { return refunds.forSale(saleId); }

  /** What can still be refunded on a sale, found by id or reference (used before creating a refund). */
  public Map<String, Object> saleSummary(String saleId, String saleReference) {
    String id = saleId != null && !saleId.isBlank() ? Validate.id(saleId, "Sale")
        : refunds.saleIdByReference(Validate.text(saleReference, "Sale reference", 30).toUpperCase())
            .orElseThrow(() -> Validate.notFound("No sale with that reference."));
    Map<String, Object> sale = refunds.findSale(id).orElseThrow(() -> Validate.notFound("Sale not found."));
    long committed = refunds.committedForSale(id);
    Map<String, Object> out = new LinkedHashMap<>(sale);
    out.put("committed_xaf", committed);
    out.put("refundable_xaf", Rows.asLong(sale.get("total_xaf")) - committed);
    out.put("refunds", refunds.forSale(id));
    return out;
  }

  // ----- workflow -----

  @Transactional
  public Map<String, Object> request(Actor actor, RefundRequest in) {
    Validate.id(in.saleId(), "Sale");
    Map<String, Object> sale = refunds.lockSale(in.saleId()).orElseThrow(() -> Validate.notFound("Sale not found."));
    long total = Rows.asLong(sale.get("total_xaf"));
    long remaining = total - refunds.committedForSale(in.saleId());
    if (remaining <= 0) throw Validate.conflict("This sale has already been fully refunded or has refunds waiting for the full amount.");
    if (in.amountXaf() == null || in.amountXaf() <= 0) throw Validate.bad("Enter a refund amount greater than zero.");
    long amount = in.amountXaf();
    if (amount > remaining) throw Validate.bad("The refund cannot exceed " + remaining + " XAF, the amount of this sale not yet refunded.");
    String reason = Validate.oneOf(in.reason(), "Reason", REASONS);
    String note = "OTHER".equals(reason) ? Validate.text(in.note(), "Explanation", 500) : Validate.optional(in.note(), "Note", 500);
    String type = amount == total ? "FULL" : "PARTIAL";
    String id = UUID.randomUUID().toString();
    String reference = references.dated("RF", "refund_reference_seq");
    // A sale linked to a loyalty member refunds that member; typed details are only for anonymous sales.
    boolean member = sale.get("customer_id") != null;
    refunds.insert(id, reference, sale, amount, type, reason, note,
        member ? (String) sale.get("customer_name") : Validate.optional(in.customerName(), "Customer name", 150),
        member ? (String) sale.get("customer_phone") : Validate.optional(in.customerPhone(), "Customer phone", 30), actor.id());
    event(id, "REQUESTED", null, REQUESTED, actor, type.toLowerCase() + " refund of " + amount + " XAF on " + sale.get("reference"));
    audit.log(actor, "REFUND_REQUESTED", "refund", id, reference + " " + amount + " XAF on " + sale.get("reference"));
    notifications.notifyApprovers(NotificationCatalog.APPROVAL_REFUND, actor.id(), Map.of("reference", reference, "amount", NotificationService.xaf(amount), "by", actor.fullName()), "refund", id);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> r = lockIn(id, "approved", REQUESTED);
    if (secondApprover && actor.id().equals(r.get("requested_by"))) throw Validate.forbidden("Another administrator must approve a refund that you requested.");
    String text = Validate.optional(note, "Note", 500);
    refunds.decide(id, APPROVED, actor.id(), text);
    event(id, "APPROVED", REQUESTED, APPROVED, actor, text);
    audit.log(actor, "REFUND_APPROVED", "refund", id, (String) r.get("reference"));
    return detail(id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    Map<String, Object> r = lockIn(id, "rejected", REQUESTED);
    if (secondApprover && actor.id().equals(r.get("requested_by"))) throw Validate.forbidden("Another administrator must decide a refund that you requested. You can cancel it instead.");
    String text = Validate.text(reason, "Reason", 500);
    refunds.decide(id, REJECTED, actor.id(), text);
    event(id, "REJECTED", REQUESTED, REJECTED, actor, text);
    audit.log(actor, "REFUND_REJECTED", "refund", id, r.get("reference") + ": " + text);
    return detail(id);
  }

  /** Withdraws a refund that has not been paid. */
  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String reason) {
    Map<String, Object> r = lockIn(id, "cancelled", REQUESTED, APPROVED);
    String text = Validate.text(reason, "Reason", 500);
    refunds.cancel(id);
    event(id, "CANCELLED", (String) r.get("status"), CANCELLED, actor, text);
    audit.log(actor, "REFUND_CANCELLED", "refund", id, r.get("reference") + ": " + text);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> pay(Actor actor, String id, RefundPayRequest in) {
    Map<String, Object> r = lockIn(id, "paid", APPROVED);
    long amount = Rows.asLong(r.get("amount_xaf"));
    String source = Validate.oneOf(in.fundSource(), "Refund fund", Set.of(SHIFT, ADVANCE));
    String recipient = Validate.text(in.recipientName(), "Recipient name", 150);
    if (!Boolean.TRUE.equals(in.recipientAcknowledged())) throw Validate.bad("The recipient must acknowledge receiving the cash.");
    String note = Validate.optional(in.note(), "Note", 500);
    String details;
    if (SHIFT.equals(source)) {
      String shiftId = Validate.id(in.shiftId(), "Cash shift");
      Map<String, Object> shift = shifts.payRefund(shiftId, id, actor.id(), amount);
      refunds.markPaid(id, SHIFT, shiftId, null, null, null, recipient, actor.id(), note);
      details = amount + " XAF from " + shift.get("seller_name") + "'s cash shift, handed to " + recipient;
    } else {
      String reference = Validate.text(in.advanceReference(), "Advance reference", 80);
      if (in.advanceAmountXaf() == null || in.advanceAmountXaf() < amount)
        throw Validate.conflict("The authorised advance must cover the refund (" + amount + " XAF). Without a fund the refund stays approved but unpaid.");
      long advance = in.advanceAmountXaf(), unused = advance - amount;
      long returned = in.advanceReturnedXaf() == null ? -1 : in.advanceReturnedXaf();
      if (returned != unused) throw Validate.bad("Record the unused advance returned: " + unused + " XAF.");
      refunds.markPaid(id, ADVANCE, null, reference, advance, returned, recipient, actor.id(), note);
      details = amount + " XAF from advance " + reference + " (" + advance + " XAF, " + returned + " XAF returned), handed to " + recipient;
    }
    event(id, "PAID", APPROVED, PAID, actor, details);
    String saleId = (String) r.get("sale_id");
    Map<String, Object> sale = refunds.findSale(saleId).orElseThrow();
    long refunded = refunds.paidForSale(saleId), saleTotal = Rows.asLong(sale.get("total_xaf"));
    loyalty.reverseForRefund(saleId, refunded, saleTotal, id, (String) r.get("reference"), actor);
    commissions.reverseForRefund(saleId, ((Timestamp) sale.get("created_at")).toInstant().atZone(Periods.DOUALA).toLocalDate(),
        refunded, saleTotal, id, (String) r.get("reference"), actor);
    event(id, "RECIPIENT_ACKNOWLEDGED", null, null, actor, recipient + " acknowledged receiving " + amount + " XAF");
    audit.log(actor, "REFUND_PAID", "refund", id, r.get("reference") + ": " + details);
    return detail(id);
  }

  // ----- evidence -----

  @Transactional
  public Map<String, Object> addEvidence(Actor actor, String id, MultipartFile file) throws Exception {
    Map<String, Object> r = find(id);
    EvidenceStorage.Saved saved = evidence.save("refunds/" + id, file, "evidence");
    refunds.insertEvidence(saved.id(), id, saved.key(), saved.fileName(), saved.contentType(), saved.size(), actor.id());
    event(id, "EVIDENCE_ADDED", null, null, actor, saved.fileName());
    audit.log(actor, "REFUND_EVIDENCE_ADDED", "refund", id, r.get("reference") + ": " + saved.fileName());
    return detail(id);
  }

  public EvidenceFile evidence(String id, String evidenceId) throws Exception {
    Map<String, Object> row = refunds.evidenceFile(id, evidenceId).orElseThrow(() -> Validate.notFound("Evidence not found."));
    ObjectStorage.StoredObject obj = evidence.load((String) row.get("object_key"), "evidence");
    return new EvidenceFile((String) row.get("file_name"), (String) row.get("content_type"), obj.bytes());
  }

  // ----- returned stock -----

  /** Requests CUSTOMER_RETURN adjustments at the sale's location; stock changes only when another manager approves them. */
  @Transactional
  public Map<String, Object> requestStockReturn(Actor actor, String id, StockReturnRequest in) {
    Map<String, Object> r = find(id);
    if (!PAID.equals(r.get("status")) && !APPROVED.equals(r.get("status")))
      throw Validate.conflict("Returned stock can be recorded once the refund is approved.");
    if (in == null || in.lines() == null || in.lines().isEmpty()) throw Validate.bad("Choose at least one returned product.");
    String note = Validate.text(in.note(), "Condition of the returned goods", 500);
    Map<String, Map<String, Object>> allowed = new LinkedHashMap<>();
    for (Map<String, Object> p : returnable((String) r.get("sale_id"))) allowed.put((String) p.get("product_id"), p);
    for (StockReturnLineRequest line : in.lines()) {
      Map<String, Object> p = allowed.get(line.productId());
      if (p == null) throw Validate.bad("That product was not part of this sale.");
      if (!Boolean.TRUE.equals(p.get("tracked"))) throw Validate.bad(p.get("product_name") + " is not stock-tracked, so it cannot be returned to stock.");
      int qty = Validate.positive(line.quantity(), "Quantity");
      if (qty > Rows.asInt(p.get("returnable"))) throw Validate.bad("At most " + p.get("returnable") + " " + p.get("product_name") + " can still be returned.");
    }
    for (StockReturnLineRequest line : in.lines()) {
      Map<String, Object> adj = adjustments.request(actor, (String) r.get("operation_id"), line.productId(), line.quantity(),
          AdjustmentService.CUSTOMER_RETURN, "Returned with refund " + r.get("reference") + ": " + note);
      refunds.linkAdjustment((String) adj.get("id"), id);
      event(id, "STOCK_RETURN_REQUESTED", null, null, actor, line.quantity() + " × " + allowed.get(line.productId()).get("product_name"));
    }
    return detail(id);
  }

  /** Per product on the sale: sold, already returned (pending or applied) and still returnable. */
  private List<Map<String, Object>> returnable(String saleId) {
    Map<String, Integer> returned = refunds.returnedQuantities(saleId);
    return refunds.soldProducts(saleId).stream().map(p -> {
      Map<String, Object> out = new LinkedHashMap<>();
      int sold = Rows.asInt(p.get("quantity")), back = returned.getOrDefault((String) p.get("product_id"), 0);
      out.put("product_id", p.get("product_id"));
      out.put("product_name", p.get("product_name"));
      out.put("tracked", p.get("track_stock") == null || Rows.truthy(p.get("track_stock")));
      out.put("sold", sold);
      out.put("returned", back);
      out.put("returnable", Math.max(0, sold - back));
      return out;
    }).toList();
  }

  // ----- helpers -----

  private Map<String, Object> find(String id) {
    Validate.id(id, "Refund");
    return refunds.find(id).orElseThrow(() -> Validate.notFound("Refund not found."));
  }

  private Map<String, Object> lockIn(String id, String verb, String... allowed) {
    return Validate.status(refunds.lock(Validate.id(id, "Refund")).orElseThrow(() -> Validate.notFound("Refund not found.")), "refund", verb, allowed);
  }

  private void event(String refundId, String type, String from, String to, Actor actor, String details) {
    refunds.insertEvent(refundId, type, from, to, actor.id(), details);
  }
}
