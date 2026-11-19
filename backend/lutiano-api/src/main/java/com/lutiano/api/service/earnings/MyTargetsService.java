package com.lutiano.api.service.earnings;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.earnings.EarningsRepository;
import com.lutiano.api.definitions.enums.BonusFilter;
import com.lutiano.api.utils.Enums;

/**
 * A seller's own targets and bonuses. Progress is live; a bonus is earned when its target is reached, and is part of the
 * seller's earnings once the month it ended in is finalised (it is paid out with the rest of the earnings, not on its own).
 */
@Service
public class MyTargetsService {
  public static final String PENDING = "PENDING", INCLUDED = "INCLUDED", REVERSED = "REVERSED", IN_PROGRESS = "IN_PROGRESS", NOT_REACHED = "NOT_REACHED", NONE = "NONE";
  private static final Set<String> STATES = Set.of("CURRENT", "UPCOMING", "ENDED");
  private static final Set<String> BONUS_FILTERS = Enums.names(BonusFilter.class);
  private static final int MAX_MONTHS = 12;

  private final EarningsRepository repo;
  private final TargetService targets;
  private final MyEarningsService earnings;

  public MyTargetsService(EarningsRepository repo, TargetService targets, MyEarningsService earnings) {
    this.repo = repo; this.targets = targets; this.earnings = earnings;
  }

  /**
   * Where a target's bonus stands: PENDING (reached, will be in the earnings of the month it ends in), INCLUDED (already in a
   * finalised month), REVERSED (taken back after a refund), IN_PROGRESS / NOT_REACHED, or NONE when the target has no bonus.
   */
  static String bonusState(Map<String, Object> t) {
    if (Rows.asLong(t.get("bonus_xaf")) <= 0) return NONE;
    if (Rows.truthy(t.get("bonus_reversed"))) return REVERSED;
    if (t.get("bonus_paid_xaf") != null && Rows.asLong(t.get("bonus_paid_xaf")) > 0) return INCLUDED;
    if (Rows.truthy(t.get("reached"))) return PENDING;
    return "ENDED".equals(t.get("state")) ? NOT_REACHED : IN_PROGRESS;
  }

  /** A target as the seller reads it: its progress, the bonus state and the month the bonus belongs to. */
  private Map<String, Object> view(Map<String, Object> raw) {
    Map<String, Object> t = new LinkedHashMap<>(targets.withProgress(new LinkedHashMap<>(raw)));
    t.remove("seller_name"); t.remove("seller_role"); t.remove("created_by"); t.remove("created_by_name");
    t.put("bonus_state", bonusState(t));
    t.put("bonus_month", YearMonth.from(TargetService.toDate(t.get("ends_on"))).toString());
    return t;
  }

  /** The seller's targets, newest first; [state] CURRENT, UPCOMING or ENDED, and/or those overlapping [month] (yyyy-MM). */
  public PageResponse<Map<String, Object>> list(Actor seller, String state, String month, PageRequest req) {
    String st = state == null || state.isBlank() ? null : Validate.oneOf(state, "State", STATES);
    LocalDate from = LocalDate.of(1970, 1, 1), to = LocalDate.of(2999, 12, 31);
    if (month != null && !month.isBlank()) { YearMonth m = MyEarningsService.month(month); from = m.atDay(1); to = m.atEndOfMonth(); }
    return repo.activeTargetsOf(seller.id(), st, from, to, Periods.today(), req).map(this::view);
  }

  public Map<String, Object> get(Actor seller, String id) {
    Map<String, Object> t = repo.target(Validate.id(id, "Target")).orElseThrow(() -> Validate.notFound("Target not found."));
    if (!seller.id().equals(t.get("seller_id")) || !"ACTIVE".equals(t.get("status"))) throw Validate.notFound("Target not found.");
    return view(t);
  }

