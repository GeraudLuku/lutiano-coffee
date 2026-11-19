package com.lutiano.api.repository.refund;

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
import java.util.UUID;
import com.lutiano.api.utils.Rows;
import java.util.HashMap;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class RefundRepository {
  private static final String SELECT =
      "select r.*, s.reference sale_reference, s.total_xaf sale_total_xaf, s.created_at sale_created_at, o.name operation_name, o.code operation_code," +
      " su.full_name seller_name, rq.full_name requested_by_name, dc.full_name decided_by_name, pb.full_name paid_by_name," +
      " sh.seller_id shift_seller_id, shu.full_name shift_seller_name" +
      " from refund r join sale s on s.id=r.sale_id join operation o on o.id=r.operation_id join staff_user su on su.id=r.seller_id" +
      " join staff_user rq on rq.id=r.requested_by left join staff_user dc on dc.id=r.decided_by left join staff_user pb on pb.id=r.paid_by" +
      " left join cash_shift sh on sh.id=r.shift_id left join staff_user shu on shu.id=sh.seller_id";
  /** Refunds that still count against the sale total (everything except rejected and cancelled). */
  static final String OPEN_OR_PAID = "('REQUESTED','APPROVED','PAID')";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public RefundRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  /** Locks the sale row so two refund requests for the same sale are serialised. */
  public Optional<Map<String, Object>> lockSale(String saleId) {
    db.queryForList("select id from sale where id=? for update", saleId);
    return findSale(saleId);
  }

  /** The sale with its loyalty member (if the customer showed their QR), so a refund uses the member's details. */
  public Optional<Map<String, Object>> findSale(String saleId) {
    return db.queryForList("select s.id,s.reference,s.operation_id,s.seller_id,s.total_xaf,s.created_at,s.customer_id,c.full_name customer_name,c.phone customer_phone,c.code customer_code" +
        " from sale s left join customer c on c.id=s.customer_id where s.id=?", saleId).stream().findFirst();
  }

  public Optional<String> saleIdByReference(String reference) {
    return db.queryForList("select id from sale where reference=?", String.class, reference).stream().findFirst();
  }

  /** Sum of refunds on a sale that are requested, approved or paid. */
  public long committedForSale(String saleId) {
    return db.queryForObject("select coalesce(sum(amount_xaf),0) from refund where sale_id=? and status in " + OPEN_OR_PAID, Long.class, saleId);
  }

  public long paidForSale(String saleId) {
    return db.queryForObject("select coalesce(sum(amount_xaf),0) from refund where sale_id=? and status='PAID'", Long.class, saleId);
  }

  public void insert(String id, String reference, Map<String, Object> sale, long amount, String type, String reason, String note,
                     String customerName, String customerPhone, String actorId) {
    db.update("insert into refund(id,reference,sale_id,operation_id,seller_id,amount_xaf,refund_type,reason,note,customer_name,customer_phone,status,requested_by,requested_at)" +
            " values(?,?,?,?,?,?,?,?,?,?,?,'REQUESTED',?,UTC_TIMESTAMP())",
        id, reference, sale.get("id"), sale.get("operation_id"), sale.get("seller_id"), amount, type, reason, note, customerName, customerPhone, actorId);
    search.changed(SearchType.REFUND, id);
  }

  public Optional<Map<String, Object>> lock(String id) {
    return db.queryForList("select * from refund where id=? for update", id).stream().findFirst();
  }

  public void decide(String id, String status, String actorId, String note) {
    db.update("update refund set status=?,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.REFUND, id);
  }

  public void cancel(String id) {
    db.update("update refund set status='CANCELLED' where id=?", id);
    search.changed(SearchType.REFUND, id);
  }

  public void markPaid(String id, String fundSource, String shiftId, String advanceReference, Long advanceAmount, Long advanceReturned,
                       String recipientName, String actorId, String note) {
    db.update("update refund set status='PAID',fund_source=?,shift_id=?,advance_reference=?,advance_amount_xaf=?,advance_returned_xaf=?," +
            "recipient_name=?,recipient_acknowledged=TRUE,paid_by=?,paid_at=UTC_TIMESTAMP(),payment_note=? where id=?",
        fundSource, shiftId, advanceReference, advanceAmount, advanceReturned, recipientName, actorId, note, id);
    search.changed(SearchType.REFUND, id);
  }

  public void insertEvent(String refundId, String type, String from, String to, String actorId, String details) {
    db.update("insert into refund_event(id,refund_id,event_type,from_status,to_status,actor_id,details,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP(6))",
        UUID.randomUUID().toString(), refundId, type, from, to, actorId, Rows.clip(details, 500));
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where r.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> events(String refundId) {
    return db.queryForList("select e.*, u.full_name actor_name from refund_event e left join staff_user u on u.id=e.actor_id" +
        " where e.refund_id=? order by e.created_at", refundId);
  }

  public List<Map<String, Object>> forSale(String saleId) { return db.queryForList(SELECT + " where r.sale_id=? order by r.requested_at", saleId); }

  /** One page of refunds; the summary counts each status (ignoring the status filter) and totals what was paid. */
  public PageResponse<Map<String, Object>> search(Timestamp from, Timestamp to, String status, String operationId, String sellerId, Candidates text, PageRequest req) {
    SqlFilter summary = filter(from, to, operationId, sellerId, text);
    return filter(from, to, operationId, sellerId, text).and("r.status=?", status).page(db, "order by r.requested_at desc, r.reference desc", req)
        .withSummary(summary.aggregate(db, "count(*) refunds, coalesce(sum(case when status='PAID' then amount_xaf end),0) refunded_xaf, sum(status='PAID') paid," +
            " sum(status in ('REQUESTED','APPROVED')) pending, sum(status='REJECTED') rejected"));
  }

  private static SqlFilter filter(Timestamp from, Timestamp to, String operationId, String sellerId, Candidates text) {
    return new SqlFilter(SELECT).and("r.requested_at>=?", from).and("r.requested_at<?", to).and("r.operation_id=?", operationId)
        .and("r.seller_id=?", sellerId).searchIds(text, "r.id", "r.customer_phone");
  }

  public long countByStatus(String status) {
    return db.queryForObject("select count(*) from refund where status=?", Long.class, status);
  }

  // ----- evidence -----

  public void insertEvidence(String id, String refundId, String key, String name, String contentType, long size, String actorId) {
    db.update("insert into refund_evidence(id,refund_id,object_key,file_name,content_type,size_bytes,uploaded_by,uploaded_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, refundId, key, name, contentType, size, actorId);
  }

  public List<Map<String, Object>> evidence(String refundId) {
    return db.queryForList("select e.id,e.file_name,e.content_type,e.size_bytes,e.uploaded_at,u.full_name uploaded_by_name from refund_evidence e" +
        " left join staff_user u on u.id=e.uploaded_by where e.refund_id=? order by e.uploaded_at", refundId);
  }

  public Optional<Map<String, Object>> evidenceFile(String refundId, String evidenceId) {
    return db.queryForList("select * from refund_evidence where id=? and refund_id=?", evidenceId, refundId).stream().findFirst();
  }

  // ----- returned stock -----

  /** Quantity sold per product on the sale (tracked and untracked). */
  public List<Map<String, Object>> soldProducts(String saleId) {
    return db.queryForList("select l.product_id, max(l.product_name) product_name, sum(l.quantity) quantity, max(p.track_stock) track_stock" +
        " from sale_line l left join product p on p.id=l.product_id where l.sale_id=? group by l.product_id order by product_name", saleId);
  }

  /** Quantities already put back through return adjustments for this sale's refunds (pending or applied). */
  public Map<String, Integer> returnedQuantities(String saleId) {
    Map<String, Integer> out = new HashMap<>();
    for (Map<String, Object> r : db.queryForList("select j.product_id, sum(j.quantity_delta) qty from stock_adjustment j join refund r on r.id=j.refund_id" +
        " where r.sale_id=? and j.status in ('PENDING_APPROVAL','APPLIED') group by j.product_id", saleId))
      out.put((String) r.get("product_id"), ((Number) r.get("qty")).intValue());
    return out;
  }

  public void linkAdjustment(String adjustmentId, String refundId) {
    db.update("update stock_adjustment set refund_id=? where id=?", refundId, adjustmentId);
  }

  public List<Map<String, Object>> stockReturns(String refundId) {
    return db.queryForList("select j.id,j.product_id,p.name product_name,j.quantity_delta,j.status,j.created_at,j.decided_at" +
        " from stock_adjustment j join product p on p.id=j.product_id where j.refund_id=? order by j.created_at", refundId);
  }
}
