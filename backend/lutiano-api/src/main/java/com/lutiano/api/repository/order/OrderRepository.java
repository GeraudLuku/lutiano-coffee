package com.lutiano.api.repository.order;

import java.util.ArrayList;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;

/** Customer orders, their lines and their state changes. */
@Repository
public class OrderRepository {
  /** The order with who, where and the status group the apps show: PENDING, COMPLETED or CANCELLED. */
  private static final String SELECT = "select o.*, c.full_name customer_name, c.phone customer_phone, op.name operation_name," +
      " case when o.status in ('PLACED','ACCEPTED','READY') then 'PENDING' when o.status='COLLECTED' then 'COMPLETED' else 'CANCELLED' end status_group" +
      " from customer_order o join customer c on c.id=o.customer_id join operation op on op.id=o.operation_id";
  private static final Map<String, String> GROUPS = Map.of("PENDING", "o.status in ('PLACED','ACCEPTED','READY')", "COMPLETED", "o.status='COLLECTED'",
      "CANCELLED", "o.status in ('REJECTED','CANCELLED','EXPIRED')");
  private final JdbcTemplate db;

  public OrderRepository(JdbcTemplate db) { this.db = db; }

  public void insert(String id, String reference, String customerId, String operationId, long total, String note, String key) {
    db.update("insert into customer_order(id,reference,customer_id,operation_id,status,total_xaf,note,idempotency_key,placed_at) values(?,?,?,?,'PLACED',?,?,?,UTC_TIMESTAMP())",
        id, reference, customerId, operationId, total, note, key);
  }

  public void insertLine(String orderId, int lineNo, String productId, String name, long unit, long addonsUnit, int quantity) {
    db.update("insert into customer_order_line(order_id,line_no,product_id,product_name,unit_price_xaf,addons_unit_xaf,quantity) values(?,?,?,?,?,?,?)",
        orderId, lineNo, productId, name, unit, addonsUnit, quantity);
  }

  public void insertLineAddon(String orderId, int lineNo, String addonId, String name, long price) {
    db.update("insert into customer_order_line_addon(order_id,line_no,addon_id,addon_name,unit_price_xaf) values(?,?,?,?,?)", orderId, lineNo, addonId, name, price);
  }

  public Optional<Map<String, Object>> byKey(String key) { return db.queryForList(SELECT + " where o.idempotency_key=?", key).stream().findFirst(); }
  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where o.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> lines(String orderId) {
    List<Map<String, Object>> lines = db.queryForList("select line_no,product_id,product_name,unit_price_xaf,addons_unit_xaf,quantity from customer_order_line where order_id=? order by line_no", orderId);
    for (Map<String, Object> l : lines)
      l.put("addons", db.queryForList("select addon_id,addon_name,unit_price_xaf from customer_order_line_addon where order_id=? and line_no=? order by addon_name", orderId, l.get("line_no")));
    return lines;
  }

  /** One customer's orders, newest first; group is PENDING, COMPLETED or CANCELLED (null for all). */
  public PageResponse<Map<String, Object>> ofCustomer(String customerId, String group, PageRequest req) {
    return filter(group).and("o.customer_id=?", customerId).page(db, "order by o.placed_at desc, o.id", req);
  }

  /**
   * Orders at sales points a seller works at (operationIds), or at any when null. group as above, null for all. The summary counts what
   * still needs a hand: how many are waiting to be accepted, being prepared, and ready.
   */
  public PageResponse<Map<String, Object>> atOperations(List<String> operationIds, String group, String status, String operationId, PageRequest req) {
    SqlFilter f = filter(group).andIn("o.operation_id", operationIds).and("o.status=?", status).and("o.operation_id=?", operationId);
    // active orders first (oldest first: they wait longest), then the rest, newest first
    PageResponse<Map<String, Object>> page = f.page(db, "order by (o.status in ('PLACED','ACCEPTED','READY')) desc, case when o.status in ('PLACED','ACCEPTED','READY') then o.placed_at end asc, o.placed_at desc, o.id", req);
    SqlFilter counts = new SqlFilter("select o.status from customer_order o").andIn("o.operation_id", operationIds).and("o.operation_id=?", operationId);
    return page.withSummary(counts.aggregate(db, "coalesce(sum(status='PLACED'),0) placed, coalesce(sum(status='ACCEPTED'),0) accepted, coalesce(sum(status='READY'),0) ready"));
  }

  private static SqlFilter filter(String group) {
    SqlFilter f = new SqlFilter(SELECT);
    if (group != null && GROUPS.containsKey(group)) f.and(GROUPS.get(group));
    return f;
  }

  /** Moves the order on only when it is still in one of {@code from}; false when somebody else got there first. */
  public boolean transition(String id, List<String> from, String to, String column, String handledBy, String reason) {
    String in = String.join(",", from.stream().map(s -> "?").toList());
    List<Object> args = new ArrayList<>(List.of(to));
    args.add(handledBy);
    args.add(reason);
    args.add(id);
    args.addAll(from);
    return db.update("update customer_order set status=?, " + column + "=UTC_TIMESTAMP(), handled_by=coalesce(?,handled_by), reason=coalesce(?,reason) where id=? and status in (" + in + ")", args.toArray()) == 1;
  }

  public void setSale(String id, String saleId, long total) { db.update("update customer_order set sale_id=?, total_xaf=? where id=?", saleId, total, id); }

  /** Orders nobody picked up in time: still PLACED after {@code minutes}. */
  public List<Map<String, Object>> unhandledSince(int minutes) {
    return db.queryForList("select id,reference,customer_id from customer_order where status='PLACED' and placed_at < DATE_SUB(UTC_TIMESTAMP(), INTERVAL ? MINUTE)", minutes);
  }

  /** Orders accepted or ready that were never collected, placed before {@code dayStart}. */
  public List<Map<String, Object>> staleBefore(java.sql.Timestamp dayStart) {
    return db.queryForList("select id,reference,customer_id from customer_order where status in ('ACCEPTED','READY') and placed_at < ?", dayStart);
  }

  /** Points earned by a sale, for the thank-you message. */
  public long pointsOfSale(String saleId) {
    Long n = db.queryForObject("select coalesce(sum(total_points),0) from loyalty_award where sale_id=?", Long.class, saleId);
    return n == null ? 0 : n;
  }

  public String loyaltyToken(String customerId) { return db.queryForObject("select loyalty_token from customer where id=?", String.class, customerId); }
}
