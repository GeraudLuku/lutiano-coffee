package com.lutiano.api.repository.earnings;

import java.util.HashMap;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.SqlFilter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Reads of one seller's own earnings: their sales and commission, payouts and the rules that apply to their role. */
@Repository
public class MyEarningsRepository {
  private static final String SALE_COMMISSION = "(select coalesce(sum(c.commission_xaf),0) from sale_commission c where c.sale_id=s.id)";
  private final JdbcTemplate db;

  public MyEarningsRepository(JdbcTemplate db) { this.db = db; }

  // ----- totals -----

  /** Number and value of the seller's sales in [from, to). */
  public Map<String, Object> sales(String sellerId, Timestamp from, Timestamp to) {
    return db.queryForMap("select count(*) sales_count, coalesce(sum(total_xaf),0) sales_xaf from sale where seller_id=? and created_at>=? and created_at<?", sellerId, from, to);
  }

  public long commission(String sellerId, Timestamp from, Timestamp to) {
    Long v = db.queryForObject("select coalesce(sum(c.commission_xaf),0) from sale_commission c join sale s on s.id=c.sale_id where c.seller_id=? and s.created_at>=? and s.created_at<?", Long.class, sellerId, from, to);
    return v == null ? 0 : v;
  }

  public long adjustments(String sellerId, Timestamp from, Timestamp to) {
    Long v = db.queryForObject("select coalesce(sum(amount_xaf),0) from earning_adjustment where seller_id=? and created_at>=? and created_at<?", Long.class, sellerId, from, to);
    return v == null ? 0 : v;
  }

  /** Commission split into what the percentage earned on add-ons and the rest (products). */
  public Map<String, Object> commissionBySource(String sellerId, Timestamp from, Timestamp to) {
    return db.queryForMap("select coalesce(sum(c.commission_xaf),0) total_xaf," +
        " coalesce(sum(case when c.basis='PERCENT' then floor(l.addons_unit_xaf*l.quantity*c.rate/10000) else 0 end),0) addons_xaf" +
        " from sale_commission c join sale s on s.id=c.sale_id join sale_line l on l.sale_id=c.sale_id and l.line_no=c.line_no" +
        " where c.seller_id=? and s.created_at>=? and s.created_at<?", sellerId, from, to);
  }

  /** The products that earned most commission (largest first). */
  public List<Map<String, Object>> topProducts(String sellerId, Timestamp from, Timestamp to, int limit) {
    return db.queryForList("select l.product_name, sum(c.quantity) units, sum(c.commission_xaf) commission_xaf from sale_commission c" +
        " join sale s on s.id=c.sale_id join sale_line l on l.sale_id=c.sale_id and l.line_no=c.line_no" +
        " where c.seller_id=? and s.created_at>=? and s.created_at<? group by l.product_name order by commission_xaf desc, l.product_name limit " + limit, sellerId, from, to);
  }

  /** Sales, their value and commission per operation. */
  public List<Map<String, Object>> byOperation(String sellerId, Timestamp from, Timestamp to) {
    return db.queryForList("select o.id operation_id, o.name operation_name, count(*) sales_count, coalesce(sum(s.total_xaf),0) sales_xaf," +
        " coalesce(sum(" + SALE_COMMISSION + "),0) commission_xaf from sale s join operation o on o.id=s.operation_id" +
        " where s.seller_id=? and s.created_at>=? and s.created_at<? group by o.id, o.name order by commission_xaf desc, o.name", sellerId, from, to);
  }

  // ----- series -----

  /** Commission per Douala day in [from, to). */
  public Map<String, Long> commissionByDay(String sellerId, Timestamp from, Timestamp to) {
    return byDay("select " + Periods.businessDate("s.created_at") + " day, sum(c.commission_xaf) amount from sale_commission c join sale s on s.id=c.sale_id" +
        " where c.seller_id=? and s.created_at>=? and s.created_at<? group by day", sellerId, from, to);
  }

  /** Adjustments per Douala day in [from, to). */
  public Map<String, Long> adjustmentsByDay(String sellerId, Timestamp from, Timestamp to) {
    return byDay("select " + Periods.businessDate("created_at") + " day, sum(amount_xaf) amount from earning_adjustment" +
        " where seller_id=? and created_at>=? and created_at<? group by day", sellerId, from, to);
  }

