package com.lutiano.api.service.earnings;

import com.lutiano.api.repository.staff.StaffRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.service.staff.SellerRoles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.earnings.CommissionRepository;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.models.request.earnings.CommissionRuleRequest;
import com.lutiano.api.definitions.enums.CommissionBasis;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.utils.Periods;

/**
 * Commission rules per product and seller role, and commission posting. Each sale line's commission is calculated
 * with the current rule when the sale completes and stored with the rule's snapshot: FIXED_PER_UNIT = rate × quantity,
 * PERCENT = line value × rate / 10 000 (rate in basis points), rounded down per line. A refund takes back the refunded
 * share as an adjustment in the current period; posted commissions are never rewritten.
 */
@Service
public class CommissionService {
  public static final String FIXED_PER_UNIT = "FIXED_PER_UNIT", PERCENT = "PERCENT";
  public static final Set<String> BASES = Enums.names(CommissionBasis.class);


  private final CommissionRepository commissions;
  private final ProductRepository products;
  private final EarningsRepository repo;
  private final TargetService targets;
  private final AuditService audit;
  private final StaffRepository staff;

  public CommissionService(CommissionRepository commissions, ProductRepository products, EarningsRepository repo, TargetService targets, AuditService audit,
                           StaffRepository staff) {
    this.commissions = commissions; this.products = products; this.repo = repo; this.targets = targets; this.audit = audit; this.staff = staff;
  }

  // ----- rules -----

  public List<Map<String, Object>> rules(String sellerRole) { return commissions.currentRules(sellerRole == null || sellerRole.isBlank() ? null : sellerRole); }

  public PageResponse<Map<String, Object>> history(String productId, String sellerRole, PageRequest req) {
    return commissions.ruleHistory(Validate.id(productId, "Product"), Validate.oneOf(sellerRole, "Seller role", SellerRoles.CHANNEL.keySet()), req);
  }

  /** Sets the rule of a product and role; the previous rule, if any, is kept in history. */
  @Transactional
  public Map<String, Object> saveRule(Actor actor, CommissionRuleRequest in) {
    String productId = Validate.id(in.productId(), "Product");
    Map<String, Object> product = products.findById(productId).orElseThrow(() -> Validate.bad("Product not found."));
    String role = Validate.oneOf(in.sellerRole(), "Seller role", SellerRoles.CHANNEL.keySet());
    String basis = Validate.oneOf(in.basis(), "Commission basis", BASES);
    int rate = FIXED_PER_UNIT.equals(basis) ? Validate.range(in.rate(), "Commission per unit (XAF)", 1, 1_000_000)
        : Validate.range(in.rate(), "Commission rate", 1, 10_000);
    commissions.lockCurrent(productId, role).ifPresent(r -> commissions.supersede((String) r.get("id"), actor.id()));
    String id = UUID.randomUUID().toString();
    commissions.insertRule(id, productId, role, basis, rate, Validate.optional(in.note(), "Note", 255), actor.id());
    audit.log(actor, "COMMISSION_RULE_SET", "product", productId, product.get("name") + " · " + role + ": " + describe(basis, rate));
    return commissions.rule(id).orElseThrow();
  }

  /** Stops paying commission on a product for a role from now on. */
  @Transactional
  public void endRule(Actor actor, String id) {
    Map<String, Object> r = commissions.rule(Validate.id(id, "Rule")).orElseThrow(() -> Validate.notFound("Commission rule not found."));
    if (r.get("superseded_at") != null) throw Validate.conflict("This rule has already ended.");
    commissions.supersede(id, actor.id());
    audit.log(actor, "COMMISSION_RULE_ENDED", "product", (String) r.get("product_id"), r.get("product_name") + " · " + r.get("seller_role"));
  }

  // ----- posting (inside the sale and refund transactions) -----

  @Transactional(propagation = Propagation.MANDATORY)
  public void postForSale(String saleId, String sellerId) {
    String sellerRole = staff.findById(sellerId).map(s -> (String) s.get("seller_role")).orElse(null);
    if (sellerRole == null) return;
    for (Map<String, Object> line : commissions.saleLines(saleId)) {
      String productId = (String) line.get("product_id");
      commissions.ruleFor(productId, (String) line.get("parent_id"), sellerRole).ifPresent(rule -> {
        int quantity = Rows.asInt(line.get("quantity"));
        long value = Rows.asLong(line.get("line_value_xaf"));
        int rate = Rows.asInt(rule.get("rate"));
        long commission = FIXED_PER_UNIT.equals(rule.get("basis")) ? (long) rate * quantity : value * rate / 10_000;
        commissions.insertLine(saleId, Rows.asInt(line.get("line_no")), sellerId, productId, rule, quantity, value, commission);
      });
    }
  }

  /** Takes back the commission of the refunded share of a sale (cumulative over partial refunds), then re-checks paid bonuses. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void reverseForRefund(String saleId, LocalDate saleDay, long refundedTotalXaf, long saleTotalXaf, String refundId, String refundReference, Actor actor) {
    if (saleTotalXaf <= 0) return;
    Map<String, Object> t = commissions.lockSaleTotals(saleId);
    long total = Rows.asLong(t.get("commission")), reversed = Rows.asLong(t.get("reversed"));
    long due = total * Math.min(refundedTotalXaf, saleTotalXaf) / saleTotalXaf - reversed;
    String sellerId = (String) t.get("seller_id");
    if (due > 0) {
      commissions.addReversed(saleId, Rows.asInt(t.get("top_line")), due);
      repo.insertAdjustment(sellerId, EarningsService.REFUND_REVERSAL, -due, "REFUND", refundId, "Commission on refund " + refundReference, actor.id(), null);
    }
    if (sellerId != null) targets.recheckPaidBonuses(sellerId, saleDay, refundId, refundReference, actor);
  }

  public static String describe(String basis, int rate) {
    return FIXED_PER_UNIT.equals(basis) ? rate + " XAF per unit" : (rate / 100.0) + "% of sale value";
  }

  /** One page of posted commission lines, optionally for one seller and a Douala date range. */
  public PageResponse<Map<String, Object>> lines(String sellerId, LocalDate from, LocalDate to, PageRequest paging) {
    return commissions.lines(sellerId, Periods.startOrNull(from), Periods.endOrNull(to), paging);
  }
}
