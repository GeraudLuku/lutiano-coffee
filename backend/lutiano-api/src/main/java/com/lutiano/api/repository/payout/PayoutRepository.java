package com.lutiano.api.repository.payout;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.utils.Candidates;
import java.util.UUID;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.definitions.enums.SearchType;

/** Payouts, their history and evidence, per-seller balances and the approval threshold. */
@Repository
public class PayoutRepository {
  /** Statuses that count against a period's balance. "Paid" are the ones money has left for; "pending" are still to be paid. */
  private static final String PAID_STATUSES = "('PAID','REVERSAL_PENDING')", PENDING_STATUSES = "('PENDING_APPROVAL','APPROVED')";
  static final String COUNTED = "('PENDING_APPROVAL','APPROVED','PAID','REVERSAL_PENDING')";

  private static final String SELECT =
      "select p.*, u.full_name seller_name, u.seller_role, ep.starts_on period_starts_on, ep.ends_on period_ends_on," +
      " rq.full_name requested_by_name, dc.full_name decided_by_name, pb.full_name paid_by_name," +
      " rr.full_name reversal_requested_by_name, rv.full_name reversed_by_name" +
      " from payout p join staff_user u on u.id=p.seller_id join earning_period ep on ep.id=p.period_id join staff_user rq on rq.id=p.requested_by" +
      " left join staff_user dc on dc.id=p.decided_by left join staff_user pb on pb.id=p.paid_by" +
      " left join staff_user rr on rr.id=p.reversal_requested_by left join staff_user rv on rv.id=p.reversed_by";

  /** Every seller with what finalised earnings made payable, what was paid and what is waiting to be paid. */
  private static final String BALANCE =
      "select u.id seller_id,u.full_name,u.seller_role,u.active,u.image_url,u.phone," +
      " (select coalesce(sum(e.payable_xaf),0) from seller_earning e where e.seller_id=u.id) owed_xaf," +
      " (select coalesce(sum(x.amount_xaf),0) from payout x where x.seller_id=u.id and x.status in " + PAID_STATUSES + ") paid_xaf," +
      " (select coalesce(sum(x.amount_xaf),0) from payout x where x.seller_id=u.id and x.status in " + PENDING_STATUSES + ") pending_xaf," +
      " (select max(x.paid_at) from payout x where x.seller_id=u.id and x.status in " + PAID_STATUSES + ") last_paid_at" +
      " from staff_user u where u.role='SELLER'";
  private static final String OUTSTANDING = "(b.owed_xaf-b.paid_xaf-b.pending_xaf)";
  private static final String BALANCE_SELECT = "select b.*, " + OUTSTANDING + " outstanding_xaf from (" + BALANCE + ") b";

  /** A seller's finalised periods with their payable amount, what was paid and what is still to pay. */
  private static final String PERIOD_SELECT =
      "select t.*, (t.payable_xaf-t.paid_xaf-t.pending_xaf) remaining_xaf from (" +
      "select ep.id period_id, ep.starts_on, ep.ends_on, se.seller_id, se.payable_xaf," +
      " (select coalesce(sum(x.amount_xaf),0) from payout x where x.seller_id=se.seller_id and x.period_id=se.period_id and x.status in " + PAID_STATUSES + ") paid_xaf," +
      " (select coalesce(sum(x.amount_xaf),0) from payout x where x.seller_id=se.seller_id and x.period_id=se.period_id and x.status in " + PENDING_STATUSES + ") pending_xaf" +
      " from seller_earning se join earning_period ep on ep.id=se.period_id) t";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public PayoutRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- setting -----

  public long approvalThreshold() { return db.queryForObject("select approval_threshold_xaf from payout_setting where id=1", Long.class); }

  public void setApprovalThreshold(long xaf, String actorId) {
    db.update("update payout_setting set approval_threshold_xaf=?,updated_by=?,updated_at=UTC_TIMESTAMP() where id=1", xaf, actorId);
  }

  // ----- balances -----

  public PageResponse<Map<String, Object>> balances(String q, boolean owedOnly, PageRequest req) {
    return filter(q, owedOnly).page(db, "order by outstanding_xaf desc, b.full_name, b.seller_id", req)
        .withSummary(filter(q, false).aggregate(db, totals()));
  }

