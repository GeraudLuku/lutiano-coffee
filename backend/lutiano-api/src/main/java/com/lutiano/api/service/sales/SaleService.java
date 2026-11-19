package com.lutiano.api.service.sales;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import com.lutiano.api.service.earnings.TargetService;
import com.lutiano.api.repository.earnings.CommissionRepository;
import com.lutiano.api.service.earnings.CommissionService;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import com.lutiano.api.service.loyalty.CustomerService;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.service.loyalty.PointsService;
import com.lutiano.api.repository.refund.RefundRepository;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.inventory.StockLedger;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.service.payment.PaymentService;
import com.lutiano.api.service.product.AddonService;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.service.cash.ShiftService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.service.search.SearchCandidates;
import java.util.HashMap;
import com.lutiano.api.repository.sales.SaleRepository;
import com.lutiano.api.models.request.common.LineItemRequest;
import com.lutiano.api.models.request.sales.SaleRequest;
import com.lutiano.api.definitions.enums.MovementType;
import java.util.LinkedHashMap;
import com.lutiano.api.entity.PricedAddon;

/**
 * Online cash sales. A sale posts exactly once per idempotency key: lines at central prices,
 * stock deducted at the operation, and the amount added to the seller's open cash shift.
 */
@Service
public class SaleService {
  /** paymentMethod defaults to CASH; only usable (available and enabled) methods are accepted. */
  /** `created` is false when an idempotent retry returned the existing sale. */
  public record SaleResult(Map<String, Object> sale, boolean created) {}

  private record MergedLine(String productId, List<String> addonIds, int quantity) {}
  private record PricedLine(String productId, String name, long unitPrice, List<PricedAddon> addons, int quantity) {
    long addonsUnit() { return addons.stream().mapToLong(PricedAddon::priceXaf).sum(); }
    long total() { return Math.multiplyExact(Math.addExact(unitPrice, addonsUnit()), quantity); }
  }

  private final SaleRepository sales;
  private final ProductRepository products;
  private final AddonService addons;
  private final PaymentService payments;
  private final StockLedger ledger;
  private final OperationRepository operations;
  private final ShiftService shifts;
  private final CurrentUser current;
  private final AuditService audit;
  private final RefundRepository refunds;
  private final CustomerService customers;
  private final PointsService loyalty;
  private final PointsRepository loyaltyAwards;
  private final References references;
  private final CommissionService commissions;
  private final CommissionRepository commissionLines;
  private final SearchCandidates candidates;
  private static final Logger log = LoggerFactory.getLogger(SaleService.class);
  private final TargetService targets;

  public SaleService(SaleRepository sales, ProductRepository products, AddonService addons, PaymentService payments, StockLedger ledger, OperationRepository operations,
                     ShiftService shifts, CurrentUser current, AuditService audit, RefundRepository refunds,
                     CustomerService customers, PointsService loyalty, PointsRepository loyaltyAwards, References references,
                     CommissionService commissions, CommissionRepository commissionLines, SearchCandidates candidates, TargetService targets) {
    this.candidates = candidates; this.targets = targets;
    this.sales = sales; this.products = products; this.addons = addons; this.payments = payments; this.ledger = ledger; this.operations = operations;
    this.shifts = shifts; this.current = current; this.audit = audit; this.refunds = refunds; this.customers = customers; this.loyalty = loyalty; this.loyaltyAwards = loyaltyAwards; this.references = references; this.commissions = commissions; this.commissionLines = commissionLines;
  }

  @Transactional
  /**
   * An administrator records a sale for a seller who has an open cash shift (cash goes into that shift). The sale belongs to
   * the seller as if they had made it; the audit log also records who entered it.
   */
  public SaleResult completeOnBehalf(Actor admin, Actor seller, String idempotencyKey, SaleRequest in) {
    SaleResult r = complete(seller, idempotencyKey, in);
    if (r.created()) audit.log(admin, "SALE_RECORDED_FOR_SELLER", "sale", (String) r.sale().get("id"), r.sale().get("reference") + " for " + seller.fullName());
    return r;
  }

