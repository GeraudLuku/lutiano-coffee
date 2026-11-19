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

/** Stock transfers from central stock to a sales operation (table stock_allocation). */
@Repository
public class TransferRepository {
  static final String SELECT =
      "select a.*,p.name product_name,p.sku,p.image_url,o.name operation_name,o.code operation_code," +
      " cb.full_name created_by_name, cb.role created_by_role, ab.full_name approved_by_name, db.full_name dispatched_by_name," +
      " rb.full_name confirmed_by_name, xb.full_name closed_by_name, coalesce(c.quantity,0) central_quantity" +
      " from stock_allocation a join product p on p.id=a.product_id join operation o on o.id=a.operation_id" +
      " left join central_inventory c on c.product_id=a.product_id" +
      " left join staff_user cb on cb.id=a.created_by left join staff_user ab on ab.id=a.approved_by" +
      " left join staff_user db on db.id=a.dispatched_by left join staff_user rb on rb.id=a.confirmed_by left join staff_user xb on xb.id=a.closed_by";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public TransferRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public void insertRequest(String id, String operationId, String productId, int quantity, String actorId, String note) { insertRequest(id, null, operationId, productId, quantity, actorId, note); }

  /** One product of a stock request ({@code requestId}), or a single-product transfer when it is null. */
  public void insertRequest(String id, String requestId, String operationId, String productId, int quantity, String actorId, String note) {
    db.update("insert into stock_allocation(id,request_id,operation_id,product_id,sent_quantity,status,created_by,request_note,created_at) values(?,?,?,?,?,'PENDING_APPROVAL',?,?,UTC_TIMESTAMP())",
        id, requestId, operationId, productId, quantity, actorId, note);
    search.changed(SearchType.TRANSFER, id);
  }

  /** The products of a stock request, by name. */
  public List<Map<String, Object>> ofRequest(String requestId) {
    return db.queryForList(SELECT + " where a.request_id=? order by p.name, a.id", requestId);
  }

  /** Locks the products of a stock request for the rest of the transaction. */
  public List<Map<String, Object>> lockOfRequest(String requestId) {
    return db.queryForList("select * from stock_allocation where request_id=? order by id for update", requestId);
  }

  public int inTransitOfRequest(String requestId) {
    Integer n = db.queryForObject("select count(*) from stock_allocation where request_id=? and status='IN_TRANSIT'", Integer.class, requestId);
    return n == null ? 0 : n;
  }

  /** Reads and row-locks a transfer for the rest of the transaction. */
  public Optional<Map<String, Object>> lock(String id) {
    return db.queryForList("select * from stock_allocation where id=? for update", id).stream().findFirst();
  }

  public Map<String, Object> find(String id) { return db.queryForMap(SELECT + " where a.id=?", id); }

  public void approve(String id, String actorId, String note) {
    db.update("update stock_allocation set status='APPROVED',approved_by=?,approved_at=UTC_TIMESTAMP(),decision_note=? where id=?", actorId, note, id);
    search.changed(SearchType.TRANSFER, id);
  }

  public void dispatch(String id, String actorId) {
    db.update("update stock_allocation set status='IN_TRANSIT',dispatched_by=?,dispatched_at=UTC_TIMESTAMP() where id=?", actorId, id);
    search.changed(SearchType.TRANSFER, id);
  }

  public void close(String id, String status, String actorId, String note) {
    db.update("update stock_allocation set status=?,closed_by=?,closed_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.TRANSFER, id);
  }

  public void markReceived(String id, int quantity, String actorId) {
    db.update("update stock_allocation set received_quantity=?,status='RECEIVED',confirmed_by=?,received_at=UTC_TIMESTAMP() where id=?", quantity, actorId, id);
    search.changed(SearchType.TRANSFER, id);
  }

  public PageResponse<Map<String, Object>> page(String status, String operationId, String q, PageRequest req) {
    return new SqlFilter(SELECT).and("a.request_id is null").and("a.status=?", status).and("a.operation_id=?", operationId)
        .search(q, "p.name", "p.sku", "o.name", "o.code")
        .page(db, "order by a.created_at desc, a.id desc", req);
  }

  public PageResponse<Map<String, Object>> forSellerPage(String sellerId, String operationId, String status, PageRequest req) {
    return new SqlFilter(SELECT + " join seller_operation so on so.operation_id=a.operation_id")
        .and("so.seller_id=?", sellerId).and("a.operation_id=?", operationId).and("a.status=?", status).page(db, "order by a.created_at desc, a.id desc", req);
  }

  public List<Map<String, Object>> search(String status, String operationId) {
    SqlFilter f = new SqlFilter(SELECT).and("a.request_id is null").and("a.status=?", status).and("a.operation_id=?", operationId);
    return db.queryForList(f.sql("order by a.created_at desc limit 300"), f.args());
  }

  /** Transfers to the seller's operations in the given status (default: in transit). */
  public List<Map<String, Object>> forSeller(String sellerId, String operationId, String status) {
    SqlFilter f = new SqlFilter(SELECT + " join seller_operation so on so.operation_id=a.operation_id")
        .and("so.seller_id=?", sellerId).and("a.operation_id=?", operationId).and("a.status=?", status);
    return db.queryForList(f.sql("order by a.created_at desc limit 200"), f.args());
  }
}
