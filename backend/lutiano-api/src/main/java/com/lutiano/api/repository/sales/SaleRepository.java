package com.lutiano.api.repository.sales;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.utils.Candidates;
import java.util.ArrayList;
import java.util.HashMap;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class SaleRepository {
  private static final String SELECT =
      "select s.id,s.reference,s.seller_id,s.operation_id,s.shift_id,s.total_xaf,s.payment_method,s.cash_received_xaf,s.created_at,s.customer_id," +
      "(select c.full_name from customer c where c.id=s.customer_id) customer_name,(select c.code from customer c where c.id=s.customer_id) customer_code," +
      "o.name operation_name,o.code operation_code,o.type operation_type,u.full_name seller_name," +
      "(select coalesce(sum(l.quantity),0) from sale_line l where l.sale_id=s.id) item_count," +
      "(select coalesce(sum(r.amount_xaf),0) from refund r where r.sale_id=s.id and r.status='PAID') refunded_xaf," +
      "(select count(*) from refund r where r.sale_id=s.id and r.status in ('REQUESTED','APPROVED')) open_refunds" +
      " from sale s join operation o on o.id=s.operation_id join staff_user u on u.id=s.seller_id";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public SaleRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public Optional<Map<String, Object>> findByIdempotencyKey(String key) {
    return db.queryForList("select id,seller_id from sale where idempotency_key=?", key).stream().findFirst();
  }

  public Optional<String> sellerOf(String saleId) {
    return db.queryForList("select seller_id from sale where id=?", String.class, saleId).stream().findFirst();
  }

  public void insert(String id, String reference, String sellerId, String operationId, String shiftId, String key, long total, Long cashReceived) {
    db.update("insert into sale(id,reference,seller_id,operation_id,shift_id,idempotency_key,total_xaf,payment_method,cash_received_xaf,created_at) values(?,?,?,?,?,?,?,'CASH',?,UTC_TIMESTAMP())",
        id, reference, sellerId, operationId, shiftId, key, total, cashReceived);
    search.changed(SearchType.SALE, id);
  }

  public void insertLine(String saleId, int lineNo, String productId, String productName, long unitPrice, long addonsUnit, int quantity) {
    db.update("insert into sale_line(sale_id,line_no,product_id,product_name,unit_price_xaf,addons_unit_xaf,quantity) values(?,?,?,?,?,?,?)",
        saleId, lineNo, productId, productName, unitPrice, addonsUnit, quantity);
  }

  public void insertLineAddon(String saleId, int lineNo, String addonId, String addonName, long unitPrice) {
    db.update("insert into sale_line_addon(sale_id,line_no,addon_id,addon_name,unit_price_xaf) values(?,?,?,?,?)", saleId, lineNo, addonId, addonName, unitPrice);
  }

  public void setCustomer(String saleId, String customerId) {
    db.update("update sale set customer_id=? where id=?", customerId, saleId);
    search.changed(SearchType.SALE, saleId);
  }

  public Map<String, Object> findById(String id) { return db.queryForMap(SELECT + " where s.id=?", id); }

  /** Lines with their add-ons; line_total_xaf = (unit price + add-ons) × quantity. */
  public List<Map<String, Object>> lines(String saleId) {
    List<Map<String, Object>> lines = db.queryForList(
        "select l.line_no,l.product_id,l.product_name,l.unit_price_xaf,l.addons_unit_xaf,l.quantity,((l.unit_price_xaf+l.addons_unit_xaf)*l.quantity) line_total_xaf," +
        "p.image_url,p.sku,p.variant_name from sale_line l left join product p on p.id=l.product_id where l.sale_id=? order by l.line_no", saleId);
    Map<Object, List<Map<String, Object>>> addons = new HashMap<>();
    for (Map<String, Object> a : db.queryForList("select line_no,addon_id,addon_name,unit_price_xaf from sale_line_addon where sale_id=? order by addon_name", saleId))
      addons.computeIfAbsent(((Number) a.get("line_no")).intValue(), k -> new ArrayList<>()).add(a);
    for (Map<String, Object> l : lines) l.put("addons", addons.getOrDefault(((Number) l.get("line_no")).intValue(), List.of()));
    return lines;
  }

  /** One page of sales, newest first, with count, revenue and units over every match. */
  public PageResponse<Map<String, Object>> search(String sellerId, String operationId, String channel, String paymentMethod, Timestamp from, Timestamp to,
                                          Candidates text, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("s.seller_id=?", sellerId).and("s.operation_id=?", operationId)
        .and("o.type=?", channel).and("s.payment_method=?", paymentMethod).and("s.created_at>=?", from).and("s.created_at<?", to)
        .searchIds(text, "s.id");
    return f.page(db, "order by s.created_at desc, s.reference desc", req)
        .withSummary(f.aggregate(db, "count(*) sales, coalesce(sum(total_xaf),0) revenue_xaf, coalesce(sum(item_count),0) units," +
            " coalesce(sum((select coalesce(sum(a.total_points),0) from loyalty_award a where a.sale_id=filtered.id)),0) points_issued," +
            " coalesce(sum((select coalesce(sum(c.commission_xaf-c.reversed_xaf),0) from sale_commission c where c.sale_id=filtered.id)),0) commission_xaf"));
  }




  /** The seller's own figures since {@code since}: sales, customers served, points issued to them, and commission earned net of refunds. */
  public Map<String, Object> summarySince(String sellerId, Timestamp since) {
    return db.queryForMap("""
        select count(*) orders, coalesce(sum(s.total_xaf),0) sales_xaf, count(distinct s.customer_id) customers,
               coalesce(sum((select sum(a.total_points - a.reversed_points) from loyalty_award a where a.sale_id = s.id)),0) points_issued,
               coalesce(sum((select sum(c.commission_xaf - c.reversed_xaf) from sale_commission c where c.sale_id = s.id)),0) commission_xaf
          from sale s where s.seller_id = ? and s.created_at >= ?""", sellerId, since);
  }

  /** The seller's sales per Douala day since {@code since} (days without a sale are absent). */
  public List<Map<String, Object>> dailySalesSince(String sellerId, Timestamp since) {
    return db.queryForList("select date(date_add(created_at, interval 1 hour)) day, coalesce(sum(total_xaf),0) sales_xaf from sale"
        + " where seller_id = ? and created_at >= ? group by day order by day", sellerId, since);
  }

  public Map<String, Object> sellerTotalsSince(String sellerId, Timestamp since) {
    return db.queryForMap("select count(*) sales, coalesce(sum(total_xaf),0) revenue_xaf from sale where seller_id=? and created_at>=?", sellerId, since);
  }
}
