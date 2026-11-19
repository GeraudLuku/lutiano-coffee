package com.lutiano.api.service.dashboard;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.payout.PayoutRepository;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.dashboard.DashboardRepository;
import com.lutiano.api.config.AppProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {
  private final DashboardRepository repo;
  private final PayoutRepository payouts;
  private final int inactivityDays;

  public DashboardService(DashboardRepository repo, PayoutRepository payouts, AppProperties.Dashboard dashboard) {
    this.repo = repo; this.payouts = payouts; this.inactivityDays = dashboard.inactivityDays();
  }

  /**
   * Totals for a Douala date range (inclusive), compared with the equally long period just before.
   * Either pass from/to, or `days` for the last N days ending today. Finance figures (members, points, commission, payouts) are included for administrators only.
   */
  public Map<String, Object> summary(Integer requestedDays, LocalDate fromDay, LocalDate toDay, boolean admin) {
    LocalDate today = Periods.today();
    LocalDate first, last;
    if (fromDay != null || toDay != null) {
      last = toDay != null ? toDay : today;
      first = fromDay != null ? fromDay : last.minusDays(29);
      if (first.isAfter(last)) throw Validate.bad("The start date must be on or before the end date.");
      if (first.plusDays(366).isBefore(last)) throw Validate.bad("Choose a range of one year or less.");
    } else {
      int days = Math.max(1, Math.min(requestedDays == null ? 30 : requestedDays, 365));
      last = today;
      first = today.minusDays(days - 1L);
    }
    int days = (int) (last.toEpochDay() - first.toEpochDay() + 1);
    Timestamp from = Periods.startOf(first), to = Periods.endOf(last), prevFrom = Periods.startOf(first.minusDays(days));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("days", days);
    out.put("from", first.toString());
    out.put("to", last.toString());
    out.put("current", repo.totals(from, to));
    out.put("previous", repo.totals(prevFrom, from));
    out.put("activeOperations", repo.activeOperations());
    out.put("openShifts", repo.openShifts());
    out.put("salesByDay", dailySeries(first, last, repo.salesByDay(from, to)));
    out.put("salesByChannel", repo.salesByChannel(from, to));
    out.put("topProducts", repo.topProducts(from, to, 5));
    out.put("topSellers", repo.topSellers(from, to, 5));
    out.put("paymentMix", repo.paymentMix(from, to));
    out.put("inactivityDays", inactivityDays);
    out.put("inactiveSellers", repo.inactiveSellers(Periods.startOf(today.minusDays(inactivityDays - 1L))));
    out.put("inventoryStatus", repo.inventoryStatus(5));
    Map<String, Object> attention = new LinkedHashMap<>();
    attention.put("pendingApprovals", repo.pendingApprovals());
    attention.put("inTransit", repo.inTransit());
    attention.put("discrepancies", repo.discrepanciesSince(from));
    attention.put("cashVariances", repo.cashVariancesSince(from));
    attention.put("lowStockCentral", repo.lowCentralStock());
    attention.put("lowStockOperations", repo.lowOperationStock());
    out.put("attention", attention);
    out.put("inventoryValueXaf", repo.inventoryValue());
    out.put("lowStockItems", repo.lowCentralStock() + repo.lowOperationStock());
    if (admin) {
      Map<String, Object> loyalty = repo.loyaltyTotals(from, to);
      Map<String, Object> finance = new LinkedHashMap<>(loyalty);
      finance.put("commissions_xaf", repo.commissionsEarned(from, to));
      finance.put("payouts_paid_xaf", repo.payoutsPaid(from, to));
      finance.put("payouts_outstanding_xaf", Rows.asLong(payouts.balanceTotals().get("outstanding_xaf")));
      out.put("finance", finance);
    }
    return out;
  }

  /** One entry per day, including days without sales. */
  private static List<Map<String, Object>> dailySeries(LocalDate first, LocalDate last, List<Map<String, Object>> rows) {
    Map<String, Map<String, Object>> byDay = new HashMap<>();
    rows.forEach(r -> byDay.put(r.get("d").toString(), r));
    List<Map<String, Object>> series = new ArrayList<>();
    for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
      Map<String, Object> r = byDay.get(d.toString());
      series.add(Map.of("date", d.toString(), "sales", r == null ? 0 : r.get("sales"), "revenueXaf", r == null ? 0 : r.get("revenue_xaf")));
    }
    return series;
  }
}