  private static SqlFilter filter(String q, boolean owedOnly) {
    SqlFilter f = new SqlFilter(BALANCE_SELECT).and("b.owed_xaf>0");
    if (owedOnly) f.and(OUTSTANDING + ">0");
    return f.search(q, "b.full_name", "b.phone");
  }

  private static String totals() {
    return "coalesce(sum(owed_xaf),0) owed_xaf, coalesce(sum(paid_xaf),0) paid_xaf, coalesce(sum(pending_xaf),0) pending_xaf," +
        " coalesce(sum(owed_xaf-paid_xaf-pending_xaf),0) outstanding_xaf, coalesce(sum(owed_xaf-paid_xaf-pending_xaf>0),0) sellers_with_balance";
  }

  /** Totals over every seller with finalised earnings. */
  public Map<String, Object> balanceTotals() {
    return new SqlFilter(BALANCE_SELECT).and("b.owed_xaf>0").aggregate(db, totals());
  }

  public Optional<Map<String, Object>> balance(String sellerId) {
    return db.queryForList(BALANCE_SELECT + " where b.seller_id=?", sellerId).stream().findFirst();
  }

  public PageResponse<Map<String, Object>> periods(String sellerId, boolean openOnly, PageRequest req) {
    SqlFilter f = new SqlFilter(PERIOD_SELECT).and("t.seller_id=?", sellerId);
    if (openOnly) f.and("(t.payable_xaf-t.paid_xaf-t.pending_xaf)>0");
    return f.page(db, "order by t.starts_on desc, t.period_id", req);
  }

  /** Locks a seller's statement for a period so two payouts cannot both take the same balance. */
  public Optional<Map<String, Object>> lockStatement(String sellerId, String periodId) {
    return db.queryForList("select * from seller_earning where seller_id=? and period_id=? for update", sellerId, periodId).stream().findFirst();
  }

  /** What is still to pay on a period: its payable amount minus payouts that count against it. */
  public long remaining(String sellerId, String periodId, long payable) {
    return payable - db.queryForObject("select coalesce(sum(amount_xaf),0) from payout where seller_id=? and period_id=? and status in " + COUNTED, Long.class, sellerId, periodId);
  }

  // ----- overview -----

  /** Payouts by state: count and amount waiting for approval, waiting to be paid and waiting for a reversal review, and paid since {@code monthStart}. */
  public Map<String, Object> workload(Timestamp monthStart) {
    return db.queryForMap("select" +
        " coalesce(sum(status='PENDING_APPROVAL'),0) approval_count, coalesce(sum(case when status='PENDING_APPROVAL' then amount_xaf end),0) approval_xaf," +
        " coalesce(sum(status='APPROVED'),0) to_pay_count, coalesce(sum(case when status='APPROVED' then amount_xaf end),0) to_pay_xaf," +
        " coalesce(sum(status='REVERSAL_PENDING'),0) reversal_count," +
        " coalesce(sum(case when status in " + PAID_STATUSES + " and paid_at>=? then amount_xaf end),0) paid_month_xaf" +
        " from payout", monthStart);
  }

  // ----- payouts -----

