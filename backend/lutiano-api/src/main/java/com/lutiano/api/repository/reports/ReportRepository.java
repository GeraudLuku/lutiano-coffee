package com.lutiano.api.repository.reports;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Set;

/** Read-only aggregates for the reports. Group and metric names are whitelisted here, never taken from the request as SQL. */
@Repository
public class ReportRepository {
  /** Sale-level groupings: label expression and join needs. */
  private static final Map<String, String> GROUPS = Map.of(
      "operation", "o.name", "seller", "u.full_name", "channel", "o.type",
      "day", "date(date_add(s.created_at, interval 1 hour))", "week", "date_format(date_add(s.created_at, interval 1 hour), '%x-W%v')",
      "month", "date_format(date_add(s.created_at, interval 1 hour), '%Y-%m')");
  /** Report-builder metrics: select expression per name. */
  private static final Map<String, String> METRICS = Map.of(
      "sales", "coalesce(sum(s.total_xaf),0)",
      "transactions", "count(*)",
      "units", "coalesce(sum((select sum(l.quantity) from sale_line l where l.sale_id=s.id)),0)",
      "avg_order_value", "coalesce(round(avg(s.total_xaf)),0)",
      "commission", "coalesce(sum((select sum(c.commission_xaf-c.reversed_xaf) from sale_commission c where c.sale_id=s.id)),0)",
      "points", "coalesce(sum((select a.total_points from loyalty_award a where a.sale_id=s.id)),0)");
  private static final String FROM = " from sale s join operation o on o.id=s.operation_id join staff_user u on u.id=s.seller_id";

  private final JdbcTemplate db;

  public ReportRepository(JdbcTemplate db) { this.db = db; }

  public static boolean knownGroup(String g) { return GROUPS.containsKey(g); }
  public static boolean knownMetric(String m) { return METRICS.containsKey(m); }
  public static Set<String> metrics() { return METRICS.keySet(); }

  /** Sale totals for a period: transactions, revenue, distinct sellers. */
  public Map<String, Object> totals(Timestamp from, Timestamp to, String operationId, String channel, String sellerId) {
    List<Object> a = new ArrayList<>(List.of(from, to));
    return db.queryForMap("select count(*) transactions, coalesce(sum(s.total_xaf),0) revenue_xaf, count(distinct s.seller_id) sellers," +
        " coalesce(sum((select sum(l.quantity) from sale_line l where l.sale_id=s.id)),0) units," +
        " coalesce(sum((select sum(c.commission_xaf-c.reversed_xaf) from sale_commission c where c.sale_id=s.id)),0) commission_xaf" +
        FROM + " where s.created_at>=? and s.created_at<?" + filters(a, operationId, channel, sellerId), a.toArray());
  }

  /** Revenue and transactions grouped by a whitelisted dimension. */
  public List<Map<String, Object>> grouped(String group, Timestamp from, Timestamp to, String operationId, String channel, String sellerId) {
    List<Object> a = new ArrayList<>(List.of(from, to));
    String key = GROUPS.get(group);
    return db.queryForList("select " + key + " label, count(*) transactions, coalesce(sum(s.total_xaf),0) revenue_xaf," +
        " coalesce(sum((select sum(l.quantity) from sale_line l where l.sale_id=s.id)),0) units" +
        FROM + " where s.created_at>=? and s.created_at<?" + filters(a, operationId, channel, sellerId) + " group by label order by " + ("day".equals(group) || "week".equals(group) || "month".equals(group) ? "label" : "revenue_xaf desc, label"), a.toArray());
  }

  /** Products by units and revenue (sale lines carry the product name sold). */
  public List<Map<String, Object>> byProduct(Timestamp from, Timestamp to, String operationId, String channel, String sellerId) {
    List<Object> a = new ArrayList<>(List.of(from, to));
    return db.queryForList("select l.product_name label, sum(l.quantity) units, sum(l.quantity*(l.unit_price_xaf+l.addons_unit_xaf)) revenue_xaf, count(distinct s.id) transactions" +
        " from sale_line l join sale s on s.id=l.sale_id join operation o on o.id=s.operation_id join staff_user u on u.id=s.seller_id where s.created_at>=? and s.created_at<?" +
        filters(a, operationId, channel, sellerId) + " group by l.product_name order by revenue_xaf desc, label limit 50", a.toArray());
  }

  /** The report builder: chosen metrics grouped by a whitelisted dimension. */
  public List<Map<String, Object>> builder(List<String> metrics, String group, Timestamp from, Timestamp to) {
    StringBuilder select = new StringBuilder(GROUPS.get(group) + " label");
    for (String m : metrics) select.append(", ").append(METRICS.get(m)).append(' ').append(m);
    return db.queryForList("select " + select + FROM + " where s.created_at>=? and s.created_at<? group by label order by " +
        ("day".equals(group) || "week".equals(group) || "month".equals(group) ? "label" : metrics.get(0) + " desc, label"), from, to);
  }

  public long refundedXaf(Timestamp from, Timestamp to) {
    Long v = db.queryForObject("select coalesce(sum(amount_xaf),0) from refund where status='PAID' and paid_at>=? and paid_at<?", Long.class, from, to);
    return v == null ? 0 : v;
  }

  private static String filters(List<Object> args, String operationId, String channel, String sellerId) {
    StringBuilder sb = new StringBuilder();
    if (operationId != null && !operationId.isBlank()) { sb.append(" and s.operation_id=?"); args.add(operationId); }
    if (channel != null && !channel.isBlank()) { sb.append(" and o.type=?"); args.add(channel); }
    if (sellerId != null && !sellerId.isBlank()) { sb.append(" and s.seller_id=?"); args.add(sellerId); }
    return sb.toString();
  }
}