  @Transactional
  public SaleResult complete(Actor seller, String idempotencyKey, SaleRequest in) {
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) throw Validate.bad("Idempotency-Key must be 1-120 characters.");
    var previous = sales.findByIdempotencyKey(idempotencyKey);
    if (previous.isPresent()) {
      if (!seller.id().equals(previous.get().get("seller_id"))) throw Validate.conflict("This idempotency key has already been used.");
      return new SaleResult(detail((String) previous.get().get("id")), false);
    }

    String operationId = Validate.id(in.operationId(), "Sales operation");
    if (!current.isAssigned(seller, operationId)) throw Validate.forbidden("This sales operation is not assigned to you.");
    if (!operations.isActive(operationId)) throw Validate.conflict("This sales operation is inactive and cannot accept sales.");
    String method = in.paymentMethod() == null || in.paymentMethod().isBlank() ? PaymentService.CASH : in.paymentMethod();
    payments.requireUsable(method);
    if (!operations.accepts(operationId, method)) throw Validate.conflict("This sales operation does not accept " + method.toLowerCase().replace('_', ' ') + " payments.");
    String shiftId = shifts.requireOpenShiftId(seller);
    var customer = customers.byToken(in.customerToken());

    String saleId = UUID.randomUUID().toString();
    String reference = references.dated("SL", "sale_reference_seq");
    List<PricedLine> lines = priceAndReserve(operationId, mergeLines(in.lines()), new StockLedger.Ref("sale", saleId, reference), seller.id());
    long total = 0;
    for (PricedLine l : lines) total = Math.addExact(total, l.total());
    Long cash = in.cashReceivedXaf();
    if (cash != null && cash < total) throw Validate.bad("Cash received (" + cash + " XAF) is less than the total (" + total + " XAF).");

