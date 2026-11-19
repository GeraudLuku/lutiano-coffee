package com.lutiano.api.service.earnings;

import java.time.temporal.ChronoUnit;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.payout.PayoutRepository;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.security.ActorRepository;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.earnings.CommissionRepository;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.repository.earnings.MyEarningsRepository;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.StatementSection;

/**
 * What a seller has earned, read from their own data only. A month is an earning period: once an administrator finalises it
 * the figures are the frozen statement, until then they are computed live and are provisional.
 */
@Service
public class MyEarningsService {
  static final int MAX_MONTHS = 24, MAX_DAYS = 31, MAX_WEEKS = 26;
  /** Payout status groups a seller filters by. */
  private static final Map<String, List<String>> PAYOUT_GROUPS = Map.of(
      "COMPLETED", List.of("PAID"),
      "PENDING", List.of("PENDING_APPROVAL", "APPROVED", "REVERSAL_PENDING"),
      "CANCELLED", List.of("REJECTED", "CANCELLED", "REVERSED"));

  private final MyEarningsRepository mine;
  private final EarningsService earnings;
  private final EarningsRepository earningsRepo;
  private final TargetService targets;
  private final CommissionRepository commissions;
  private final PayoutRepository payouts;
  private final ActorRepository actors;
  private final EarningsStatementPdf statements;

  public MyEarningsService(MyEarningsRepository mine, EarningsService earnings, EarningsRepository earningsRepo, TargetService targets,
                           CommissionRepository commissions, PayoutRepository payouts, ActorRepository actors, EarningsStatementPdf statements) {
    this.mine = mine; this.earnings = earnings; this.earningsRepo = earningsRepo; this.targets = targets;
    this.commissions = commissions; this.payouts = payouts; this.actors = actors; this.statements = statements;
  }

  // ----- months -----

  /** Parses yyyy-MM (this month when blank); not in the future and not more than two years back. */
  public static YearMonth month(String value) {
    YearMonth now = YearMonth.from(Periods.today());
    if (value == null || value.isBlank()) return now;
    YearMonth m;
    try { m = YearMonth.parse(value.trim()); } catch (RuntimeException e) { throw Validate.bad("Month must look like 2026-03."); }
    if (m.isAfter(now)) throw Validate.bad("That month has not started yet.");
    if (m.isBefore(now.minusMonths(MAX_MONTHS))) throw Validate.bad("Earnings are available for the last " + MAX_MONTHS + " months.");
    return m;
  }

  private static Timestamp from(YearMonth m) { return Periods.startOf(m.atDay(1)); }
  private static Timestamp to(YearMonth m) { return Periods.endOf(m.atEndOfMonth()); }

