package com.lutiano.api.repository.earnings;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.UUID;
import com.lutiano.api.definitions.enums.SearchType;

/** Seller targets, earning periods, adjustments and frozen statements. */
@Repository
public class EarningsRepository {
  private static final String TARGET_SELECT = "select t.*, u.full_name seller_name, u.seller_role, p.name product_name, cb.full_name created_by_name" +
      " from seller_target t join staff_user u on u.id=t.seller_id left join product p on p.id=t.product_id left join staff_user cb on cb.id=t.created_by";
  private static final String ADJUSTMENT_SELECT = "select a.*, u.full_name seller_name, cb.full_name created_by_name" +
      " from earning_adjustment a join staff_user u on u.id=a.seller_id left join staff_user cb on cb.id=a.created_by";
  /** Paid value and units of each sale line net of refunds paid on its sale (refunds reduce achievement proportionally). */
  private static final String NET_LINES = "select s.seller_id, s.created_at, l.product_id, p.parent_id," +
      " (l.unit_price_xaf+l.addons_unit_xaf)*l.quantity*(s.total_xaf-coalesce(rf.paid,0))/s.total_xaf net_value," +
      " l.quantity*(s.total_xaf-coalesce(rf.paid,0))/s.total_xaf net_units" +
      " from sale s join sale_line l on l.sale_id=s.id join product p on p.id=l.product_id" +
      " left join (select sale_id, sum(amount_xaf) paid from refund where status='PAID' group by sale_id) rf on rf.sale_id=s.id" +
      " where s.total_xaf>0 and exists (select 1 from payment pm where pm.sale_id=s.id and pm.status='CONFIRMED')";

  private final JdbcTemplate db;
  private final SearchOutbox search;
  private final References references;

  public EarningsRepository(JdbcTemplate db, References references, SearchOutbox search) { this.db = db; this.search = search; this.references = references; }

  // ----- targets -----

  public void insertTarget(String id, String sellerId, String name, String metric, String productId, String periodType, LocalDate startsOn, LocalDate endsOn,
                           long targetValue, String bonusType, long bonusXaf, String actorId) {
    db.update("insert into seller_target(id,seller_id,name,metric,product_id,period_type,starts_on,ends_on,target_value,bonus_type,bonus_xaf,status,created_by,created_at)" +
            " values(?,?,?,?,?,?,?,?,?,?,?,'ACTIVE',?,UTC_TIMESTAMP())",
        id, sellerId, name, metric, productId, periodType, startsOn, endsOn, targetValue, bonusType, bonusXaf, actorId);
    search.changed(SearchType.TARGET, id);
  }