    sales.insert(saleId, reference, seller.id(), operationId, shiftId, idempotencyKey, total, cash);
    int lineNo = 0;
    for (PricedLine l : lines) {
      sales.insertLine(saleId, ++lineNo, l.productId(), l.name(), l.unitPrice(), l.addonsUnit(), l.quantity());
      for (PricedAddon a : l.addons()) sales.insertLineAddon(saleId, lineNo, a.id(), a.name(), a.priceXaf());
    }
    payments.recordCash(saleId, reference, operationId, seller.id(), shiftId, total, cash);
    shifts.addCashSale(shiftId, saleId, seller.id(), total);
    commissions.postForSale(saleId, seller.id());
    try { targets.checkMilestones(seller.id()); }
    catch (RuntimeException e) { log.error("Could not check target milestones: {}", e.toString()); }   // never fail a sale over a notification
    if (customer.isPresent()) {
      String customerId = (String) customer.get().get("id");
      sales.setCustomer(saleId, customerId);
      loyalty.awardForSale(saleId, reference, customerId, seller.id());
    }
    audit.log(seller, "SALE_COMPLETED", "sale", saleId, reference + " " + total + " XAF");
    return new SaleResult(detail(saleId), true);
  }

  /** Merges lines with the same product and the same add-ons, in a stable order. */
  private List<MergedLine> mergeLines(List<LineItemRequest> lines) {
    if (lines == null || lines.isEmpty()) throw Validate.bad("Add at least one product to the sale.");
    TreeMap<String, MergedLine> merged = new TreeMap<>();
    for (LineItemRequest line : lines) {
      String id = Validate.id(line.productId(), "Product");
      int qty = Validate.positive(line.quantity(), "Each quantity");
      List<String> addonIds = line.addonIds() == null ? List.of() : line.addonIds().stream().distinct().sorted().toList();
      merged.merge(id + "|" + String.join(",", addonIds), new MergedLine(id, addonIds, qty),
          (a, b) -> new MergedLine(a.productId(), a.addonIds(), Math.addExact(a.quantity(), b.quantity())));
    }
    return new ArrayList<>(merged.values());
  }

  /**
   * Prices each line at the current central price plus add-ons, then deducts stock for tracked products
   * (one deduction per product, in id order so concurrent sales lock rows in the same order). Any failure fails the whole sale.
   */
  private List<PricedLine> priceAndReserve(String operationId, List<MergedLine> lines, StockLedger.Ref ref, String sellerId) {
    List<PricedLine> out = new ArrayList<>();
    TreeMap<String, Integer> stockNeeded = new TreeMap<>();
    Map<String, String> names = new HashMap<>();
    for (MergedLine line : lines) {
      Map<String, Object> p = products.findSellable(line.productId())
          .orElseThrow(() -> Validate.conflict("A product in this sale is no longer available. Refresh the catalogue."));
      List<PricedAddon> priced = addons.priceFor(line.productId(), (String) p.get("parent_id"), line.addonIds());
      out.add(new PricedLine(line.productId(), (String) p.get("name"), Rows.asLong(p.get("price_xaf")), priced, line.quantity()));
      if (Boolean.TRUE.equals(p.get("track_stock"))) {
        stockNeeded.merge(line.productId(), line.quantity(), Math::addExact);
        names.put(line.productId(), (String) p.get("name"));
      }
    }
    stockNeeded.forEach((productId, qty) -> {
      if (!ledger.take(operationId, productId, qty, MovementType.SALE, ref, sellerId, null))
        throw Validate.conflict("Not enough stock of " + names.get(productId) + " at this operation.");
    });
    return out;
  }

  public PageResponse<Map<String, Object>> search(Actor actor, LocalDate from, LocalDate to, String operationId, String sellerId, String channel, String paymentMethod,
                                          String q, PageRequest req) {
    String seller = actor.isSeller() ? actor.id() : sellerId;
    return sales.search(seller, operationId, channel, paymentMethod, Periods.startOrNull(from), Periods.endOrNull(to), candidates.find(actor, "SALE", q), req);
  }

  /** Sellers may only open their own sales. */
  public Map<String, Object> get(Actor actor, String id) {
    String sellerId = sales.sellerOf(id).orElseThrow(() -> Validate.notFound("Sale not found."));
    if (actor.isSeller() && !actor.id().equals(sellerId)) throw Validate.forbidden("You can only view your own sales.");
    return detail(id);
  }

  /** A sale with its lines, for a receipt; the caller has already checked who may see it. */
  public Map<String, Object> receiptData(String saleId) { return detail(saleId); }

  private Map<String, Object> detail(String id) {
    Map<String, Object> sale = sales.findById(id);
    sale.put("lines", sales.lines(id));
    Object cash = sale.get("cash_received_xaf");
    sale.put("change_xaf", cash == null ? null : Rows.asLong(cash) - Rows.asLong(sale.get("total_xaf")));
    sale.put("status", "COMPLETED");
    List<Map<String, Object>> paid = payments.forSale(id);
    sale.put("payments", paid);
    sale.put("payment_status", paid.stream().anyMatch(p -> "CONFIRMED".equals(p.get("status"))) ? "CONFIRMED" : "PENDING");
    sale.put("refunds", refunds.forSale(id));
    sale.put("commissions", commissionLines.forSale(id));
    sale.put("loyalty", loyaltyAwards.award(id).orElse(null));
    return sale;
  }

  /** Today and this month (Douala business days): orders, sales, customers, points issued and commission; and the last seven days' sales. */
  public Map<String, Object> sellerSummary(Actor seller) {
    var today = Periods.today();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("today", sales.summarySince(seller.id(), Periods.startOf(today)));
    out.put("month", sales.summarySince(seller.id(), Periods.startOf(today.withDayOfMonth(1))));
    // the last seven Douala days, oldest first, with zero for days without a sale
    Map<String, Long> byDay = new HashMap<>();
    for (Map<String, Object> r : sales.dailySalesSince(seller.id(), Periods.startOf(today.minusDays(6))))
      byDay.put(String.valueOf(r.get("day")), ((Number) r.get("sales_xaf")).longValue());
    List<Map<String, Object>> week = new ArrayList<>();
    for (int i = 6; i >= 0; i--) week.add(Map.of("day", today.minusDays(i).toString(), "sales_xaf", byDay.getOrDefault(today.minusDays(i).toString(), 0L)));
    out.put("week", week);
    return out;
  }
}
