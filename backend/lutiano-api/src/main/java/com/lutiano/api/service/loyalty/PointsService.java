package com.lutiano.api.service.loyalty;

import com.lutiano.api.repository.common.References;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.inventory.StockLedger;
import com.lutiano.api.security.Actor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.models.request.loyalty.PointsAdjustmentRequest;
import com.lutiano.api.models.request.loyalty.RedeemRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.MovementType;
import com.lutiano.api.definitions.enums.PointsAdjustmentReason;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.LinkedHashMap;
import java.time.LocalDate;
import com.lutiano.api.entity.EarningLine;

/**
 * Points workflows: earning on completed sales, reversal on refunds, reward redemption (and its reversal),
 * manual adjustments, expiry and tier qualification. Balances change only through {@link PointsLedger}.
 */
@Service
public class PointsService {
  public static final Set<String> ADJUSTMENT_REASONS = Enums.names(PointsAdjustmentReason.class);


  private final PointsRepository points;
  private final LoyaltyRepository config;
  private final PointsLedger ledger;
  private final EarningCalculator calculator;
  private final StockLedger stock;
  private final AuditService audit;
  private final References references;

  public PointsService(PointsRepository points, LoyaltyRepository config, PointsLedger ledger, EarningCalculator calculator, StockLedger stock,
                       AuditService audit, References references) {
    this.points = points; this.config = config; this.ledger = ledger; this.calculator = calculator; this.stock = stock; this.audit = audit; this.references = references;
  }

  // ----- earning (inside the sale transaction) -----

  /**
   * Called when a sale completes with a confirmed customer: awards points on the eligible paid value and
   * updates the member's tier. Does nothing when the programme is switched off or nothing earns.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void awardForSale(String saleId, String reference, String customerId, String sellerId) {
    Map<String, Object> settings = config.settings();
    Map<String, Object> customer = ledger.lock(customerId);
    if (Rows.truthy(settings.get("program_active"))) {
      List<EarningLine> lines = points.earningLines(saleId);
      String tierCode = (String) customer.get("tier_code");
      int tierPct = tierMultiplier(tierCode);
      EarningCalculator.Result r = calculator.calculate(lines, settings, config.activeRules(Periods.today()), tierPct, points.earningSales(customerId) == 0);
      String batchId = null;
      if (r.totalPoints() > 0)
        batchId = ledger.credit(customerId, r.totalPoints(), new PointsLedger.Entry(PointsLedger.EARNED, "SALE", saleId, reference,
            "Purchase " + reference, sellerId));
      points.insertAward(saleId, customerId, r.eligibleXaf(), r.basePoints(), tierCode, tierPct, r.bonusPoints(), r.totalPoints(), r.breakdown(), batchId);
    }
    refreshTier(customerId);
  }

  /** Takes back the points of the refunded share of a sale (cumulative across partial refunds), never below zero. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void reverseForRefund(String saleId, long refundedTotalXaf, long saleTotalXaf, String refundId, String refundReference, Actor actor) {
    var award = points.lockAward(saleId);
    if (award.isEmpty() || saleTotalXaf <= 0) return;
    Map<String, Object> a = award.get();
    String customerId = (String) a.get("customer_id");
    ledger.lock(customerId);
    int total = Rows.asInt(a.get("total_points")), already = Rows.asInt(a.get("reversed_points"));
    int target = (int) Math.min(total, (long) total * Math.min(refundedTotalXaf, saleTotalXaf) / saleTotalXaf);
    int due = target - already;
    if (due <= 0) return;
    var taken = ledger.debit(customerId, due, new PointsLedger.Entry(PointsLedger.REFUND_REVERSAL, "REFUND", refundId, refundReference,
        "Refund " + refundReference, actor.id()), true, (String) a.get("batch_id"));
    int reversed = taken.stream().mapToInt(PointsLedger.Taken::points).sum();
    points.addReversed(saleId, due);
    if (reversed < due) audit.log(actor, "LOYALTY_REVERSAL_SHORTFALL", "customer", customerId,
        (due - reversed) + " pts could not be taken back for refund " + refundReference + " (already spent)");
  }

  // ----- redemptions -----

  @Transactional
  public Map<String, Object> redeem(Actor actor, RedeemRequest in) {
    if (!Boolean.TRUE.equals(in.customerConfirmed())) throw Validate.bad("The customer must confirm before points are spent.");
    String customerId = Validate.id(in.customerId(), "Customer");
    Map<String, Object> customer = ledger.lock(customerId);
    if (!"ACTIVE".equals(customer.get("status"))) throw Validate.conflict("This customer account is not active.");
    Map<String, Object> reward = config.reward(Validate.id(in.rewardId(), "Reward")).orElseThrow(() -> Validate.notFound("Reward not found."));
    int cost = requireEligible(customerId, reward);

    String id = UUID.randomUUID().toString();
    String reference = references.dated("RWD", "redemption_seq");
    String operationId = null;
    if (Boolean.TRUE.equals(reward.get("requires_stock"))) {
      operationId = in.operationId() == null || in.operationId().isBlank() ? null : Validate.id(in.operationId(), "Location");
      if (!stock.take(operationId, (String) reward.get("product_id"), 1, MovementType.REWARD_OUT, new StockLedger.Ref("reward", id, reference), actor.id(), reward.get("name").toString()))
        throw Validate.conflict("No " + reward.get("product_name") + " in stock " + (operationId == null ? "centrally" : "at this location") + ". The redemption was not made.");
    }
    points.insertRedemption(id, reference, customerId, (String) reward.get("id"), (String) reward.get("name"), cost, operationId, actor.id());
    for (var t : ledger.debit(customerId, cost, new PointsLedger.Entry(PointsLedger.REDEEMED, "REDEMPTION", id, reference, "Redeemed " + reward.get("name"), actor.id()), false, null))
      points.insertRedemptionBatch(id, t.batchId(), t.points());
    audit.log(actor, "REWARD_REDEEMED", "customer", customerId, reference + " " + reward.get("name") + " (" + cost + " pts)");
    return points.redemption(id).orElseThrow();
  }

  /**
   * Checks that a customer may redeem a reward now (it is active, enough points, minimum purchases and spend) and returns its cost.
   * The caller has locked the customer.
   */
  int requireEligible(String customerId, Map<String, Object> reward) {
    if (!Boolean.TRUE.equals(reward.get("active"))) throw Validate.conflict("This reward is not active.");
    Map<String, Object> settings = config.settings();
    int cost = Rows.asInt(reward.get("point_cost")), balance = points.balance(customerId);
    if (balance < Rows.asInt(settings.get("min_redeem_points"))) throw Validate.conflict("At least " + settings.get("min_redeem_points") + " pts are needed to redeem.");
    if (balance < cost) throw Validate.conflict("Not enough points: " + cost + " pts needed, " + balance + " pts available.");
    Map<String, Object> c = points.customer(customerId).orElseThrow();
    if (Rows.asInt(c.get("purchases")) < Rows.asInt(reward.get("min_purchases")))
      throw Validate.conflict("This reward needs at least " + reward.get("min_purchases") + " purchases.");
    if (Rows.asLong(c.get("lifetime_spend_xaf")) < Rows.asLong(reward.get("min_spend_xaf")))
      throw Validate.conflict("This reward needs a total spend of at least " + reward.get("min_spend_xaf") + " XAF.");

    return cost;
  }

