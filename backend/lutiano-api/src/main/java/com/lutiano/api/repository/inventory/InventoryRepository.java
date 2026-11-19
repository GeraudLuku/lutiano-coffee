package com.lutiano.api.repository.inventory;

import com.lutiano.api.utils.SqlFilter;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;
import com.lutiano.api.definitions.enums.SearchType;

/** Stock balances (central and per operation), receipts, discrepancies and reorder queries. */
@Repository
public class InventoryRepository {
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public InventoryRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- balances (change them only through StockLedger) -----

  private static final String OPERATION_BALANCES =
      "select ? operation_id,p.id product_id,p.name,p.sku,p.category,p.price_xaf,p.image_url,p.low_stock_threshold,p.unit,p.track_stock," +
      " p.kind,p.parent_id,p.variant_name,coalesce(b.quantity,0) quantity" +
      " from product p left join product pp on pp.id=p.parent_id left join inventory_balance b on b.product_id=p.id and b.operation_id=?" +
      " where p.kind<>'PARENT' and ((p.active=true and (pp.id is null or pp.active=true)) or coalesce(b.quantity,0)>0)";

  /**
   * One page of an operation's stock, filtered by text, category, level (out, low, attention = low or out) or product ids;
   * {@code lowestFirst} puts the smallest quantities first instead of grouping by category.
   * The summary covers the operation's whole stock, not the filter: products, units held, value at selling price,
   * products needing attention, and the categories (for filter chips).
   */
  public PageResponse<Map<String, Object>> operationBalancesPage(String operationId, String q, String category, String status, List<String> ids, boolean lowestFirst, PageRequest req) {
    SqlFilter f = new SqlFilter("select * from (" + OPERATION_BALANCES + ") b").and("b.category=?", category).search(q, "b.name", "b.sku").andIn("b.product_id", ids);
    if ("out".equals(status)) f.and("b.quantity<=0");
    else if ("low".equals(status)) f.and("b.quantity>0 and b.quantity<=b.low_stock_threshold");
    else if ("attention".equals(status)) f.and("b.quantity<=b.low_stock_threshold");
    // the base select takes the operation id twice, ahead of the filter values
    Object[] args = Stream.concat(Stream.of(operationId, operationId), Arrays.stream(f.args())).toArray();
    Long total = db.queryForObject("select count(*) from (" + f.sql("") + ") counted", Long.class, args);
    List<Object> pageArgs = new ArrayList<>(Arrays.asList(args));
    pageArgs.add(req.size());
    pageArgs.add(req.offset());
    List<Map<String, Object>> rows = db.queryForList(f.sql((lowestFirst ? "order by b.quantity, b.name, b.product_id" : "order by b.category, b.name, b.product_id") + " limit ? offset ?"), pageArgs.toArray());
    Map<String, Object> summary = db.queryForMap("select count(*) products, coalesce(sum(case when quantity>0 then quantity end),0) units,"
        + " coalesce(sum(case when quantity>0 then quantity*price_xaf end),0) value_xaf, coalesce(sum(quantity<=low_stock_threshold),0) attention"
        + " from (" + OPERATION_BALANCES + ") b", operationId, operationId);
    summary.put("categories", db.queryForList("select distinct category from (" + OPERATION_BALANCES + ") b order by category", String.class, operationId, operationId));
    return PageResponse.of(rows, req, total == null ? 0 : total, summary);
  }

  /** Every operation's stock, one page at a time (staff). */
  public PageResponse<Map<String, Object>> allOperationBalancesPage(PageRequest req) {
    SqlFilter f = new SqlFilter("select b.operation_id,o.name operation_name,o.code operation_code,b.product_id,p.name,p.sku,p.category,p.price_xaf,p.image_url,p.low_stock_threshold,b.quantity"
        + " from inventory_balance b join product p on p.id=b.product_id join operation o on o.id=b.operation_id");
    return f.page(db, "order by o.name,p.name,b.product_id", req);
  }

  public List<Map<String, Object>> centralBalances() {
    return db.queryForList("select p.id product_id,p.name,p.sku,p.category,p.image_url,p.active,p.low_stock_threshold,p.unit,coalesce(c.quantity,0) quantity" +
        " from product p left join central_inventory c on c.product_id=p.id where p.kind<>'PARENT' and p.track_stock=true order by p.name");
  }