  public void insert(String id, String reference, String sellerId, String periodId, long amount, String method, Map<String, Object> destination,
                     String status, boolean requiresApproval, String note, String actorId) {
    db.update("insert into payout(id,reference,seller_id,period_id,amount_xaf,method,destination_id,destination_phone,destination_name,status,requires_approval,note,requested_by,requested_at)" +
            " values(?,?,?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, reference, sellerId, periodId, amount, method, destination == null ? null : destination.get("id"),
        destination == null ? null : destination.get("phone"), destination == null ? null : destination.get("account_name"),
        status, requiresApproval, note, actorId);
    search.changed(SearchType.PAYOUT, id);
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where p.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> lock(String id) { return db.queryForList("select * from payout where id=? for update", id).stream().findFirst(); }

  public void decide(String id, String status, String actorId, String note) {
    db.update("update payout set status=?,decided_by=?,decided_at=UTC_TIMESTAMP(),decision_note=? where id=?", status, actorId, note, id);
    search.changed(SearchType.PAYOUT, id);
  }

  public void setStatus(String id, String status) { db.update("update payout set status=? where id=?", status, id); search.changed(SearchType.PAYOUT, id); }

  public void markPaid(String id, String providerReference, String actorId, String note) {
    db.update("update payout set status='PAID',provider_reference=?,paid_by=?,paid_at=UTC_TIMESTAMP(),payment_note=? where id=?", providerReference, actorId, note, id);
    search.changed(SearchType.PAYOUT, id);
  }

  public void requestReversal(String id, String reason, String actorId) {
    db.update("update payout set status='REVERSAL_PENDING',reversal_reason=?,reversal_requested_by=?,reversal_requested_at=UTC_TIMESTAMP() where id=?", reason, actorId, id);
    search.changed(SearchType.PAYOUT, id);
  }

  /** The reversal is refused: the payout stays paid. */
  public void clearReversal(String id) {
    db.update("update payout set status='PAID',reversal_reason=null,reversal_requested_by=null,reversal_requested_at=null where id=?", id);
    search.changed(SearchType.PAYOUT, id);
  }

  /** Corrects the payout away. {@code reason}/{@code requestedBy} are set here when no review was needed. */
  public void reverse(String id, String reason, String requestedBy, String actorId) {
    db.update("update payout set status='REVERSED',reversal_reason=coalesce(reversal_reason,?),reversal_requested_by=coalesce(reversal_requested_by,?)," +
        "reversal_requested_at=coalesce(reversal_requested_at,UTC_TIMESTAMP()),reversed_by=?,reversed_at=UTC_TIMESTAMP() where id=?", reason, requestedBy, actorId, id);
    search.changed(SearchType.PAYOUT, id);
  }

  public PageResponse<Map<String, Object>> search(Timestamp from, Timestamp to, String status, String method, String sellerId, Candidates text, PageRequest req) {
    return filter(from, to, method, sellerId, text).and("p.status=?", status).page(db, "order by p.requested_at desc, p.reference desc", req)
        .withSummary(filter(from, to, method, sellerId, text).aggregate(db, "count(*) payouts, coalesce(sum(case when status in " + PAID_STATUSES + " then amount_xaf end),0) paid_xaf," +
            " coalesce(sum(status in " + PAID_STATUSES + "),0) paid, coalesce(sum(status in " + PENDING_STATUSES + "),0) pending, coalesce(sum(status='REJECTED'),0) rejected"));
  }

  private static SqlFilter filter(Timestamp from, Timestamp to, String method, String sellerId, Candidates text) {
    return new SqlFilter(SELECT).and("p.requested_at>=?", from).and("p.requested_at<?", to).and("p.method=?", method).and("p.seller_id=?", sellerId)
        .searchIds(text, "p.id", "p.provider_reference", "p.destination_phone");
  }

  // ----- history and evidence -----

  public void insertEvent(String payoutId, String type, String from, String to, String actorId, String details) {
    db.update("insert into payout_event(id,payout_id,event_type,from_status,to_status,actor_id,details,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP(6))",
        UUID.randomUUID().toString(), payoutId, type, from, to, actorId, Rows.clip(details, 500));
  }

  public List<Map<String, Object>> events(String payoutId) {
    return db.queryForList("select e.*, u.full_name actor_name from payout_event e left join staff_user u on u.id=e.actor_id where e.payout_id=? order by e.created_at", payoutId);
  }

  public void insertEvidence(String id, String payoutId, String key, String name, String contentType, long size, String actorId) {
    db.update("insert into payout_evidence(id,payout_id,object_key,file_name,content_type,size_bytes,uploaded_by,uploaded_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, payoutId, key, name, contentType, size, actorId);
  }

  public List<Map<String, Object>> evidence(String payoutId) {
    return db.queryForList("select e.id,e.file_name,e.content_type,e.size_bytes,e.uploaded_at,u.full_name uploaded_by_name from payout_evidence e" +
        " join staff_user u on u.id=e.uploaded_by where e.payout_id=? order by e.uploaded_at", payoutId);
  }

  public Optional<Map<String, Object>> evidenceFile(String payoutId, String evidenceId) {
    return db.queryForList("select * from payout_evidence where id=? and payout_id=?", evidenceId, payoutId).stream().findFirst();
  }
}