  /** One month's figures for a seller: the frozen statement when finalised, otherwise computed now (provisional). */
  public Map<String, Object> statement(String sellerId, YearMonth m) {
    Map<String, Object> period = earnings.periodFor(m.atDay(1));
    boolean finalized = "FINALIZED".equals(period.get("status"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("month", m.toString());
    out.put("starts_on", m.atDay(1).toString());
    out.put("ends_on", m.atEndOfMonth().toString());
    out.put("status", finalized ? "FINAL" : "PROVISIONAL");
    long salesCount, salesXaf, commission, bonus, adjustments;
    var frozen = finalized ? mine.frozenStatement((String) period.get("id"), sellerId) : Optional.<Map<String, Object>>empty();
    if (finalized) {
      Map<String, Object> f = frozen.orElse(Map.of());
      salesCount = Rows.asLong(f.getOrDefault("sales_count", 0)); salesXaf = Rows.asLong(f.getOrDefault("sales_xaf", 0));
      commission = Rows.asLong(f.getOrDefault("commission_xaf", 0)); bonus = Rows.asLong(f.getOrDefault("bonus_xaf", 0)); adjustments = Rows.asLong(f.getOrDefault("adjustments_xaf", 0));
    } else {
      Map<String, Object> s = mine.sales(sellerId, from(m), to(m));
      salesCount = Rows.asLong(s.get("sales_count")); salesXaf = Rows.asLong(s.get("sales_xaf"));
      commission = mine.commission(sellerId, from(m), to(m)); adjustments = mine.adjustments(sellerId, from(m), to(m));
      bonus = bonusEndingBetween(sellerId, m.atDay(1), m.atEndOfMonth());
    }
    out.put("sales_count", salesCount); out.put("sales_xaf", salesXaf);
    out.put("commission_xaf", commission); out.put("bonus_xaf", bonus); out.put("adjustments_xaf", adjustments);
    out.put("total_xaf", commission + bonus + adjustments);
    return out;
  }

  private long bonusEndingBetween(String sellerId, LocalDate start, LocalDate end) {
    long bonus = 0;
    for (Map<String, Object> t : earningsRepo.targetsEndingBetween(sellerId, start, end)) bonus += Rows.asLong(targets.withProgress(t).get("bonus_earned"));
    return bonus;
  }

  /** The last [count] months, oldest first, for charts and trends (a bounded series, not a list). */
  public List<Map<String, Object>> months(Actor seller, int count) {
    int n = Validate.range(count, "Months", 1, MAX_MONTHS);
    YearMonth now = YearMonth.from(Periods.today());
    List<Map<String, Object>> out = new ArrayList<>();
    for (int i = n - 1; i >= 0; i--) out.add(statement(seller.id(), now.minusMonths(i)));
    return out;
  }

  // ----- overview and breakdown -----

  /** A month's earnings, the month before it, and what is owed to the seller from finalised periods. */
  public Map<String, Object> overview(Actor seller, String month) {
    YearMonth m = month(month);
    Map<String, Object> out = new LinkedHashMap<>(statement(seller.id(), m));
    out.put("previous_total_xaf", statement(seller.id(), m.minusMonths(1)).get("total_xaf"));
    Map<String, Object> balance = new LinkedHashMap<>();
    payouts.balance(seller.id()).ifPresent(b -> {
      balance.put("owed_xaf", b.get("owed_xaf")); balance.put("paid_xaf", b.get("paid_xaf"));
      balance.put("pending_xaf", b.get("pending_xaf")); balance.put("outstanding_xaf", b.get("outstanding_xaf"));
    });
    out.put("balance", balance);
    return out;
  }

  /** Where a month's earnings came from: product and add-on commission, bonuses and adjustments, and the top products. */
  public Map<String, Object> breakdown(Actor seller, String month) {
    YearMonth m = month(month);
    return breakdown(seller.id(), m, m);
  }

  Map<String, Object> breakdown(String sellerId, YearMonth first, YearMonth last) {
    Map<String, Object> source = mine.commissionBySource(sellerId, from(first), to(last));
    long total = Rows.asLong(source.get("total_xaf")), addons = Rows.asLong(source.get("addons_xaf"));
    long bonus = 0, adjustments = 0;
    for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) {
      Map<String, Object> s = statement(sellerId, m);
      bonus += Rows.asLong(s.get("bonus_xaf")); adjustments += Rows.asLong(s.get("adjustments_xaf"));
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("from", first.toString()); out.put("to", last.toString());
    out.put("commission_xaf", total);
    out.put("product_commission_xaf", total - addons);
    out.put("addon_commission_xaf", addons);
    out.put("bonus_xaf", bonus);
    out.put("adjustments_xaf", adjustments);
    out.put("top_products", mine.topProducts(sellerId, from(first), to(last), 5));
    return out;
  }

  public List<Map<String, Object>> operations(Actor seller, String month) {
    YearMonth m = month(month);
    return mine.byOperation(seller.id(), from(m), to(m));
  }

  // ----- series -----

  /**
   * Earnings per day (last [count] days), week (Monday to Sunday) or month, oldest first. Commission and adjustments fall on the day
   * they happened; a bonus on the day its target ended. A bounded series for charts, not a list.
   */
  public List<Map<String, Object>> series(Actor seller, String granularity, int count) {
    String g = Validate.oneOf(granularity == null ? "MONTH" : granularity.toUpperCase(), "Granularity", Set.of("DAY", "WEEK", "MONTH"));
    if ("MONTH".equals(g)) {
      List<Map<String, Object>> out = new ArrayList<>();
      for (Map<String, Object> s : months(seller, count)) out.add(point(s.get("starts_on"), s.get("ends_on"), Rows.asLong(s.get("commission_xaf")), Rows.asLong(s.get("bonus_xaf")), Rows.asLong(s.get("adjustments_xaf"))));
      return out;
    }
    LocalDate today = Periods.today();
    int n = "DAY".equals(g) ? Validate.range(count, "Days", 1, MAX_DAYS) : Validate.range(count, "Weeks", 1, MAX_WEEKS);
    LocalDate firstDay = "DAY".equals(g) ? today.minusDays(n - 1L) : today.with(DayOfWeek.MONDAY).minusWeeks(n - 1L);
    Map<String, Long> commission = mine.commissionByDay(seller.id(), Periods.startOf(firstDay), Periods.endOf(today));
    Map<String, Long> adjustments = mine.adjustmentsByDay(seller.id(), Periods.startOf(firstDay), Periods.endOf(today));
    Map<String, Long> bonus = new LinkedHashMap<>();
    for (Map<String, Object> t : earningsRepo.targetsEndingBetween(seller.id(), firstDay, today))
      bonus.merge(String.valueOf(t.get("ends_on")), Rows.asLong(targets.withProgress(t).get("bonus_earned")), Long::sum);
    List<Map<String, Object>> out = new ArrayList<>();
    int span = "DAY".equals(g) ? 1 : 7;
    for (LocalDate start = firstDay; !start.isAfter(today); start = start.plusDays(span)) {
      long c = 0, a = 0, b = 0;
      for (int i = 0; i < span; i++) {
        String d = start.plusDays(i).toString();
        c += commission.getOrDefault(d, 0L); a += adjustments.getOrDefault(d, 0L); b += bonus.getOrDefault(d, 0L);
      }
      out.add(point(start.toString(), start.plusDays(span - 1L).toString(), c, b, a));
    }
    return out;
  }

  private static Map<String, Object> point(Object start, Object end, long commission, long bonus, long adjustments) {
    Map<String, Object> p = new LinkedHashMap<>();
    p.put("starts_on", String.valueOf(start)); p.put("ends_on", String.valueOf(end));
    p.put("commission_xaf", commission); p.put("bonus_xaf", bonus); p.put("adjustments_xaf", adjustments); p.put("total_xaf", commission + bonus + adjustments);
    return p;
  }

  // ----- sales -----

  public PageResponse<Map<String, Object>> sales(Actor seller, String month, PageRequest req) {
    YearMonth m = month(month);
    return mine.salesWithCommission(seller.id(), from(m), to(m), req);
  }

  /** What one of the seller's sales earned: the sale, each line's rule and commission, and what a refund took back. */
  public Map<String, Object> sale(Actor seller, String saleId) {
    Map<String, Object> sale = mine.saleOf(Validate.id(saleId, "Sale"), seller.id()).orElseThrow(() -> Validate.notFound("Sale not found."));
    List<Map<String, Object>> lines = new ArrayList<>();
    long commission = 0, reversed = 0;
    for (Map<String, Object> l : commissions.forSale(saleId)) {
      Map<String, Object> line = new LinkedHashMap<>();
      line.put("product_name", l.get("product_name")); line.put("quantity", l.get("quantity")); line.put("line_value_xaf", l.get("line_value_xaf"));
      line.put("basis", l.get("basis")); line.put("rate", l.get("rate"));   // the app words the rule in the seller's language
      line.put("rule", CommissionService.describe((String) l.get("basis"), Rows.asInt(l.get("rate"))));
      line.put("commission_xaf", l.get("commission_xaf")); line.put("reversed_xaf", l.get("reversed_xaf"));
      lines.add(line);
      commission += Rows.asLong(l.get("commission_xaf")); reversed += Rows.asLong(l.get("reversed_xaf"));
    }
    Map<String, Object> out = new LinkedHashMap<>(sale);
    out.put("lines", lines);
    out.put("commission_xaf", commission); out.put("reversed_xaf", reversed); out.put("net_xaf", commission - reversed);
    return out;
  }

  // ----- payouts -----

  /** The seller's payouts; [group] is COMPLETED, PENDING or CANCELLED. The summary counts every payout, whatever the filter. */
  public PageResponse<Map<String, Object>> payouts(Actor seller, String group, PageRequest req) {
    List<String> statuses = group == null || group.isBlank() ? List.of() : PAYOUT_GROUPS.get(Validate.oneOf(group, "Status", PAYOUT_GROUPS.keySet()));
    return mine.payouts(seller.id(), statuses, req).map(MyEarningsService::maskPayout);
  }

  public Map<String, Object> payout(Actor seller, String id) {
    Map<String, Object> p = mine.payout(Validate.id(id, "Payout"), seller.id()).orElseThrow(() -> Validate.notFound("Payout not found."));
    Map<String, Object> out = maskPayout(p);
    mine.frozenStatement((String) p.get("period_id"), seller.id()).ifPresent(s -> {
      Map<String, Object> st = new LinkedHashMap<>();
      for (String k : List.of("commission_xaf", "bonus_xaf", "adjustments_xaf", "total_xaf", "payable_xaf")) st.put(k, s.get(k));
      out.put("statement", st);
    });
    return out;
  }

  /** A payout without the destination number: only its last four digits are shown. */
  private static Map<String, Object> maskPayout(Map<String, Object> p) {
    Map<String, Object> out = new LinkedHashMap<>(p);
    String phone = (String) out.remove("destination_phone");
    if (phone != null) {
      String digits = phone.replaceAll("\\D", "");
      out.put("destination_last4", digits.length() >= 4 ? digits.substring(digits.length() - 4) : digits);
    }
    return out;
  }

  // ----- statement -----

  public static final Set<String> STATEMENT_SECTIONS = Enums.names(StatementSection.class);

  /** The earnings statement of the signed-in seller for up to twelve months, with the sections they chose, as a PDF. */
  public byte[] statementPdf(Actor seller, String fromMonth, String toMonth, Set<String> sections, String language) {
    YearMonth first = month(fromMonth), last = month(toMonth);
    if (last.isBefore(first)) throw Validate.bad("The end month must not be before the start month.");
    if (first.until(last, ChronoUnit.MONTHS) >= 12) throw Validate.bad("A statement covers at most 12 months.");
    if (sections == null || sections.isEmpty()) throw Validate.bad("Choose what the statement includes.");
    for (String sec : sections) Validate.oneOf(sec, "Section", STATEMENT_SECTIONS);
    List<Map<String, Object>> monthRows = new ArrayList<>();
    for (YearMonth m = first; !m.isAfter(last); m = m.plusMonths(1)) monthRows.add(statement(seller.id(), m));
    List<Map<String, Object>> bonuses = new ArrayList<>();
    for (Map<String, Object> t : earningsRepo.targetsEndingBetween(seller.id(), first.atDay(1), last.atEndOfMonth())) {
      Map<String, Object> p = targets.withProgress(t);
      if (Rows.asLong(p.get("bonus_earned")) > 0) bonuses.add(Map.of("name", p.get("name"), "ends_on", String.valueOf(p.get("ends_on")), "bonus_xaf", p.get("bonus_earned")));
    }
    return statements.render(new EarningsStatementPdf.Model(seller.fullName(), first, last, language, sections, monthRows,
        breakdown(seller.id(), first, last), bonuses, mine.adjustmentLines(seller.id(), from(first), to(last), 200), mine.byOperation(seller.id(), from(first), to(last))));
  }

  // ----- rules -----

  /** The commission rules in force for the seller's role, by product. */
  public PageResponse<Map<String, Object>> rules(Actor seller, PageRequest req) {
    String role = actors.sellerRole(seller.id());
    if (role == null) return PageResponse.of(List.of(), req, 0);
    return mine.rulesFor(role, req).map(r -> {
      Map<String, Object> out = new LinkedHashMap<>(r);
      out.put("description", CommissionService.describe((String) r.get("basis"), Rows.asInt(r.get("rate"))));
      return out;
    });
  }
}