  /** Gives the points back to the batches they came from (and the stock back, if any). */
  @Transactional
  public Map<String, Object> reverseRedemption(Actor actor, String id, String reason) {
    Map<String, Object> x = points.lockRedemption(Validate.id(id, "Redemption")).orElseThrow(() -> Validate.notFound("Redemption not found."));
    if (!"COMPLETED".equals(x.get("status"))) throw Validate.conflict("This redemption has already been reversed.");
    String text = Validate.text(reason, "Reason", 500);
    String customerId = (String) x.get("customer_id");
    ledger.lock(customerId);
    List<PointsLedger.Taken> taken = new ArrayList<>();
    for (Map<String, Object> b : points.redemptionBatches(id)) taken.add(new PointsLedger.Taken((String) b.get("batch_id"), Rows.asInt(b.get("points"))));
    ledger.restore(customerId, taken, new PointsLedger.Entry(PointsLedger.REDEMPTION_REVERSED, "REDEMPTION", id, (String) x.get("reference"),
        "Reversed " + x.get("reward_name") + ": " + text, actor.id()));
    ledger.expireDue(customerId);
    Map<String, Object> reward = config.reward((String) x.get("reward_id")).orElse(null);
    if (reward != null && Boolean.TRUE.equals(reward.get("requires_stock")) && "USED".equals(x.get("use_state")))   // a wallet reward not yet used never took stock
      stock.add((String) x.get("operation_id"), (String) reward.get("product_id"), 1, MovementType.REWARD_RETURN,
          new StockLedger.Ref("reward", id, (String) x.get("reference")), actor.id(), text);
    points.reverseRedemption(id, actor.id(), text);
    audit.log(actor, "REDEMPTION_REVERSED", "customer", customerId, x.get("reference") + ": " + text);
    return points.redemption(id).orElseThrow();
  }

