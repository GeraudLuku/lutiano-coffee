package com.lutiano.api.repository.cash;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.UUID;
import com.lutiano.api.definitions.enums.SearchType;

@Repository
public class CashRepository {
  /** Shift with ledger totals per movement type and outstanding remittances. */
  private static final String SHIFT_SELECT =
      "select c.id,c.seller_id,c.opening_float,c.expected_cash,c.counted_cash,(c.counted_cash-c.expected_cash) variance,c.variance_reason,c.status," +
      " c.opened_at,c.closed_at,c.review_status,c.reviewed_at,c.review_note,u.full_name seller_name,rv.full_name reviewed_by_name," +
      " (select count(*) from sale s where s.shift_id=c.id) sale_count," +
      " coalesce((select sum(amount_xaf) from cash_ledger_entry e where e.shift_id=c.id and e.entry_type='CASH_SALE'),0) cash_sales_xaf," +
      " coalesce((select -sum(amount_xaf) from cash_ledger_entry e where e.shift_id=c.id and e.entry_type='REFUND_PAID'),0) refunds_paid_xaf," +
      " coalesce((select -sum(amount_xaf) from cash_ledger_entry e where e.shift_id=c.id and e.entry_type='OUTFLOW'),0) outflows_xaf," +
      " coalesce((select -sum(amount_xaf) from cash_ledger_entry e where e.shift_id=c.id and e.entry_type='REMITTANCE'),0) remitted_xaf," +
      " (select count(*) from cash_remittance r where r.shift_id=c.id and r.status='OUTSTANDING') outstanding_remittances," +
      " (select count(*) from cash_outflow o where o.shift_id=c.id and o.status='REQUESTED') pending_outflows," +
      " (select count(*) from cash_count k where k.shift_id=c.id) count_count" +
      " from cash_shift c join staff_user u on u.id=c.seller_id left join staff_user rv on rv.id=c.reviewed_by";
  private static final String OUTFLOW_SELECT =
      "select o.*,u.full_name seller_name,d.full_name decided_by_name from cash_outflow o join staff_user u on u.id=o.seller_id left join staff_user d on d.id=o.decided_by";
  private static final String REMITTANCE_SELECT =
      "select r.*,u.full_name seller_name,rc.full_name recipient_name,a.full_name acknowledged_by_name from cash_remittance r join staff_user u on u.id=r.seller_id" +
      " left join staff_user rc on rc.id=r.recipient_id left join staff_user a on a.id=r.acknowledged_by";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public CashRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- shifts -----

  /** review: a review status, or "OPEN_REVIEW" for shifts waiting on a manager or a recount. */
  public PageResponse<Map<String, Object>> shifts(String status, String reviewStatus, String sellerId, String q, PageRequest req) {
    SqlFilter f = new SqlFilter(SHIFT_SELECT).and("c.status=?", status).and("c.seller_id=?", sellerId)
        .search(q, "u.full_name");
    if ("OPEN_REVIEW".equals(reviewStatus)) f.and("c.review_status in ('PENDING_REVIEW','RECOUNT_REQUESTED')");
    else f.and("c.review_status=?", reviewStatus);
    return f.page(db, "order by c.opened_at desc, c.id desc", req);
  }

