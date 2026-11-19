package com.lutiano.api.repository.inventory;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Stock requests (table stock_request); the products are stock_allocation rows that point back by request_id. */
@Repository
public class StockRequestRepository {
  static final String SELECT =
      "select r.*, o.name operation_name, o.code operation_code, rb.full_name requested_by_name, ab.full_name approved_by_name, xb.full_name closed_by_name," +
      " (select count(*) from stock_allocation a where a.request_id=r.id) item_count," +
      " (select coalesce(sum(a.sent_quantity),0) from stock_allocation a where a.request_id=r.id) total_quantity" +
      " from stock_request r join operation o on o.id=r.operation_id join staff_user rb on rb.id=r.requested_by" +
      " left join staff_user ab on ab.id=r.approved_by left join staff_user xb on xb.id=r.closed_by";
  private final JdbcTemplate db;

  public StockRequestRepository(JdbcTemplate db) { this.db = db; }

  public void insert(String id, String reference, String operationId, String requestedBy, String idempotencyKey, LocalDate neededBy, String reason) {
    db.update("insert into stock_request(id,reference,operation_id,requested_by,idempotency_key,needed_by,reason,status,created_at) values(?,?,?,?,?,?,?,'PENDING_APPROVAL',UTC_TIMESTAMP())",
        id, reference, operationId, requestedBy, idempotencyKey, neededBy, reason);
  }

  /** Reads and row-locks a request for the rest of the transaction. */
  public Optional<Map<String, Object>> lock(String id) {
    return db.queryForList("select * from stock_request where id=? for update", id).stream().findFirst();
  }

  public Optional<Map<String, Object>> find(String id) {
    return db.queryForList(SELECT + " where r.id=?", id).stream().findFirst();
  }

  public Optional<String> idByKey(String requestedBy, String key) {
    if (key == null) return Optional.empty();
    return db.queryForList("select id from stock_request where requested_by=? and idempotency_key=?", String.class, requestedBy, key).stream().findFirst();
  }

  public void approve(String id, String actorId, String note) {
    db.update("update stock_request set status='APPROVED',approved_by=?,approved_at=UTC_TIMESTAMP(),decision_note=? where id=?", actorId, note, id);
  }

  public void dispatch(String id) {
    db.update("update stock_request set status='IN_TRANSIT',dispatched_at=UTC_TIMESTAMP() where id=?", id);
  }

  public void close(String id, String status, String actorId, String note) {
    db.update("update stock_request set status=?,closed_by=?,closed_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
  }

  public void complete(String id) {
    db.update("update stock_request set status='RECEIVED',completed_at=UTC_TIMESTAMP() where id=?", id);
  }

  /** The request with its products attached as {@code items}. */
  public Map<String, Object> withItems(Map<String, Object> request, List<Map<String, Object>> items) {
    Map<String, Object> out = new LinkedHashMap<>(request);
    out.put("items", items);
    return out;
  }

  /** Attaches {@code items} to every request of a page with one query. */
  public List<Map<String, Object>> withItems(List<Map<String, Object>> requests) {
    if (requests.isEmpty()) return requests;
    List<Object> ids = requests.stream().map(r -> r.get("id")).toList();
    List<Map<String, Object>> lines = db.queryForList(TransferRepository.SELECT + " where a.request_id in (" + String.join(",", Collections.nCopies(ids.size(), "?")) + ") order by p.name, a.id", ids.toArray());
    Map<Object, List<Map<String, Object>>> byRequest = new LinkedHashMap<>();
    for (Map<String, Object> line : lines) byRequest.computeIfAbsent(line.get("request_id"), k -> new ArrayList<>()).add(line);
    return requests.stream().map(r -> withItems(r, byRequest.getOrDefault(r.get("id"), List.of()))).toList();
  }

  /** The staff view: every request, newest first, with its products. */
  public PageResponse<Map<String, Object>> page(String status, String operationId, String reference, PageRequest req) {
    PageResponse<Map<String, Object>> page = new SqlFilter(SELECT).and("r.status=?", status).and("r.operation_id=?", operationId)
        .and("r.reference like ?", prefix(reference)).page(db, "order by r.created_at desc, r.id desc", req);
    return new PageResponse<>(withItems(page.items()), page.page(), page.size(), page.total(), page.pages(), page.summary(), page.truncated());
  }

  /** A seller's own requests at one operation, newest first, with counts per status over all of them. */
  public PageResponse<Map<String, Object>> forSeller(String sellerId, String operationId, String status, String reference, PageRequest req) {
    PageResponse<Map<String, Object>> page = new SqlFilter(SELECT).and("r.requested_by=?", sellerId).and("r.operation_id=?", operationId)
        .and("r.status=?", status).and("r.reference like ?", prefix(reference)).page(db, "order by r.created_at desc, r.id desc", req);
    Map<String, Object> counts = db.queryForMap(
        "select count(*) total, coalesce(sum(status='PENDING_APPROVAL'),0) pending, coalesce(sum(status='APPROVED'),0) approved, coalesce(sum(status='IN_TRANSIT'),0) in_fulfillment," +
        " coalesce(sum(status='RECEIVED'),0) completed, coalesce(sum(status='REJECTED'),0) rejected from stock_request where requested_by=? and operation_id=?", sellerId, operationId);
    return new PageResponse<>(withItems(page.items()), page.page(), page.size(), page.total(), page.pages(), counts, false);
  }

  /** Every request in one status (the manager approvals queue), with its products. */
  public List<Map<String, Object>> inStatus(String status) {
    SqlFilter f = new SqlFilter(SELECT).and("r.status=?", status);
    return withItems(db.queryForList(f.sql("order by r.created_at desc, r.id limit 300"), f.args()));
  }

  private static String prefix(String reference) {
    if (reference == null || reference.isBlank()) return null;
    return reference.trim().toUpperCase().replace("%", "").replace("_", "") + "%";
  }
}
