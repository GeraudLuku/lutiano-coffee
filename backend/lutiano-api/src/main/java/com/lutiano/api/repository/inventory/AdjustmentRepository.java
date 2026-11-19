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
public class AdjustmentRepository {
  private static final String SELECT =
      "select j.*, p.name product_name, p.sku, coalesce(o.name,'Central stock') location_name, o.code location_code," +
      " rb.full_name requested_by_name, rb.role requested_by_role, db.full_name decided_by_name" +
      " from stock_adjustment j join product p on p.id=j.product_id left join operation o on o.id=j.operation_id" +
      " left join staff_user rb on rb.id=j.requested_by left join staff_user db on db.id=j.decided_by";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public AdjustmentRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public void insert(String id, String operationId, String productId, int delta, String reason, String note, String actorId) {
    db.update("insert into stock_adjustment(id,operation_id,product_id,quantity_delta,reason,note,status,requested_by,created_at) values(?,?,?,?,?,?,'PENDING_APPROVAL',?,UTC_TIMESTAMP())",
        id, operationId, productId, delta, reason, note, actorId);
    search.changed(SearchType.ADJUSTMENT, id);
  }

  public Optional<Map<String, Object>> lock(String id) {
    return db.queryForList("select * from stock_adjustment where id=? for update", id).stream().findFirst();
  }

  public Map<String, Object> find(String id) { return db.queryForMap(SELECT + " where j.id=?", id); }

  public void decide(String id, String status, String actorId, String note) {
    db.update("update stock_adjustment set status=?,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.ADJUSTMENT, id);
  }

  /** @param location null = all; "CENTRAL" = central only; otherwise an operation id. */
  public PageResponse<Map<String, Object>> page(String status, String location, String requestedBy, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("j.status=?", status).and("j.requested_by=?", requestedBy);
    if ("CENTRAL".equals(location)) f.and("j.operation_id is null");
    else f.and("j.operation_id=?", location);
    return f.page(db, "order by j.created_at desc, j.id desc", req);
  }

  public List<Map<String, Object>> search(String status, String location, String requestedBy) {
    SqlFilter f = new SqlFilter(SELECT).and("j.status=?", status).and("j.requested_by=?", requestedBy);
    if ("CENTRAL".equals(location)) f.and("j.operation_id is null");
    else f.and("j.operation_id=?", location);
    return db.queryForList(f.sql("order by j.created_at desc limit 300"), f.args());
  }
}