  /** For a month: the targets running in it, how many are reached, whose bonus is waiting or already in earnings, and the main target. */
  public Map<String, Object> summary(Actor seller, String month) {
    YearMonth m = MyEarningsService.month(month);
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Map<String, Object> raw : repo.activeTargetsOverlapping(seller.id(), m.atDay(1), m.atEndOfMonth())) rows.add(view(raw));
    int reached = 0, pending = 0, included = 0, current = 0;
    long pendingXaf = 0, includedXaf = 0;
    Map<String, Object> main = null;
    for (Map<String, Object> t : rows) {
      if (Rows.truthy(t.get("reached"))) reached++;
      if ("CURRENT".equals(t.get("state"))) current++;
      switch ((String) t.get("bonus_state")) {
        case PENDING -> { pending++; pendingXaf += Rows.asLong(t.get("bonus_earned")); }
        case INCLUDED -> { included++; includedXaf += Rows.asLong(t.get("bonus_paid_xaf")); }
        default -> { }
      }
      // the main target: a running one, preferring monthly ones, else the first of the month
      if (main == null || rank(t) > rank(main)) main = t;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("month", m.toString());
    out.put("targets", rows.size()); out.put("current", current); out.put("reached", reached);
    out.put("bonus_pending", pending); out.put("bonus_pending_xaf", pendingXaf);
    out.put("bonus_included", included); out.put("bonus_included_xaf", includedXaf);
    out.put("main", main);
    return out;
  }

  private static int rank(Map<String, Object> t) {
    return ("CURRENT".equals(t.get("state")) ? 2 : 0) + ("MONTHLY".equals(t.get("period_type")) ? 1 : 0);
  }

  /**
   * The seller's bonus history: targets whose bonus was earned (waiting or included in earnings) or taken back, newest first.
   * [status] PENDING, INCLUDED or REVERSED. A seller has few targets, so they are read together and paged here.
   */
  public PageResponse<Map<String, Object>> bonuses(Actor seller, String status, PageRequest req) {
    String filter = status == null || status.isBlank() ? null : Validate.oneOf(status, "Status", BONUS_FILTERS);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> raw : repo.activeTargetsOverlapping(seller.id(), LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31))) {
      Map<String, Object> t = view(raw);
      String s = (String) t.get("bonus_state");
      if (!BONUS_FILTERS.contains(s)) continue;
      if (filter == null || filter.equals(s)) out.add(t);
    }
    return PageResponse.slice(out, req);
  }

  /**
   * Month by month over the last [count] months: sales, how many targets ended in the month and were reached, their average
   * progress, and the totals for monthly value targets (for the trend). A bounded series for charts.
   */
  public Map<String, Object> performance(Actor seller, int count) {
    int n = Validate.range(count, "Months", 1, MAX_MONTHS);
    YearMonth now = YearMonth.from(Periods.today());
    List<Map<String, Object>> months = new ArrayList<>();
    long sales = 0, bonus = 0;
    int targetCount = 0, reached = 0;
    for (int i = n - 1; i >= 0; i--) {
      YearMonth m = now.minusMonths(i);
      Map<String, Object> st = earnings.statement(seller.id(), m);
      int ended = 0, hit = 0;
      double progress = 0;
      long monthlyTarget = 0, monthlyAchieved = 0;
      boolean hasMonthly = false;
      for (Map<String, Object> raw : repo.targetsEndingBetween(seller.id(), m.atDay(1), m.atEndOfMonth())) {
        Map<String, Object> t = view(raw);
        ended++; progress += ((Number) t.get("progress_pct")).doubleValue();
        if (Rows.truthy(t.get("reached"))) { hit++; bonus += Rows.asLong(t.get("bonus_earned")); }
        if ("MONTHLY".equals(t.get("period_type")) && "VALUE".equals(t.get("metric"))) { hasMonthly = true; monthlyTarget += Rows.asLong(t.get("target_value")); monthlyAchieved += Rows.asLong(t.get("achieved")); }
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("month", m.toString());
      row.put("sales_xaf", st.get("sales_xaf"));
      row.put("targets", ended); row.put("reached", hit);
      row.put("avg_progress_pct", ended == 0 ? null : Math.round(progress * 10 / ended) / 10.0);
      row.put("monthly_target_xaf", hasMonthly ? monthlyTarget : null);
      row.put("monthly_achieved_xaf", hasMonthly ? monthlyAchieved : null);
      months.add(row);
      sales += Rows.asLong(st.get("sales_xaf")); targetCount += ended; reached += hit;
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("total_sales_xaf", sales); out.put("targets", targetCount); out.put("reached", reached); out.put("bonus_xaf", bonus);
    out.put("months", months);
    return out;
  }
}
