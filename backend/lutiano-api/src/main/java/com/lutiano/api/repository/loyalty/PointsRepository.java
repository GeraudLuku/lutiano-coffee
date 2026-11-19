package com.lutiano.api.repository.loyalty;

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
import com.lutiano.api.definitions.enums.SearchType;
import com.lutiano.api.entity.EarningLine;

/** Customers, point batches, ledger entries, awards, redemptions and adjustments. */
@Repository
public class PointsRepository {
  private static final String CUSTOMER_SELECT = "select c.id,c.code,c.full_name,c.phone,c.phone_verified,c.status,c.tier_code,c.created_at," +
      " t.name tier_name, t.color tier_color," +
      " (select coalesce(sum(b.remaining),0) from point_batch b where b.customer_id=c.id and b.expires_at>UTC_TIMESTAMP()) points_balance," +
      " (select count(*) from sale s where s.customer_id=c.id) purchases," +
      " (select coalesce(sum(s.total_xaf),0) from sale s where s.customer_id=c.id) lifetime_spend_xaf," +
      " (select max(s.created_at) from sale s where s.customer_id=c.id) last_purchase_at" +
      " from customer c left join loyalty_tier t on t.code=c.tier_code";
  private static final String REDEMPTION_SELECT = "select x.*, c.full_name customer_name, c.code customer_code, w.reward_type, o.name operation_name," +
      " u.full_name created_by_name, rv.full_name reversed_by_name from reward_redemption x join customer c on c.id=x.customer_id join reward w on w.id=x.reward_id" +
      " left join operation o on o.id=x.operation_id left join staff_user u on u.id=x.created_by left join staff_user rv on rv.id=x.reversed_by";
  private static final String ADJUSTMENT_SELECT = "select a.*, c.full_name customer_name, c.code customer_code, u.full_name created_by_name" +
      " from points_adjustment a join customer c on c.id=a.customer_id left join staff_user u on u.id=a.created_by";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public PointsRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- customers -----