  // ----- adjustments -----

  @Transactional
  public Map<String, Object> adjust(Actor actor, PointsAdjustmentRequest in) {
    String customerId = Validate.id(in.customerId(), "Customer");
    ledger.lock(customerId);
    if (in.points() == null || in.points() == 0 || Math.abs(in.points()) > 10_000_000) throw Validate.bad("Enter the points to add (positive) or deduct (negative).");
    String reason = Validate.oneOf(in.reason(), "Reason", ADJUSTMENT_REASONS);
    String note = Validate.text(in.note(), "Explanation", 500);
    int balance = points.balance(customerId);
    if (in.points() < 0 && -in.points() > balance) throw Validate.conflict("Not enough points: the balance is " + balance + " pts.");
    String id = UUID.randomUUID().toString();
    String reference = references.plain("ADJ", "points_adjustment_seq");
    var entry = new PointsLedger.Entry(PointsLedger.ADJUSTED, "ADJUSTMENT", id, reference, reason + ": " + note, actor.id());
    if (in.points() > 0) ledger.credit(customerId, in.points(), entry);
    else ledger.debit(customerId, -in.points(), entry, false, null);
    points.insertAdjustment(id, reference, customerId, in.points(), reason, note, actor.id());
    audit.log(actor, "POINTS_ADJUSTED", "customer", customerId, reference + " " + (in.points() > 0 ? "+" : "") + in.points() + " pts (" + reason + ")");
    return points.adjustment(id).orElseThrow();
  }

  // ----- expiry & tiers -----

  /** Daily at 01:30 Douala time, and on demand. */
  @Scheduled(cron = "0 30 1 * * *", zone = "Africa/Douala")
  @Transactional
  public int expireAll() { return ledger.expireDue(null); }

  /** Tier from spend over the last 12 months (highest active tier the spend qualifies for). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void refreshTier(String customerId) {
    long spend = points.annualSpend(customerId, Timestamp.from(ZonedDateTime.now(Periods.DOUALA).minusYears(1).toInstant()));
    String tier = null;
    for (Map<String, Object> t : config.tiers())
      if (Boolean.TRUE.equals(t.get("active")) && spend >= Rows.asLong(t.get("min_annual_spend_xaf"))) tier = (String) t.get("code");
    points.setTier(customerId, tier);
  }

  private int tierMultiplier(String code) {
    if (code == null) return 100;
    return config.tiers().stream().filter(t -> code.equals(t.get("code")) && Boolean.TRUE.equals(t.get("active")))
        .map(t -> Rows.asInt(t.get("multiplier_pct"))).findFirst().orElse(100);
  }

  // ----- reports for the administrator screens -----

  /** Programme overview for a Douala date range: totals, daily points, redemptions by type and members by tier. */
  public Map<String, Object> overview(LocalDate from, LocalDate to) {
    LocalDate end = to == null ? Periods.today() : to, start = from == null ? end.minusDays(29) : from;
    var f = Periods.startOf(start); var t = Periods.endOf(end);
    Map<String, Object> out = new LinkedHashMap<>(config.overview(f, t, new Timestamp(System.currentTimeMillis())));
    out.put("from", start.toString()); out.put("to", end.toString());
    out.put("point_value_xaf", config.settings().get("point_value_xaf"));
    out.put("daily", config.daily(f, t));
    out.put("redemptionsByType", config.redemptionsByType(f, t));
    out.put("membersByTier", config.membersByTier());
    return out;
  }

  public PageResponse<Map<String, Object>> ledger(String type, LocalDate from, LocalDate to, PageRequest paging) {
    return config.ledger(null, blankToNull(type), Periods.startOrNull(from), Periods.endOrNull(to), paging);
  }

  public PageResponse<Map<String, Object>> redemptions(String status, LocalDate from, LocalDate to, PageRequest paging) {
    return points.redemptions(null, blankToNull(status), Periods.startOrNull(from), Periods.endOrNull(to), paging);
  }

  public PageResponse<Map<String, Object>> adjustments(String reason, LocalDate from, LocalDate to, PageRequest paging) {
    return points.adjustments(null, blankToNull(reason), Periods.startOrNull(from), Periods.endOrNull(to), paging);
  }

  private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }
}
