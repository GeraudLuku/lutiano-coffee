package com.lutiano.api.service.earnings;

import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.format.TextStyle;
import java.util.Locale;
import com.lutiano.api.repository.earnings.CommissionRepository;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.models.request.earnings.EarningsAdjustmentRequest;

/**
 * Monthly earning periods and seller statements: commissions on sales in the period + bonuses of targets that ended in
 * it + adjustments dated in it. An open period is calculated live; finalising freezes each statement (and the bonuses
 * in it). A negative total is not payable: it is carried into the next period as a RECOVERY adjustment.
 */
@Service
public class EarningsService {
  public static final String REFUND_REVERSAL = "REFUND_REVERSAL", BONUS_REVERSAL = "BONUS_REVERSAL", RECOVERY = "RECOVERY", MANUAL = "MANUAL";


  private final EarningsRepository repo;
  private final CommissionRepository commissions;
  private final TargetService targets;
  private final AuditService audit;
  private final NotificationService notifications;

  public EarningsService(EarningsRepository repo, CommissionRepository commissions, TargetService targets, AuditService audit, NotificationService notifications) {
    this.repo = repo; this.commissions = commissions; this.targets = targets; this.audit = audit; this.notifications = notifications;
  }

  // ----- periods -----

  /** The month containing {@code day}, created when first needed. */
  public Map<String, Object> periodFor(LocalDate day) {
    LocalDate start = day.withDayOfMonth(1);
    repo.insertPeriod(UUID.randomUUID().toString(), start, start.with(TemporalAdjusters.lastDayOfMonth()));
    return repo.periodStarting(start).orElseThrow();
  }

  /** Every month from the first commissioned sale to today, newest first (missing months are created). */
  public PageResponse<Map<String, Object>> periods(PageRequest req) {
    LocalDate today = Periods.today();
    for (LocalDate m = commissions.firstSaleDay().orElse(today).withDayOfMonth(1); !m.isAfter(today); m = m.plusMonths(1)) periodFor(m);
    return repo.periods(req);
  }

  /** The period with its totals (frozen when finalised, live while open). */
  public Map<String, Object> period(String id) {
    Map<String, Object> p = find(id);
    List<Map<String, Object>> rows = statements(p);
    Map<String, Object> totals = new LinkedHashMap<>();
    for (String k : List.of("commission_xaf", "bonus_xaf", "adjustments_xaf", "total_xaf", "payable_xaf"))
      totals.put(k, rows.stream().mapToLong(r -> Rows.asLong(r.get(k))).sum());
    totals.put("sellers", rows.stream().filter(r -> Rows.asLong(r.get("total_xaf")) != 0 || Rows.asLong(r.get("sales_count")) > 0).count());
    p.put("totals", totals);
    p.put("can_finalize", "OPEN".equals(p.get("status")) && TargetService.toDate(p.get("ends_on")).isBefore(Periods.today()));
    return p;
  }

  public PageResponse<Map<String, Object>> periodSellers(String id, PageRequest req) { return PageResponse.slice(statements(find(id)), req); }

  /**
   * Freezes every seller's statement. Periods are finalised in order, after they end, so a recovery balance can always
   * be carried into the next one.
   */
  @Transactional
  public Map<String, Object> finalize(Actor actor, String id) {
    Map<String, Object> p = Validate.status(repo.lockPeriod(Validate.id(id, "Period")).orElseThrow(() -> Validate.notFound("Earning period not found.")),
        "period", "finalised", "OPEN");
    LocalDate start = TargetService.toDate(p.get("starts_on")), end = TargetService.toDate(p.get("ends_on"));
    if (!end.isBefore(Periods.today())) throw Validate.conflict("This period has not ended yet.");
    if (repo.openPeriodBefore(start)) throw Validate.conflict("Finalise the earlier periods first.");
    periodFor(end.plusDays(1));
    Timestamp nextStart = Periods.startOf(end.plusDays(1));
    long carriedTotal = 0;
    for (Map<String, Object> r : statements(p)) {
      String sellerId = (String) r.get("seller_id");
      long total = Rows.asLong(r.get("total_xaf")), carried = Math.min(total, 0);
      if (total == 0 && Rows.asLong(r.get("sales_count")) == 0) continue;
      repo.insertStatement(id, sellerId, Rows.asInt(r.get("sales_count")), Rows.asLong(r.get("sales_xaf")), Rows.asLong(r.get("commission_xaf")),
          Rows.asLong(r.get("bonus_xaf")), Rows.asLong(r.get("adjustments_xaf")), total, Math.max(total, 0), carried);
      for (Map<String, Object> t : repo.targetsEndingBetween(sellerId, start, end)) {
        long bonus = Rows.asLong(targets.withProgress(t).get("bonus_earned"));
        if (bonus > 0) {
          repo.freezeBonus((String) t.get("id"), bonus);
          notifications.send(NotificationCatalog.TARGET_REACHED, sellerId, Map.of("target", (String) t.get("name"), "bonus", NotificationService.xaf(bonus)), "seller_target", (String) t.get("id"));
        }
      }
      notifications.send(NotificationCatalog.EARNINGS_FINALIZED, sellerId, Map.of("month", String.format("%02d/%d", start.getMonthValue(), start.getYear()), "total", NotificationService.xaf(total)), "earning_period", id);
      if (carried < 0) {
        repo.insertAdjustment(sellerId, RECOVERY, carried, "PERIOD", id, "Balance carried from " + start.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + start.getYear(), actor.id(), nextStart);
        carriedTotal += carried;
      }
    }
    repo.finalizePeriod(id, actor.id());
    audit.log(actor, "EARNINGS_FINALIZED", "earning_period", id, start + " to " + end + (carriedTotal < 0 ? ", recovery carried " + carriedTotal + " XAF" : ""));
    return period(id);
  }