  public Optional<Map<String, Object>> shift(String id) { return db.queryForList(SHIFT_SELECT + " where c.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> lockShift(String id) {
    return db.queryForList("select * from cash_shift where id=? for update", id).stream().findFirst();
  }

  public Optional<Map<String, Object>> openShiftOf(String sellerId) {
    return db.queryForList("select * from cash_shift where seller_id=? and status='OPEN' for update", sellerId).stream().findFirst();
  }

  public void review(String id, String status, String actorId, String note) {
    db.update("update cash_shift set review_status=?,reviewed_by=?,reviewed_at=UTC_TIMESTAMP(),review_note=? where id=?", status, actorId, note, id);
  }

  /** A recount replaces the shift's current counted cash; the earlier counts stay in cash_count. */
  public void recount(String id, long counted, String reason, String reviewStatus) {
    db.update("update cash_shift set counted_cash=?,variance_reason=?,review_status=? where id=?", counted, reason, reviewStatus, id);
  }

  /** Sets the running balance after a ledger posting; refunds also add to the shift's refunds-paid total. */
  public void setExpectedCash(String shiftId, long balance, long refundPaid) {
    db.update("update cash_shift set expected_cash=?,refunds_paid_xaf=refunds_paid_xaf+? where id=?", balance, refundPaid, shiftId);
  }

  /** Appends an entry with the next sequence number of the shift (callers hold the shift lock). */
  public void insertLedgerEntry(String id, String shiftId, String type, long amount, long balance, String sourceType, String sourceId, String actorId, String note) {
    db.update("insert into cash_ledger_entry(id,shift_id,seq,entry_type,amount_xaf,balance_after_xaf,source_type,source_id,actor_id,note,created_at)" +
        " select ?,?,coalesce(max(seq),0)+1,?,?,?,?,?,?,?,UTC_TIMESTAMP(6) from cash_ledger_entry where shift_id=?",
        id, shiftId, type, amount, balance, sourceType, sourceId, actorId, note, shiftId);
  }


  public void insertCount(String shiftId, String kind, long expected, long counted, String reason, String actorId) {
    db.update("insert into cash_count(id,shift_id,kind,expected_xaf,counted_xaf,variance_xaf,reason,counted_by,created_at) values(?,?,?,?,?,?,?,?,UTC_TIMESTAMP(6))",
        UUID.randomUUID().toString(), shiftId, kind, expected, counted, counted - expected, reason, actorId);
  }

  public List<Map<String, Object>> counts(String shiftId) {
    return db.queryForList("select k.*,u.full_name counted_by_name from cash_count k left join staff_user u on u.id=k.counted_by where k.shift_id=? order by k.created_at", shiftId);
  }

  // ----- outflows -----

  public void insertOutflow(String id, String reference, String shiftId, String sellerId, long amount, String category, String description) {
    db.update("insert into cash_outflow(id,reference,shift_id,seller_id,amount_xaf,category,description,status,requested_at) values(?,?,?,?,?,?,?,'REQUESTED',UTC_TIMESTAMP())",
        id, reference, shiftId, sellerId, amount, category, description);
    search.changed(SearchType.CASH_OUTFLOW, id);
  }

  public Optional<Map<String, Object>> lockOutflow(String id) {
    return db.queryForList("select * from cash_outflow where id=? for update", id).stream().findFirst();
  }

  public void decideOutflow(String id, String status, String actorId, String note) {
    db.update("update cash_outflow set status=?,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.CASH_OUTFLOW, id);
  }

  public Optional<Map<String, Object>> outflow(String id) { return db.queryForList(OUTFLOW_SELECT + " where o.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> outflows(String status, String shiftId, String sellerId) {
    SqlFilter f = new SqlFilter(OUTFLOW_SELECT).and("o.status=?", status).and("o.shift_id=?", shiftId).and("o.seller_id=?", sellerId);
    return db.queryForList(f.sql("order by o.requested_at desc limit 500"), f.args());
  }

  /** Waiting requests first, then newest. */
  public PageResponse<Map<String, Object>> outflowPage(String status, String sellerId, PageRequest req) {
    return new SqlFilter(OUTFLOW_SELECT).and("o.status=?", status).and("o.seller_id=?", sellerId)
        .page(db, "order by (o.status='REQUESTED') desc, o.requested_at desc, o.id desc", req);
  }

  /** Closing a shift withdraws its outflow requests that were never decided. */
  public int cancelOpenOutflows(String shiftId) {
    search.changedAll(SearchType.CASH_OUTFLOW, db.queryForList("select id from cash_outflow where shift_id=? and status='REQUESTED'", String.class, shiftId));
    return db.update("update cash_outflow set status='CANCELLED',decision_note='Shift closed before approval',decided_at=UTC_TIMESTAMP() where shift_id=? and status='REQUESTED'", shiftId);
  }

  // ----- remittances -----

  public void insertRemittance(String id, String reference, String shiftId, String sellerId, long amount, String method, String recipientId, String bankReference, String note) {
    db.update("insert into cash_remittance(id,reference,shift_id,seller_id,amount_xaf,method,recipient_id,bank_reference,note,status,recorded_at)" +
        " values(?,?,?,?,?,?,?,?,?,'OUTSTANDING',UTC_TIMESTAMP())", id, reference, shiftId, sellerId, amount, method, recipientId, bankReference, note);
    search.changed(SearchType.CASH_REMITTANCE, id);
  }

  public Optional<Map<String, Object>> lockRemittance(String id) {
    return db.queryForList("select * from cash_remittance where id=? for update", id).stream().findFirst();
  }

  public void acknowledge(String id, String status, String actorId, long received, String note) {
    db.update("update cash_remittance set status=?,acknowledged_by=?,acknowledged_at=UTC_TIMESTAMP(),received_xaf=?,acknowledgement_note=? where id=?",
        status, actorId, received, note, id);
    search.changed(SearchType.CASH_REMITTANCE, id);
  }

  public Optional<Map<String, Object>> remittance(String id) { return db.queryForList(REMITTANCE_SELECT + " where r.id=?", id).stream().findFirst(); }

  /** Outstanding first, then newest. */
  public PageResponse<Map<String, Object>> remittancePage(String status, String sellerId, String recipientId, PageRequest req) {
    return new SqlFilter(REMITTANCE_SELECT).and("r.status=?", status).and("r.seller_id=?", sellerId).and("r.recipient_id=?", recipientId)
        .page(db, "order by (r.status='OUTSTANDING') desc, r.recorded_at desc, r.id desc", req);
  }

  public PageResponse<Map<String, Object>> ledgerPage(String shiftId, PageRequest req) {
    return new SqlFilter("select e.*,u.full_name actor_name, coalesce(s.reference,rf.reference,o.reference,rm.reference) source_reference" +
        " from cash_ledger_entry e left join staff_user u on u.id=e.actor_id" +
        " left join sale s on e.source_type='SALE' and s.id=e.source_id left join refund rf on e.source_type='REFUND' and rf.id=e.source_id" +
        " left join cash_outflow o on e.source_type='OUTFLOW' and o.id=e.source_id left join cash_remittance rm on e.source_type='REMITTANCE' and rm.id=e.source_id")
        .and("e.shift_id=?", shiftId).page(db, "order by e.seq desc", req);
  }

  public List<Map<String, Object>> remittances(String status, String shiftId, String sellerId, String recipientId) {
    SqlFilter f = new SqlFilter(REMITTANCE_SELECT).and("r.status=?", status).and("r.shift_id=?", shiftId).and("r.seller_id=?", sellerId).and("r.recipient_id=?", recipientId);
    return db.queryForList(f.sql("order by r.recorded_at desc limit 500"), f.args());
  }

  /** Active staff (not sellers) who can receive cash handovers. */
  public List<Map<String, Object>> recipients() {
    return db.queryForList("select id,full_name,role from staff_user where active=TRUE and role in ('ADMIN','INVENTORY_MANAGER') order by full_name");
  }

  public boolean isRecipient(String id) {
    Integer n = db.queryForObject("select count(*) from staff_user where id=? and active=TRUE and role in ('ADMIN','INVENTORY_MANAGER')", Integer.class, id);
    return n != null && n > 0;
  }

  // ----- summary -----

  /** What needs a seller's attention across all of their shifts: undecided outflows, unconfirmed or disputed remittances, recounts asked for. */
  public Map<String, Object> sellerOverview(String sellerId) {
    return db.queryForMap("select" +
        " (select count(*) from cash_outflow where seller_id=? and status='REQUESTED') pending_outflows," +
        " (select count(*) from cash_remittance where seller_id=? and status='OUTSTANDING') outstanding_remittances," +
        " (select coalesce(sum(amount_xaf),0) from cash_remittance where seller_id=? and status='OUTSTANDING') outstanding_remittances_xaf," +
        " (select count(*) from cash_remittance where seller_id=? and status='DISPUTED') disputed_remittances," +
        " (select count(*) from cash_shift where seller_id=? and review_status='RECOUNT_REQUESTED') recounts_requested",
        sellerId, sellerId, sellerId, sellerId, sellerId);
  }

  public Map<String, Object> summary() {
    return db.queryForMap("select" +
        " (select count(*) from cash_shift where status='OPEN') open_shifts," +
        " (select coalesce(sum(expected_cash),0) from cash_shift where status='OPEN') cash_in_open_shifts_xaf," +
        " (select count(*) from cash_shift where review_status in ('PENDING_REVIEW','RECOUNT_REQUESTED')) shifts_to_review," +
        " (select count(*) from cash_outflow where status='REQUESTED') pending_outflows," +
        " (select count(*) from cash_remittance where status='OUTSTANDING') outstanding_remittances," +
        " (select coalesce(sum(amount_xaf),0) from cash_remittance where status='OUTSTANDING') outstanding_remittances_xaf," +
        " (select count(*) from cash_remittance where status='DISPUTED') disputed_remittances");
  }
}