  public Optional<Map<String, Object>> target(String id) { return db.queryForList(TARGET_SELECT + " where t.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> lockTarget(String id) {
    return db.queryForList("select * from seller_target where id=? for update", id).stream().findFirst();
  }

  public void cancelTarget(String id) { db.update("update seller_target set status='CANCELLED' where id=?", id); search.changed(SearchType.TARGET, id); }

  /** state: CURRENT (running today), UPCOMING, ENDED; or all when null. */
  public PageResponse<Map<String, Object>> targets(String sellerId, String status, String state, LocalDate today, PageRequest req) {
    SqlFilter f = new SqlFilter(TARGET_SELECT).and("t.seller_id=?", sellerId).and("t.status=?", status);
    if ("CURRENT".equals(state)) f.and("t.starts_on<=?", today).and("t.ends_on>=?", today);
    else if ("UPCOMING".equals(state)) f.and("t.starts_on>?", today);
    else if ("ENDED".equals(state)) f.and("t.ends_on<?", today);
    return f.page(db, "order by t.ends_on desc, t.created_at desc, t.id desc", req);
  }

  /** A seller's active (not cancelled) targets, newest first; [state] CURRENT, UPCOMING or ENDED, and/or those overlapping [from, to] (either may be null). */
  public PageResponse<Map<String, Object>> activeTargetsOf(String sellerId, String state, LocalDate from, LocalDate to, LocalDate today, PageRequest req) {
    SqlFilter f = new SqlFilter(TARGET_SELECT).and("t.seller_id=?", sellerId).and("t.status='ACTIVE'");
    if ("CURRENT".equals(state)) f.and("t.starts_on<=?", today).and("t.ends_on>=?", today);
    else if ("UPCOMING".equals(state)) f.and("t.starts_on>?", today);
    else if ("ENDED".equals(state)) f.and("t.ends_on<?", today);
    f.and("t.starts_on<=?", to).and("t.ends_on>=?", from);
    return f.page(db, "order by t.ends_on desc, t.created_at desc, t.id desc", req);
  }

  /** Every active target of a seller overlapping [from, to] (a seller has few), for totals and trends. */
  public List<Map<String, Object>> activeTargetsOverlapping(String sellerId, LocalDate from, LocalDate to) {
    return db.queryForList(TARGET_SELECT + " where t.seller_id=? and t.status='ACTIVE' and t.starts_on<=? and t.ends_on>=? order by t.ends_on desc, t.id limit 500", sellerId, to, from);
  }

  /** Remembers that a target's milestone (80 or 100) was told; true the first time, false when it already was. */
  public boolean recordMilestone(String targetId, int milestone) {
    return db.update("insert ignore into target_milestone(target_id,milestone) values(?,?)", targetId, milestone) == 1;
  }

  /** Active targets of a seller whose period ends inside [from, to]. */
  public List<Map<String, Object>> targetsEndingBetween(String sellerId, LocalDate from, LocalDate to) {
    return db.queryForList(TARGET_SELECT + " where t.seller_id=? and t.status='ACTIVE' and t.ends_on between ? and ? order by t.ends_on", sellerId, from, to);
  }

  /** Paid bonuses (frozen at finalisation, not yet reversed) whose period contains the given day. */
  public List<Map<String, Object>> paidTargetsCovering(String sellerId, LocalDate day) {
    return db.queryForList("select * from seller_target where seller_id=? and status='ACTIVE' and bonus_paid_xaf>0 and bonus_reversed=false" +
        " and starts_on<=? and ends_on>=? for update", sellerId, day, day);
  }

  public void freezeBonus(String id, long bonus) { db.update("update seller_target set bonus_paid_xaf=? where id=?", bonus, id); search.changed(SearchType.TARGET, id); }
  public void markBonusReversed(String id) { db.update("update seller_target set bonus_reversed=true where id=?", id); search.changed(SearchType.TARGET, id); }

  /** Net paid value and units a seller sold in [from, to), optionally for one product (and its variants). */
  public Map<String, Object> achievement(String sellerId, String productId, Timestamp from, Timestamp to) {
    SqlFilter f = new SqlFilter("select * from (" + NET_LINES + ") n").and("n.seller_id=?", sellerId).and("n.created_at>=?", from).and("n.created_at<?", to);
    if (productId != null) f.and("? in (n.product_id, n.parent_id)", productId);
    return f.aggregate(db, "coalesce(floor(sum(net_value)),0) value_xaf, coalesce(floor(sum(net_units)),0) units");
  }

  // ----- periods -----

  public Optional<Map<String, Object>> periodStarting(LocalDate start) {
    return db.queryForList("select * from earning_period where starts_on=?", start).stream().findFirst();
  }

  public void insertPeriod(String id, LocalDate start, LocalDate end) {
    db.update("insert ignore into earning_period(id,starts_on,ends_on,status) values(?,?,?,'OPEN')", id, start, end);
  }

  public Optional<Map<String, Object>> period(String id) {
    return db.queryForList("select ep.*, u.full_name finalized_by_name from earning_period ep left join staff_user u on u.id=ep.finalized_by where ep.id=?", id)
        .stream().findFirst();
  }

  public Optional<Map<String, Object>> lockPeriod(String id) { return db.queryForList("select * from earning_period where id=? for update", id).stream().findFirst(); }

  public boolean openPeriodBefore(LocalDate start) {
    Integer n = db.queryForObject("select count(*) from earning_period where status='OPEN' and starts_on<?", Integer.class, start);
    return n != null && n > 0;
  }

  public PageResponse<Map<String, Object>> periods(PageRequest req) {
    return new SqlFilter("select ep.*, u.full_name finalized_by_name," +
        " (select coalesce(sum(se.total_xaf),0) from seller_earning se where se.period_id=ep.id) total_xaf," +
        " (select coalesce(sum(se.payable_xaf),0) from seller_earning se where se.period_id=ep.id) payable_xaf" +
        " from earning_period ep left join staff_user u on u.id=ep.finalized_by").page(db, "order by ep.starts_on desc, ep.id", req);
  }

  public void finalizePeriod(String id, String actorId) {
    db.update("update earning_period set status='FINALIZED', finalized_by=?, finalized_at=UTC_TIMESTAMP() where id=?", actorId, id);
  }

  // ----- statements -----

  public void insertStatement(String periodId, String sellerId, int salesCount, long salesXaf, long commission, long bonus, long adjustments,
                              long total, long payable, long carried) {
    db.update("insert into seller_earning(period_id,seller_id,sales_count,sales_xaf,commission_xaf,bonus_xaf,adjustments_xaf,total_xaf,payable_xaf,carried_xaf,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", periodId, sellerId, salesCount, salesXaf, commission, bonus, adjustments, total, payable, carried);
  }

  public List<Map<String, Object>> statements(String periodId) {
    return db.queryForList("select se.*, u.full_name seller_name, u.seller_role, u.active from seller_earning se join staff_user u on u.id=se.seller_id" +
        " where se.period_id=? order by se.total_xaf desc, u.full_name", periodId);
  }

  public PageResponse<Map<String, Object>> sellerStatements(String sellerId, PageRequest req) {
    return new SqlFilter("select se.*, ep.starts_on, ep.ends_on from seller_earning se join earning_period ep on ep.id=se.period_id")
        .and("se.seller_id=?", sellerId).page(db, "order by ep.starts_on desc, ep.id", req);
  }

  /** Every seller with their sales in [from, to) (sellers without sales included, for carried recoveries and adjustments). */
  public List<Map<String, Object>> sellerSales(Timestamp from, Timestamp to) {
    return db.queryForList("select u.id seller_id, u.full_name seller_name, u.seller_role, u.active," +
        " (select count(*) from sale s where s.seller_id=u.id and s.created_at>=? and s.created_at<?) sales_count," +
        " (select coalesce(sum(s.total_xaf),0) from sale s where s.seller_id=u.id and s.created_at>=? and s.created_at<?) sales_xaf" +
        " from staff_user u where u.role='SELLER' order by u.full_name", from, to, from, to);
  }

  // ----- adjustments -----

  /** Posts an adjustment (reference EA-NNNNNN) at {@code at}, or now. Returns its id. */
  public String insertAdjustment(String sellerId, String type, long amount, String sourceType, String sourceId, String note, String actorId, Timestamp at) {
    String id = UUID.randomUUID().toString();
    db.update("insert into earning_adjustment(id,reference,seller_id,adjustment_type,amount_xaf,source_type,source_id,note,created_by,created_at)" +
        " values(?,?,?,?,?,?,?,?,?,coalesce(?,UTC_TIMESTAMP()))",
        id, references.plain("EA", "earning_adjustment_seq"), sellerId, type, amount, sourceType, sourceId, note, actorId, at);
    return id;
  }

  public Optional<Map<String, Object>> adjustment(String id) { return db.queryForList(ADJUSTMENT_SELECT + " where a.id=?", id).stream().findFirst(); }

  public PageResponse<Map<String, Object>> adjustments(String sellerId, String type, Timestamp from, Timestamp to, PageRequest req) {
    SqlFilter f = new SqlFilter(ADJUSTMENT_SELECT).and("a.seller_id=?", sellerId).and("a.adjustment_type=?", type)
        .and("a.created_at>=?", from).and("a.created_at<?", to);
    return f.page(db, "order by a.created_at desc, a.reference desc", req)
        .withSummary(f.aggregate(db, "count(*) adjustments, coalesce(sum(amount_xaf),0) amount_xaf"));
  }

  /** Adjustment totals per seller in [from, to). */
  public List<Map<String, Object>> adjustmentTotals(Timestamp from, Timestamp to) {
    return db.queryForList("select seller_id, coalesce(sum(amount_xaf),0) amount_xaf from earning_adjustment where created_at>=? and created_at<? group by seller_id", from, to);
  }
}