  /** Current quantity; operationId null means central stock. */
  public int quantity(String operationId, String productId) {
    List<Integer> rows = operationId == null
        ? db.queryForList("select quantity from central_inventory where product_id=?", Integer.class, productId)
        : db.queryForList("select quantity from inventory_balance where operation_id=? and product_id=?", Integer.class, operationId, productId);
    return rows.isEmpty() || rows.get(0) == null ? 0 : rows.get(0);
  }

  public void addCentral(String productId, int quantity) {
    db.update("insert into central_inventory(product_id,quantity) values(?,?) on duplicate key update quantity=quantity+values(quantity)", productId, quantity);
  }

  public boolean takeCentral(String productId, int quantity) {
    return db.update("update central_inventory set quantity=quantity-? where product_id=? and quantity>=?", quantity, productId, quantity) == 1;
  }

  public void addToOperation(String operationId, String productId, int quantity) {
    db.update("insert into inventory_balance(operation_id,product_id,quantity) values(?,?,?) on duplicate key update quantity=quantity+values(quantity)", operationId, productId, quantity);
  }

  public boolean takeFromOperation(String operationId, String productId, int quantity) {
    return db.update("update inventory_balance set quantity=quantity-? where operation_id=? and product_id=? and quantity>=?", quantity, operationId, productId, quantity) == 1;
  }

  // ----- receipts -----

  public void insertReceipt(String id, String productId, int quantity, String actorId, String source, String supplier, String reference, String note) {
    db.update("insert into stock_receipt(id,product_id,quantity,actor_id,source,supplier,reference,note,created_at) values(?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, productId, quantity, actorId, source, supplier, reference, note);
  }

  public PageResponse<Map<String, Object>> receipts(PageRequest req) {
    return new SqlFilter("select r.*,p.name product_name,p.sku,u.full_name actor_name from stock_receipt r join product p on p.id=r.product_id left join staff_user u on u.id=r.actor_id")
        .page(db, "order by r.created_at desc, r.id desc", req);
  }

  // ----- discrepancies -----

  public void insertDiscrepancy(String id, String allocationId, int sent, int received, String note) {
    db.update("insert into stock_discrepancy(id,allocation_id,sent_quantity,received_quantity,note,created_at) values(?,?,?,?,?,UTC_TIMESTAMP())",
        id, allocationId, sent, received, note);
    search.changed(SearchType.DISCREPANCY, id);
  }

  public PageResponse<Map<String, Object>> discrepancies(PageRequest req) {
    return new SqlFilter("select d.*,p.name product_name,o.name operation_name,u.full_name confirmed_by_name from stock_discrepancy d join stock_allocation a on a.id=d.allocation_id" +
        " join product p on p.id=a.product_id join operation o on o.id=a.operation_id left join staff_user u on u.id=a.confirmed_by").page(db, "order by d.created_at desc, d.id desc", req);
  }

  // ----- reorder -----

  /**
   * Open stock alerts (products at or below their threshold, centrally and at active operations) with today's quantity.
   * `inbound` is stock already requested, approved or in transit to that operation.
   */
  public List<Map<String, Object>> reorderAlerts() {
    return db.queryForList(
        "select * from (" +
        " select null operation_id, 'Central stock' operation_name, null operation_code, p.id product_id, p.name, p.sku, p.image_url, p.low_stock_threshold, coalesce(c.quantity,0) quantity, 0 inbound" +
        "  from stock_alert a join product p on p.id=a.product_id left join central_inventory c on c.product_id=p.id" +
        "  where a.scope_id='CENTRAL' and p.active=true" +
        " union all" +
        " select o.id, o.name, o.code, p.id, p.name, p.sku, p.image_url, p.low_stock_threshold, b.quantity," +
        "  coalesce((select sum(a.sent_quantity) from stock_allocation a where a.operation_id=o.id and a.product_id=p.id and a.status in ('PENDING_APPROVAL','APPROVED','IN_TRANSIT')),0)" +
        "  from stock_alert a join product p on p.id=a.product_id join operation o on o.id=a.scope_id join inventory_balance b on b.operation_id=o.id and b.product_id=p.id" +
        "  where p.active=true and o.active=true" +
        ") r order by (quantity - low_stock_threshold), name");
  }
}
