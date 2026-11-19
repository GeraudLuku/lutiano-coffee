package com.lutiano.api.repository.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class StocktakeRepository {
  private static final String SELECT =
      "select s.*, coalesce(o.name,'Central stock') location_name, o.code location_code," +
      " cb.full_name created_by_name, sb.full_name submitted_by_name, db.full_name decided_by_name," +
      " (select count(*) from stocktake_line l where l.stocktake_id=s.id) line_count," +
      " (select count(*) from stocktake_line l where l.stocktake_id=s.id and l.counted_quantity is not null) counted_count," +
      " (select count(*) from stocktake_line l where l.stocktake_id=s.id and l.counted_quantity is not null and l.counted_quantity<>l.expected_quantity) variance_count" +
      " from stocktake s left join operation o on o.id=s.operation_id" +
      " left join staff_user cb on cb.id=s.created_by left join staff_user sb on sb.id=s.submitted_by left join staff_user db on db.id=s.decided_by";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public StocktakeRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public boolean hasOpen(String operationId) {
    Integer n = operationId == null
        ? db.queryForObject("select count(*) from stocktake where operation_id is null and status in ('COUNTING','SUBMITTED')", Integer.class)
        : db.queryForObject("select count(*) from stocktake where operation_id=? and status in ('COUNTING','SUBMITTED')", Integer.class, operationId);
    return n != null && n > 0;
  }

  public void insert(String id, String operationId, String actorId, String note) {
    db.update("insert into stocktake(id,operation_id,status,note,created_by,created_at) values(?,?,'COUNTING',?,?,UTC_TIMESTAMP())", id, operationId, note, actorId);
    search.changed(SearchType.STOCKTAKE, id);
  }

  /** Snapshots the expected quantity of every active product (and any inactive one still held). */
  public void snapshotLines(String stocktakeId, String operationId) {
    if (operationId == null)
      db.update("insert into stocktake_line(stocktake_id,product_id,expected_quantity) select ?, p.id, coalesce(c.quantity,0) from product p left join central_inventory c on c.product_id=p.id where p.kind<>'PARENT' and p.track_stock=true and (p.active=true or coalesce(c.quantity,0)>0)", stocktakeId);
    else
      db.update("insert into stocktake_line(stocktake_id,product_id,expected_quantity) select ?, p.id, coalesce(b.quantity,0) from product p left join inventory_balance b on b.product_id=p.id and b.operation_id=? where p.kind<>'PARENT' and p.track_stock=true and (p.active=true or coalesce(b.quantity,0)>0)", stocktakeId, operationId);
  }

  public Optional<Map<String, Object>> lock(String id) {
    return db.queryForList("select * from stocktake where id=? for update", id).stream().findFirst();
  }

  public Map<String, Object> find(String id) { return db.queryForMap(SELECT + " where s.id=?", id); }

  public List<Map<String, Object>> lines(String id) {
    return db.queryForList("select l.product_id, p.name product_name, p.sku, p.category, p.image_url, l.expected_quantity, l.counted_quantity," +
        " case when l.counted_quantity is null then null else l.counted_quantity-l.expected_quantity end difference" +
        " from stocktake_line l join product p on p.id=l.product_id where l.stocktake_id=? order by p.category, p.name", id);
  }

  public int setCount(String id, String productId, Integer counted) {
    return db.update("update stocktake_line set counted_quantity=? where stocktake_id=? and product_id=?", counted, id, productId);
  }

  public int uncountedLines(String id) {
    Integer n = db.queryForObject("select count(*) from stocktake_line where stocktake_id=? and counted_quantity is null", Integer.class, id);
    return n == null ? 0 : n;
  }

  public void submit(String id, String actorId) {
    db.update("update stocktake set status='SUBMITTED',submitted_by=?,submitted_at=UTC_TIMESTAMP() where id=?", actorId, id);
    search.changed(SearchType.STOCKTAKE, id);
  }

  public void decide(String id, String status, String actorId, String note) {
    db.update("update stocktake set status=?,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.STOCKTAKE, id);
  }

  /** Sends a rejected count back for recounting. */
  public void reopen(String id, String actorId, String note) {
    db.update("update stocktake set status='COUNTING',submitted_by=null,submitted_at=null,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", actorId, note, id);
    search.changed(SearchType.STOCKTAKE, id);
  }

  public PageResponse<Map<String, Object>> page(String status, String operationId, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("s.status=?", status);
    if ("CENTRAL".equals(operationId)) f.and("s.operation_id is null"); else f.and("s.operation_id=?", operationId);
    return f.page(db, "order by s.created_at desc, s.id desc", req);
  }

  public List<Map<String, Object>> search(String status) {
    SqlFilter f = new SqlFilter(SELECT).and("s.status=?", status);
    return db.queryForList(f.sql("order by s.created_at desc limit 200"), f.args());
  }
}