  private Map<String, Long> byDay(String sql, String sellerId, Timestamp from, Timestamp to) {
    Map<String, Long> out = new HashMap<>();
    for (Map<String, Object> r : db.queryForList(sql, sellerId, from, to)) out.put(String.valueOf(r.get("day")), ((Number) r.get("amount")).longValue());
    return out;
  }

  // ----- sales with commission -----

  /** The seller's sales that earned commission in [from, to), newest first, with the commission of each. */
  public PageResponse<Map<String, Object>> salesWithCommission(String sellerId, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter("select s.id sale_id, s.reference, s.created_at, s.total_xaf, o.name operation_name, " + SALE_COMMISSION + " commission_xaf," +
        " (select coalesce(sum(c.reversed_xaf),0) from sale_commission c where c.sale_id=s.id) reversed_xaf" +
        " from sale s join operation o on o.id=s.operation_id")
        .and("s.seller_id=?", sellerId).and("s.created_at>=?", from).and("s.created_at<?", to)
        .and("exists (select 1 from sale_commission c where c.sale_id=s.id)");
    return f.page(db, "order by s.created_at desc, s.id", req);
  }

  public Optional<Map<String, Object>> saleOf(String saleId, String sellerId) {
    return db.queryForList("select s.id sale_id, s.reference, s.created_at, s.total_xaf, o.name operation_name from sale s join operation o on o.id=s.operation_id where s.id=? and s.seller_id=?", saleId, sellerId)
        .stream().findFirst();
  }

  // ----- statements -----

  public Optional<Map<String, Object>> frozenStatement(String periodId, String sellerId) {
    return db.queryForList("select * from seller_earning where period_id=? and seller_id=?", periodId, sellerId).stream().findFirst();
  }

  /** Manual and system adjustments in [from, to), newest first (for the statement). */
  public List<Map<String, Object>> adjustmentLines(String sellerId, Timestamp from, Timestamp to, int limit) {
    return db.queryForList("select reference, adjustment_type, amount_xaf, note, created_at from earning_adjustment where seller_id=? and created_at>=? and created_at<?" +
        " order by created_at desc, reference desc limit " + limit, sellerId, from, to);
  }

  // ----- payouts -----

  private static final String PAYOUT_SELECT = "select p.id, p.reference, p.amount_xaf, p.method, p.status, p.requested_at, p.paid_at, p.destination_phone, p.destination_name," +
      " p.provider_reference, ep.starts_on period_starts_on, ep.ends_on period_ends_on, p.period_id from payout p join earning_period ep on ep.id=p.period_id";

  /** Payouts of the seller, newest first, filtered by the statuses in [statuses] (all when empty); the summary counts and totals all of them. */
  public PageResponse<Map<String, Object>> payouts(String sellerId, List<String> statuses, PageRequest req) {
    SqlFilter f = new SqlFilter(PAYOUT_SELECT).and("p.seller_id=?", sellerId).andIn("p.status", statuses);
    Map<String, Object> summary = db.queryForMap("select" +
        " coalesce(sum(status='PAID'),0) completed, coalesce(sum(status in ('PENDING_APPROVAL','APPROVED','REVERSAL_PENDING')),0) pending," +
        " coalesce(sum(status in ('REJECTED','CANCELLED','REVERSED')),0) cancelled," +
        " coalesce(sum(case when status in ('PAID','REVERSAL_PENDING') then amount_xaf else 0 end),0) paid_out_xaf from payout where seller_id=?", sellerId);
    return f.page(db, "order by p.requested_at desc, p.id", req).withSummary(summary);
  }

  public Optional<Map<String, Object>> payout(String id, String sellerId) {
    return db.queryForList(PAYOUT_SELECT + " where p.id=? and p.seller_id=?", id, sellerId).stream().findFirst();
  }

  // ----- rules -----

  /** The commission rules in force for a seller role, by product. */
  public PageResponse<Map<String, Object>> rulesFor(String sellerRole, PageRequest req) {
    return new SqlFilter("select r.id, r.basis, r.rate, r.created_at, p.name product_name, p.category from commission_rule r join product p on p.id=r.product_id")
        .and("r.superseded_at is null").and("r.seller_role=?", sellerRole).and("p.active=true").page(db, "order by p.name, r.id", req);
  }
}