  // ----- statements -----

  /** Frozen statements of a finalised period, or the live calculation of an open one (same columns). */
  private List<Map<String, Object>> statements(Map<String, Object> period) {
    String id = (String) period.get("id");
    if ("FINALIZED".equals(period.get("status"))) return repo.statements(id);
    LocalDate start = TargetService.toDate(period.get("starts_on")), end = TargetService.toDate(period.get("ends_on"));
    Timestamp from = Periods.startOf(start), to = Periods.endOf(end);
    Map<Object, Long> commission = byseller(commissions.totalsBySeller(from, to), "commission_xaf");
    Map<Object, Long> adjustments = byseller(repo.adjustmentTotals(from, to), "amount_xaf");
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> s : repo.sellerSales(from, to)) {
      Object sellerId = s.get("seller_id");
      long bonus = 0;
      for (Map<String, Object> t : repo.targetsEndingBetween((String) sellerId, start, end)) bonus += Rows.asLong(targets.withProgress(t).get("bonus_earned"));
      long c = commission.getOrDefault(sellerId, 0L), a = adjustments.getOrDefault(sellerId, 0L), total = c + bonus + a;
      if (Rows.asLong(s.get("sales_count")) == 0 && total == 0) continue;
      Map<String, Object> row = new LinkedHashMap<>(s);
      row.put("period_id", id);
      row.put("commission_xaf", c);
      row.put("bonus_xaf", bonus);
      row.put("adjustments_xaf", a);
      row.put("total_xaf", total);
      row.put("payable_xaf", Math.max(total, 0));
      row.put("carried_xaf", Math.min(total, 0));
      rows.add(row);
    }
    rows.sort((x, y) -> Long.compare(Rows.asLong(y.get("total_xaf")), Rows.asLong(x.get("total_xaf"))));
    return rows;
  }

  /** A seller's current-period earnings (live) and frozen statements. */
  public Map<String, Object> seller(String sellerId) {
    Map<String, Object> current = period((String) periodFor(Periods.today()).get("id"));
    Map<String, Object> mine = statements(current).stream().filter(r -> sellerId.equals(r.get("seller_id"))).findFirst().orElse(Map.of());
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("period", current);
    out.put("current", mine);
    return out;
  }

  public PageResponse<Map<String, Object>> sellerStatements(String sellerId, PageRequest req) { return repo.sellerStatements(Validate.id(sellerId, "Seller"), req); }

  // ----- adjustments -----

  public PageResponse<Map<String, Object>> adjustments(String sellerId, String type, LocalDate from, LocalDate to, PageRequest req) {
    return repo.adjustments(sellerId, type, Periods.startOrNull(from), Periods.endOrNull(to), req);
  }

  /** A manual correction (bonus, deduction…) posted now, in the open period. */
  @Transactional
  public Map<String, Object> addManual(Actor actor, EarningsAdjustmentRequest in) {
    String sellerId = Validate.id(in.sellerId(), "Seller");
    if (in.amountXaf() == null || in.amountXaf() == 0 || Math.abs(in.amountXaf()) > 100_000_000L) throw Validate.bad("Enter an amount to add (positive) or deduct (negative).");
    String note = Validate.text(in.note(), "Reason", 500);
    if (!"OPEN".equals(periodFor(Periods.today()).get("status"))) throw Validate.conflict("The current period is already finalised.");
    String id = repo.insertAdjustment(sellerId, MANUAL, in.amountXaf(), null, null, note, actor.id(), null);
    audit.log(actor, "EARNINGS_ADJUSTED", "staff_user", sellerId, (in.amountXaf() > 0 ? "+" : "") + in.amountXaf() + " XAF: " + note);
    return repo.adjustment(id).orElseThrow();
  }

  // ----- overview -----

  /** The current month so far: totals, top earners and the products that earned most commission. */
  public Map<String, Object> overview() {
    Map<String, Object> current = period((String) periodFor(Periods.today()).get("id"));
    LocalDate start = TargetService.toDate(current.get("starts_on")), end = TargetService.toDate(current.get("ends_on"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("period", current);
    out.put("topEarners", statements(current).stream().limit(5).toList());
    out.put("topProducts", commissions.totalsByProduct(Periods.startOf(start), Periods.endOf(end), 5));
    return out;
  }

  private Map<String, Object> find(String id) {
    return repo.period(Validate.id(id, "Period")).orElseThrow(() -> Validate.notFound("Earning period not found."));
  }

  private static Map<Object, Long> byseller(List<Map<String, Object>> rows, String column) {
    Map<Object, Long> m = new HashMap<>();
    for (Map<String, Object> r : rows) m.put(r.get("seller_id"), Rows.asLong(r.get(column)));
    return m;
  }
}
