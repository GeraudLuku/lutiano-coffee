package com.lutiano.api.repository.inventory;

import java.util.LinkedHashMap;
import java.util.Arrays;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Append-only stock ledger. Rows are inserted by StockLedger and never updated or deleted. */
@Repository
public class MovementRepository {
  private static final String SELECT =
      "select m.*, p.name product_name, p.sku, coalesce(o.name,'Central stock') location_name, o.code location_code, u.full_name actor_name" +
      " from stock_movement m join product p on p.id=m.product_id left join operation o on o.id=m.operation_id left join staff_user u on u.id=m.actor_id";
  private final JdbcTemplate db;

  public MovementRepository(JdbcTemplate db) { this.db = db; }

  public void insert(String operationId, String productId, int delta, int balanceAfter, String type, String refType, String refId, String refLabel, String actorId, String note) {
    db.update("insert into stock_movement(id,operation_id,product_id,quantity_delta,balance_after,movement_type,reference_type,reference_id,reference_label,actor_id,note,created_at) values(?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        UUID.randomUUID().toString(), operationId, productId, delta, balanceAfter, type, refType, refId, refLabel, actorId, note);
  }

  /**
   * @param location null = every location; "CENTRAL" = central stock only; otherwise an operation id.
   */
  public PageResponse<Map<String, Object>> search(String location, String productId, String type, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT)
        .and("m.product_id=?", productId).and("m.movement_type=?", type)
        .and("m.created_at>=?", from).and("m.created_at<?", to);
    if ("CENTRAL".equals(location)) f.and("m.operation_id is null");
    else f.and("m.operation_id=?", location);
    return f.page(db, "order by m.created_at desc, m.id", req);
  }

  /**
   * Movements at one operation in the shape the Seller app reads (direction, quantity, source…).
   * {@code direction}: IN (stock added), OUT (stock removed) or null for both; {@code from} (inclusive) and {@code to} (exclusive) bound the time, either may be null.
   */
  public PageResponse<Map<String, Object>> forOperation(String operationId, String productId, String direction, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter(
        "select case when m.quantity_delta>0 then 'IN' else 'OUT' end direction, m.product_id, p.name product_name," +
        " abs(m.quantity_delta) quantity, m.balance_after, m.created_at occurred_at, m.movement_type, m.reference_id source_id, m.reference_label reference," +
        " case m.movement_type when 'TRANSFER_IN' then 'Allocation received' when 'SALE' then 'Sale' when 'ADJUSTMENT' then 'Stock adjustment'" +
        "  when 'STOCKTAKE' then 'Stock count' when 'RETURN_OUT' then 'Returned to central' when 'OPENING_BALANCE' then 'Opening balance' else m.movement_type end source" +
        " from stock_movement m join product p on p.id=m.product_id")
        .and("m.operation_id=?", operationId).and("m.product_id=?", productId)
        .and("m.created_at>=?", from).and("m.created_at<?", to);
    if ("IN".equals(direction)) f.and("m.quantity_delta>0");
    else if ("OUT".equals(direction)) f.and("m.quantity_delta<0");
    return f.page(db, "order by m.created_at desc, m.id desc", req);
  }

  /** Units that came in and went out at one operation over a period (optionally one product), and the most consumed products. */
  public Map<String, Object> summary(String operationId, String productId, Timestamp from, Timestamp to, int topCount) {
    Map<String, Object> totals = scoped(operationId, productId, from, to).aggregate(db,
        "coalesce(sum(case when quantity_delta>0 then quantity_delta end),0) received, coalesce(sum(case when quantity_delta<0 then -quantity_delta end),0) consumed");
    long received = ((Number) totals.get("received")).longValue(), consumed = ((Number) totals.get("consumed")).longValue();
    SqlFilter out = scoped(operationId, productId, from, to).and("m.quantity_delta<0");
    List<Map<String, Object>> top = db.queryForList(
        "select product_id, product_name, sum(-quantity_delta) quantity from (" + out.sql("") + ") consumed group by product_id, product_name order by quantity desc, product_name limit ?",
        concat(out.args(), topCount));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("received", received);
    result.put("consumed", consumed);
    result.put("net", received - consumed);
    result.put("top_consumed", top);
    return result;
  }

  private static SqlFilter scoped(String operationId, String productId, Timestamp from, Timestamp to) {
    return new SqlFilter("select m.product_id, p.name product_name, m.quantity_delta from stock_movement m join product p on p.id=m.product_id")
        .and("m.operation_id=?", operationId).and("m.product_id=?", productId).and("m.created_at>=?", from).and("m.created_at<?", to);
  }

  private static Object[] concat(Object[] args, Object extra) {
    Object[] all = Arrays.copyOf(args, args.length + 1);
    all[args.length] = extra;
    return all;
  }
}