  public void insertCustomer(String id, String code, String name, String phone, boolean verified, String token, String tier) {
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code,created_at) values(?,?,?,?,?,'ACTIVE',?,?,UTC_TIMESTAMP())",
        id, code, name, phone, verified, token, tier);
    search.changed(SearchType.CUSTOMER, id);
  }

  public void updateCustomer(String id, String name, String phone, boolean verified, String status) {
    db.update("update customer set full_name=?,phone=?,phone_verified=?,status=? where id=?", name, phone, verified, status, id);
    search.changed(SearchType.CUSTOMER, id);
    search.changedAll(SearchType.SALE, db.queryForList("select id from sale where customer_id=?", String.class, id));   // sales embed the customer's name
  }

  public boolean phoneTaken(String phone, String exceptId) {
    Integer n = db.queryForObject("select count(*) from customer where phone=? and id<>?", Integer.class, phone, exceptId == null ? "" : exceptId);
    return n != null && n > 0;
  }

  public void setToken(String id, String token) { db.update("update customer set loyalty_token=? where id=?", token, id); }
  public void setTier(String id, String tier) {
    db.update("update customer set tier_code=? where id=?", tier, id);
    search.changed(SearchType.CUSTOMER, id);
  }

  public Optional<Map<String, Object>> customer(String id) { return db.queryForList(CUSTOMER_SELECT + " where c.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> customerByToken(String token) {
    return db.queryForList("select * from customer where loyalty_token=?", token).stream().findFirst();
  }

  public String loyaltyToken(String id) { return db.queryForObject("select loyalty_token from customer where id=?", String.class, id); }

  /** Locks the customer so concurrent point changes are serialised. */
  public Optional<Map<String, Object>> lockCustomer(String id) {
    return db.queryForList("select * from customer where id=? for update", id).stream().findFirst();
  }

  /** Search by name or customer code (through the search service), or by exact phone number. */
  public PageResponse<Map<String, Object>> customers(Candidates text, String tier, String status, PageRequest req) {
    return new SqlFilter(CUSTOMER_SELECT).and("c.tier_code=?", tier).and("c.status=?", status)
        .searchIds(text, "c.id", "c.phone", "c.code")
        .page(db, "order by c.created_at desc, c.code desc", req);
  }

  /** Spend over the last 12 months, for tier qualification. */
  public long annualSpend(String customerId, Timestamp since) {
    return db.queryForObject("select coalesce(sum(total_xaf),0) from sale where customer_id=? and created_at>=?", Long.class, customerId, since);
  }

  public int earningSales(String customerId) {
    return db.queryForObject("select count(*) from loyalty_award where customer_id=?", Integer.class, customerId);
  }

  // ----- batches & ledger -----

  public void insertBatch(String id, String customerId, String sourceType, String sourceId, int points, Timestamp expiresAt) {
    db.update("insert into point_batch(id,customer_id,source_type,source_id,points,remaining,earned_at,expires_at) values(?,?,?,?,?,?,UTC_TIMESTAMP(6),?)",
        id, customerId, sourceType, sourceId, points, points, expiresAt);
  }

  /** Unexpired batches with points left, earliest expiry first (the order points are spent in). */
  public List<Map<String, Object>> spendableBatches(String customerId) {
    return db.queryForList("select * from point_batch where customer_id=? and remaining>0 and expires_at>UTC_TIMESTAMP() order by expires_at, earned_at, id for update", customerId);
  }

  public Optional<Map<String, Object>> batch(String id) { return db.queryForList("select * from point_batch where id=? for update", id).stream().findFirst(); }

  public void changeRemaining(String batchId, int delta) { db.update("update point_batch set remaining=remaining+? where id=?", delta, batchId); }

  public PageResponse<Map<String, Object>> batches(String customerId, PageRequest req) {
    return new SqlFilter("select b.*, (b.expires_at<=UTC_TIMESTAMP()) expired from point_batch b").and("b.customer_id=?", customerId)
        .page(db, "order by b.expires_at desc, b.id", req);
  }

  /** Batches past expiry that still hold points, oldest first (all customers, or one). */
  public List<Map<String, Object>> dueBatches(String customerId) {
    return customerId == null
        ? db.queryForList("select * from point_batch where remaining>0 and expires_at<=UTC_TIMESTAMP() order by customer_id, expires_at for update")
        : db.queryForList("select * from point_batch where customer_id=? and remaining>0 and expires_at<=UTC_TIMESTAMP() order by expires_at for update", customerId);
  }

  public int balance(String customerId) {
    return db.queryForObject("select coalesce(sum(remaining),0) from point_batch where customer_id=? and expires_at>UTC_TIMESTAMP()", Integer.class, customerId);
  }

  /** Points expiring within the notice window (for the expiry reminder). */
  public int expiringSoon(String customerId, Timestamp until) {
    return db.queryForObject("select coalesce(sum(remaining),0) from point_batch where customer_id=? and expires_at>UTC_TIMESTAMP() and expires_at<=?",
        Integer.class, customerId, until);
  }

  /** The points that will expire, soonest first (each batch with what is left of it). */
  public PageResponse<Map<String, Object>> expiringBatches(String customerId, PageRequest req) {
    return new SqlFilter("select b.id, b.remaining points, b.expires_at from point_batch b").and("b.customer_id=?", customerId).and("b.remaining>0").and("b.expires_at>UTC_TIMESTAMP()")
        .page(db, "order by b.expires_at, b.id", req);
  }

  public void insertLedger(String id, String customerId, String type, int points, int balanceAfter, String sourceType, String sourceId,
                           String reference, String description, String actorId) {
    db.update("insert into loyalty_ledger(id,customer_id,entry_type,points,balance_after,source_type,source_id,reference,description,actor_id,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP(6))", id, customerId, type, points, balanceAfter, sourceType, sourceId, reference, description, actorId);
  }

  /** A sale's lines with the product facts earning rules need (category, parent, eligibility). */
  public List<EarningLine> earningLines(String saleId) {
    return db.query("select l.product_id,p.parent_id,l.product_name,p.category,l.quantity,(l.unit_price_xaf+l.addons_unit_xaf)*l.quantity total," +
            "p.loyalty_eligible,p.promotion_eligible from sale_line l join product p on p.id=l.product_id where l.sale_id=? order by l.line_no",
        (rs, i) -> new EarningLine(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5), rs.getLong(6),
            rs.getBoolean(7), rs.getBoolean(8)), saleId);
  }

  // ----- awards -----

  public void insertAward(String saleId, String customerId, long eligible, int base, String tier, int tierPct, int bonus, int total, String breakdown, String batchId) {
    db.update("insert into loyalty_award(sale_id,customer_id,eligible_xaf,base_points,tier_code,tier_multiplier_pct,bonus_points,total_points,breakdown,batch_id,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", saleId, customerId, eligible, base, tier, tierPct, bonus, total, breakdown, batchId);
  }

  public Optional<Map<String, Object>> award(String saleId) {
    return db.queryForList("select a.*, c.full_name customer_name, c.code customer_code from loyalty_award a join customer c on c.id=a.customer_id where a.sale_id=?", saleId)
        .stream().findFirst();
  }

  public Optional<Map<String, Object>> lockAward(String saleId) {
    return db.queryForList("select * from loyalty_award where sale_id=? for update", saleId).stream().findFirst();
  }

  public void addReversed(String saleId, int points) { db.update("update loyalty_award set reversed_points=reversed_points+? where sale_id=?", points, saleId); }

  // ----- redemptions -----


  public void insertRedemption(String id, String reference, String customerId, String rewardId, String rewardName, int points, String operationId, String actorId) {
    db.update("insert into reward_redemption(id,reference,customer_id,reward_id,reward_name,points,operation_id,status,created_by,created_at)" +
        " values(?,?,?,?,?,?,?,'COMPLETED',?,UTC_TIMESTAMP())", id, reference, customerId, rewardId, rewardName, points, operationId, actorId);
    search.changed(SearchType.REDEMPTION, id);
  }

  public void insertRedemptionBatch(String redemptionId, String batchId, int points) {
    db.update("insert into redemption_batch(redemption_id,batch_id,points) values(?,?,?)", redemptionId, batchId, points);
  }

  public List<Map<String, Object>> redemptionBatches(String redemptionId) {
    return db.queryForList("select * from redemption_batch where redemption_id=?", redemptionId);
  }

  public Optional<Map<String, Object>> lockRedemption(String id) {
    return db.queryForList("select * from reward_redemption where id=? for update", id).stream().findFirst();
  }

  public void reverseRedemption(String id, String actorId, String reason) {
    db.update("update reward_redemption set status='REVERSED',reversed_by=?,reversed_at=UTC_TIMESTAMP(),reversal_reason=? where id=?", actorId, reason, id);
    search.changed(SearchType.REDEMPTION, id);
  }

  public Optional<Map<String, Object>> redemption(String id) { return db.queryForList(REDEMPTION_SELECT + " where x.id=?", id).stream().findFirst(); }

  public PageResponse<Map<String, Object>> redemptions(String customerId, String status, Timestamp from, Timestamp to, PageRequest req) {
    return new SqlFilter(REDEMPTION_SELECT).and("x.customer_id=?", customerId).and("x.status=?", status).and("x.created_at>=?", from).and("x.created_at<?", to)
        .page(db, "order by x.created_at desc, x.reference desc", req);
  }

  public int completedRedemptions(String customerId) {
    return db.queryForObject("select count(*) from reward_redemption where customer_id=? and status='COMPLETED'", Integer.class, customerId);
  }

  // ----- adjustments -----


  public void insertAdjustment(String id, String reference, String customerId, int points, String reason, String note, String actorId) {
    db.update("insert into points_adjustment(id,reference,customer_id,points,reason,note,created_by,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, reference, customerId, points, reason, note, actorId);
    search.changed(SearchType.POINTS_ADJUSTMENT, id);
  }

  public Optional<Map<String, Object>> adjustment(String id) { return db.queryForList(ADJUSTMENT_SELECT + " where a.id=?", id).stream().findFirst(); }

  /** One page of adjustments with totals (added, deducted, members affected) over every match. */
  public PageResponse<Map<String, Object>> adjustments(String customerId, String reason, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter(ADJUSTMENT_SELECT).and("a.customer_id=?", customerId).and("a.reason=?", reason).and("a.created_at>=?", from).and("a.created_at<?", to);
    return f.page(db, "order by a.created_at desc, a.reference desc", req).withSummary(f.aggregate(db,
        "count(*) adjustments, coalesce(sum(case when points>0 then points end),0) added, coalesce(sum(case when points<0 then points end),0) deducted," +
        " count(distinct customer_id) members"));
  }
}
