package com.lutiano.api.repository.earnings;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Objects;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.definitions.enums.SearchType;

/** Commission rules (current and history) and the commission snapshot of each sale line. */
@Repository
public class CommissionRepository {
  private static final String RULE_SELECT = "select r.*, p.name product_name, p.sku, p.kind product_kind, p.category, u.full_name created_by_name" +
      " from commission_rule r join product p on p.id=r.product_id left join staff_user u on u.id=r.created_by";
  private static final String LINE_SELECT = "select c.*, s.reference sale_reference, s.created_at sale_created_at, l.product_name, o.name operation_name" +
      " from sale_commission c join sale s on s.id=c.sale_id join sale_line l on l.sale_id=c.sale_id and l.line_no=c.line_no" +
      " join operation o on o.id=s.operation_id";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public CommissionRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- rules -----

  public List<Map<String, Object>> currentRules(String sellerRole) {
    return new SqlFilter(RULE_SELECT).and("r.superseded_at is null").and("r.seller_role=?", sellerRole).list(db, "order by p.name, r.seller_role");
  }

  public PageResponse<Map<String, Object>> ruleHistory(String productId, String sellerRole, PageRequest req) {
    return new SqlFilter(RULE_SELECT).and("r.product_id=?", productId).and("r.seller_role=?", sellerRole).page(db, "order by r.created_at desc, r.id desc", req);
  }

  public Optional<Map<String, Object>> rule(String id) { return db.queryForList(RULE_SELECT + " where r.id=?", id).stream().findFirst(); }

  /** The current rule of a product and role, row-locked so concurrent changes are serialised. */
  public Optional<Map<String, Object>> lockCurrent(String productId, String sellerRole) {
    return db.queryForList("select * from commission_rule where product_id=? and seller_role=? and superseded_at is null for update", productId, sellerRole)
        .stream().findFirst();
  }

  public void insertRule(String id, String productId, String sellerRole, String basis, int rate, String note, String actorId) {
    db.update("insert into commission_rule(id,product_id,seller_role,basis,rate,note,created_by,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, productId, sellerRole, basis, rate, note, actorId);
    search.changed(SearchType.COMMISSION_RULE, id);
  }

  public void supersede(String id, String actorId) {
    db.update("update commission_rule set superseded_at=UTC_TIMESTAMP(), superseded_by=? where id=?", actorId, id);
    search.changed(SearchType.COMMISSION_RULE, id);
  }

  /** The rule for a sold product and role: the product's own, else its parent's. */
  public Optional<Map<String, Object>> ruleFor(String productId, String parentId, String sellerRole) {
    return db.queryForList("select * from commission_rule where superseded_at is null and seller_role=? and product_id in (?, coalesce(?, ?))" +
        " order by product_id=? desc limit 1", sellerRole, productId, parentId, productId, productId).stream().findFirst();
  }

  // ----- sale commissions -----

  /** The Douala day of the first sale that earned commission. */
  public Optional<LocalDate> firstSaleDay() {
    return db.queryForList("select date(date_add(min(s.created_at), interval 1 hour)) from sale_commission c join sale s on s.id=c.sale_id", LocalDate.class)
        .stream().filter(Objects::nonNull).findFirst();
  }

  /** A sale's lines with what commission needs: product, parent, quantity and paid line value. */
  public List<Map<String, Object>> saleLines(String saleId) {
    return db.queryForList("select l.line_no, l.product_id, p.parent_id, l.quantity, (l.unit_price_xaf+l.addons_unit_xaf)*l.quantity line_value_xaf" +
        " from sale_line l join product p on p.id=l.product_id where l.sale_id=? order by l.line_no", saleId);
  }

  public void insertLine(String saleId, int lineNo, String sellerId, String productId, Map<String, Object> rule, int quantity, long lineValue, long commission) {
    db.update("insert into sale_commission(sale_id,line_no,seller_id,product_id,rule_id,basis,rate,quantity,line_value_xaf,commission_xaf,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", saleId, lineNo, sellerId, productId, rule.get("id"), rule.get("basis"), rule.get("rate"), quantity, lineValue, commission);
  }

  /** {commission, reversed} of a sale, row-locked. */
  public Map<String, Object> lockSaleTotals(String saleId) {
    db.queryForList("select sale_id from sale_commission where sale_id=? for update", saleId);
    return db.queryForMap("select coalesce(sum(commission_xaf),0) commission, coalesce(sum(reversed_xaf),0) reversed, max(seller_id) seller_id," +
        " (select line_no from sale_commission where sale_id=? order by commission_xaf desc, line_no limit 1) top_line from sale_commission where sale_id=?", saleId, saleId);
  }

  public void addReversed(String saleId, int lineNo, long amount) {
    db.update("update sale_commission set reversed_xaf=reversed_xaf+? where sale_id=? and line_no=?", amount, saleId, lineNo);
  }

  public List<Map<String, Object>> forSale(String saleId) {
    return db.queryForList(LINE_SELECT + " where c.sale_id=? order by c.line_no", saleId);
  }

  /** Commission lines of a seller (or all) between two times, newest first, with the total over every match. */
  public PageResponse<Map<String, Object>> lines(String sellerId, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter(LINE_SELECT).and("c.seller_id=?", sellerId).and("s.created_at>=?", from).and("s.created_at<?", to);
    return f.page(db, "order by s.created_at desc, c.sale_id, c.line_no", req)
        .withSummary(f.aggregate(db, "count(*) line_count, coalesce(sum(commission_xaf),0) commission_xaf, coalesce(sum(line_value_xaf),0) sales_xaf"));
  }

  /** Commission per seller for sales between two times. */
  public List<Map<String, Object>> totalsBySeller(Timestamp from, Timestamp to) {
    return db.queryForList("select c.seller_id, coalesce(sum(c.commission_xaf),0) commission_xaf from sale_commission c join sale s on s.id=c.sale_id" +
        " where s.created_at>=? and s.created_at<? group by c.seller_id", from, to);
  }

  /** Commission per product for sales between two times (largest first). */
  public List<Map<String, Object>> totalsByProduct(Timestamp from, Timestamp to, int limit) {
    return db.queryForList("select l.product_name, sum(c.quantity) units, sum(c.commission_xaf) commission_xaf from sale_commission c" +
        " join sale s on s.id=c.sale_id join sale_line l on l.sale_id=c.sale_id and l.line_no=c.line_no where s.created_at>=? and s.created_at<?" +
        " group by l.product_name order by commission_xaf desc limit " + limit, from, to);
  }
}
