package com.lutiano.api.service.reports;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import com.lutiano.api.repository.reports.ReportRepository;

/** Sales analytics for administrators: an overview, grouped sales reports and a small report builder. */
@Service
public class ReportService {
  private static final int MAX_DAYS = 366, MAX_METRICS = 6;
  private final ReportRepository repo;

  public ReportService(ReportRepository repo) { this.repo = repo; }

  /** The period (default: last 30 Douala days) and the equally long one before it. */
  private record Period(LocalDate first, LocalDate last) {
    int days() { return (int) (last.toEpochDay() - first.toEpochDay() + 1); }
    Timestamp from() { return Periods.startOf(first); }
    Timestamp to() { return Periods.endOf(last); }
    Timestamp prevFrom() { return Periods.startOf(first.minusDays(days())); }
  }

  private static Period period(LocalDate from, LocalDate to) {
    LocalDate last = to != null ? to : Periods.today(), first = from != null ? from : last.minusDays(29);
    if (first.isAfter(last)) throw Validate.bad("The start date must be on or before the end date.");
    if (first.plusDays(MAX_DAYS).isBefore(last)) throw Validate.bad("Choose a range of one year or less.");
    return new Period(first, last);
  }

  public Map<String, Object> overview(LocalDate from, LocalDate to) {
    Period p = period(from, to);
    Map<String, Object> current = repo.totals(p.from(), p.to(), null, null, null), previous = repo.totals(p.prevFrom(), p.from(), null, null, null);
    long revenue = Rows.asLong(current.get("revenue_xaf"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("from", p.first().toString()); out.put("to", p.last().toString()); out.put("days", p.days());
    out.put("current", current); out.put("previous", previous);
    out.put("avg_commission_pct", revenue == 0 ? 0 : Math.round(Rows.asLong(current.get("commission_xaf")) * 1000.0 / revenue) / 10.0);
    out.put("byDay", fill(p, repo.grouped("day", p.from(), p.to(), null, null, null)));
    out.put("byChannel", repo.grouped("channel", p.from(), p.to(), null, null, null));
    List<Map<String, Object>> products = repo.byProduct(p.from(), p.to(), null, null, null);
    out.put("topProduct", products.isEmpty() ? null : products.get(0));
    out.put("refunded_xaf", repo.refundedXaf(p.from(), p.to()));
    return out;
  }

  /** Sales grouped by operation, product, seller, channel or day, with each row's share and growth versus the previous period. */
  public Map<String, Object> sales(String groupBy, LocalDate from, LocalDate to, String operationId, String channel, String sellerId) {
    Period p = period(from, to);
    String group = groupBy == null || groupBy.isBlank() ? "operation" : groupBy;
    if (!"product".equals(group) && !ReportRepository.knownGroup(group)) throw Validate.bad("Group by operation, product, seller, channel or day.");
    List<Map<String, Object>> rows = "product".equals(group) ? repo.byProduct(p.from(), p.to(), operationId, channel, sellerId) : repo.grouped(group, p.from(), p.to(), operationId, channel, sellerId);
    Map<String, Object> totals = repo.totals(p.from(), p.to(), operationId, channel, sellerId), previous = repo.totals(p.prevFrom(), p.from(), operationId, channel, sellerId);
    long revenue = Rows.asLong(totals.get("revenue_xaf"));
    for (Map<String, Object> r : rows) r.put("share_pct", revenue == 0 ? 0 : Math.round(Rows.asLong(r.get("revenue_xaf")) * 1000.0 / revenue) / 10.0);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("groupBy", group); out.put("from", p.first().toString()); out.put("to", p.last().toString());
    out.put("rows", rows); out.put("totals", totals); out.put("previous", previous);
    long tx = Rows.asLong(totals.get("transactions"));
    out.put("avg_order_xaf", tx == 0 ? 0 : revenue / tx);
    out.put("byDay", "day".equals(group) ? fill(p, rows) : fill(p, repo.grouped("day", p.from(), p.to(), operationId, channel, sellerId)));
    return out;
  }

  /** Chosen metrics grouped by a dimension; the metrics and group are checked against fixed lists. */
  public Map<String, Object> builder(List<String> metrics, String groupBy, LocalDate from, LocalDate to) {
    if (metrics == null || metrics.isEmpty()) throw Validate.bad("Choose at least one metric.");
    if (metrics.size() > MAX_METRICS) throw Validate.bad("Choose at most " + MAX_METRICS + " metrics.");
    List<String> chosen = metrics.stream().distinct().toList();
    for (String m : chosen) if (!ReportRepository.knownMetric(m)) throw Validate.bad("Unknown metric " + m + ".");
    String group = groupBy == null || groupBy.isBlank() ? "week" : groupBy;
    if (!ReportRepository.knownGroup(group)) throw Validate.bad("Group by day, week, month, operation, seller or channel.");
    Period p = period(from, to);
    List<Map<String, Object>> rows = repo.builder(chosen, group, p.from(), p.to());
    Map<String, Object> total = new LinkedHashMap<>();
    for (String m : chosen) {
      if ("avg_order_value".equals(m)) continue;
      total.put(m, rows.stream().mapToLong(r -> Rows.asLong(r.get(m))).sum());
    }
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("metrics", chosen); out.put("groupBy", group); out.put("from", p.first().toString()); out.put("to", p.last().toString());
    out.put("rows", rows); out.put("totals", total);
    return out;
  }

  /** One entry per day of the period, including days without sales. */
  private static List<Map<String, Object>> fill(Period p, List<Map<String, Object>> rows) {
    Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
    rows.forEach(r -> byDay.put(String.valueOf(r.get("label")), r));
    List<Map<String, Object>> out = new ArrayList<>();
    for (LocalDate d = p.first(); !d.isAfter(p.last()); d = d.plusDays(1)) {
      Map<String, Object> r = byDay.get(d.toString());
      out.add(Map.of("date", d.toString(), "transactions", r == null ? 0 : r.get("transactions"), "revenue_xaf", r == null ? 0 : r.get("revenue_xaf")));
    }
    return out;
  }
}
