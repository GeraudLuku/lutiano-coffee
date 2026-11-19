package com.lutiano.api.repository.dashboard;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Periods;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Read-only aggregate queries for the admin dashboard. */
@Repository
public class DashboardRepository {
  private final JdbcTemplate db;

  public DashboardRepository(JdbcTemplate db) { this.db = db; }

  public Map<String, Object> totals(Timestamp from, Timestamp to) {
    return db.queryForMap("select count(*) sales, coalesce(sum(s.total_xaf),0) revenue_xaf," +
        " coalesce((select sum(l.quantity) from sale_line l join sale x on x.id=l.sale_id where x.created_at>=? and x.created_at<?),0) items" +
        " from sale s where s.created_at>=? and s.created_at<?", from, to, from, to);
  }

  public int count(String sql, Object... args) {
    Integer n = db.queryForObject(sql, Integer.class, args);
    return n == null ? 0 : n;
  }

  public int activeOperations() { return count("select count(*) from operation where active=true"); }
  public int openShifts() { return count("select count(*) from cash_shift where status='OPEN'"); }
  public int pendingApprovals() {
    return count("select (select count(distinct coalesce(request_id, id)) from stock_allocation where status in ('PENDING_APPROVAL','APPROVED'))" +   // a request counts once, whatever its products
        " + (select count(*) from stock_adjustment where status='PENDING_APPROVAL') + (select count(*) from stocktake where status='SUBMITTED')");
  }
  public int inTransit() { return count("select count(distinct coalesce(request_id, id)) from stock_allocation where status='IN_TRANSIT'"); }
  public int discrepanciesSince(Timestamp from) { return count("select count(*) from stock_discrepancy where created_at>=?", from); }
  public int cashVariancesSince(Timestamp from) { return count("select count(*) from cash_shift where status='CLOSED' and counted_cash<>expected_cash and closed_at>=?", from); }
  public int lowCentralStock() { return count("select count(*) from stock_alert a join product p on p.id=a.product_id where a.scope_id='CENTRAL' and p.active=true"); }
  public int lowOperationStock() { return count("select count(*) from stock_alert a join product p on p.id=a.product_id join operation o on o.id=a.scope_id where p.active=true and o.active=true"); }

  public List<Map<String, Object>> salesByDay(Timestamp from, Timestamp to) {
    String day = Periods.businessDate("created_at");
    return db.queryForList("select " + day + " d, count(*) sales, coalesce(sum(total_xaf),0) revenue_xaf from sale where created_at>=? and created_at<? group by " + day, from, to);
  }

  public List<Map<String, Object>> salesByChannel(Timestamp from, Timestamp to) {
    return db.queryForList("select o.type channel, count(*) sales, coalesce(sum(s.total_xaf),0) revenue_xaf from sale s join operation o on o.id=s.operation_id where s.created_at>=? and s.created_at<? group by o.type", from, to);
  }

  public List<Map<String, Object>> topProducts(Timestamp from, Timestamp to, int limit) {
    return db.queryForList("select l.product_id, l.product_name, sum(l.quantity) units, sum(l.quantity*(l.unit_price_xaf+l.addons_unit_xaf)) revenue_xaf from sale_line l join sale s on s.id=l.sale_id where s.created_at>=? and s.created_at<? group by l.product_id,l.product_name order by revenue_xaf desc limit " + limit, from, to);
  }

  public List<Map<String, Object>> topSellers(Timestamp from, Timestamp to, int limit) {
    return db.queryForList("select s.seller_id, u.full_name seller_name, count(*) sales, coalesce(sum(s.total_xaf),0) revenue_xaf" +
        " from sale s join staff_user u on u.id=s.seller_id where s.created_at>=? and s.created_at<? group by s.seller_id,u.full_name order by revenue_xaf desc limit " + limit, from, to);
  }

  public List<Map<String, Object>> paymentMix(Timestamp from, Timestamp to) {
    return db.queryForList("select payment_method, count(*) sales, coalesce(sum(total_xaf),0) revenue_xaf from sale where created_at>=? and created_at<? group by payment_method order by revenue_xaf desc", from, to);
  }

  /** Active sellers with no completed sale since `since` (or never), for manager review. Sellers are never deactivated automatically. */
  public List<Map<String, Object>> inactiveSellers(Timestamp since) {
    return db.queryForList("select u.id seller_id, u.full_name seller_name, u.created_at, u.last_login_at," +
        " (select max(s.created_at) from sale s where s.seller_id=u.id) last_sale_at," +
        " (select count(*) from seller_operation x join operation o on o.id=x.operation_id where x.seller_id=u.id and o.active=true) active_operations" +
        " from staff_user u where u.role='SELLER' and u.active=true" +
        " having last_sale_at is null or last_sale_at<? order by last_sale_at is not null, last_sale_at", since);
  }

  /** Value of stock held centrally and at active locations, at selling prices (tracked, active products). */
  public long inventoryValue() {
    Long v = db.queryForObject("select (select coalesce(sum(c.quantity*p.price_xaf),0) from central_inventory c join product p on p.id=c.product_id where p.active=true and p.track_stock=true)" +
        " + (select coalesce(sum(b.quantity*p.price_xaf),0) from inventory_balance b join product p on p.id=b.product_id join operation o on o.id=b.operation_id where p.active=true and p.track_stock=true and o.active=true)", Long.class);
    return v == null ? 0 : v;
  }

  /** Loyalty members, members who joined in the period, and points earned and redeemed in it. */
  public Map<String, Object> loyaltyTotals(Timestamp from, Timestamp to) {
    return db.queryForMap("select (select count(*) from customer) customers, (select count(*) from customer where created_at>=? and created_at<?) new_customers," +
        " (select coalesce(sum(points),0) from loyalty_ledger where entry_type='EARNED' and created_at>=? and created_at<?) points_earned," +
        " (select coalesce(-sum(points),0) from loyalty_ledger where entry_type in ('REDEEMED','REDEMPTION_REVERSED') and created_at>=? and created_at<?) points_redeemed",
        from, to, from, to, from, to);
  }

  /** Commission earned on sales in the period, after refund reversals. */
  public long commissionsEarned(Timestamp from, Timestamp to) {
    Long v = db.queryForObject("select coalesce(sum(commission_xaf-reversed_xaf),0) from sale_commission where created_at>=? and created_at<?", Long.class, from, to);
    return v == null ? 0 : v;
  }

  /** Payouts paid out in the period. */
  public long payoutsPaid(Timestamp from, Timestamp to) {
    Long v = db.queryForObject("select coalesce(sum(amount_xaf),0) from payout where status in ('PAID','REVERSAL_PENDING') and paid_at>=? and paid_at<?", Long.class, from, to);
    return v == null ? 0 : v;
  }

  /** Active products, lowest stock relative to threshold first. */
  public List<Map<String, Object>> inventoryStatus(int limit) {
    return db.queryForList("select p.id product_id,p.name,p.sku,p.image_url,p.low_stock_threshold,coalesce(c.quantity,0) quantity from product p left join central_inventory c on c.product_id=p.id where p.active=true and p.kind<>'PARENT' and p.track_stock=true order by (coalesce(c.quantity,0) - p.low_stock_threshold), p.name limit " + limit);
  }
}
