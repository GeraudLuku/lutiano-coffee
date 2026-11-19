package com.lutiano.api.repository.campaign;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Campaigns, their tiers, the sales that count toward them and the bonuses paid. */
@Repository
public class CampaignRepository {
  private static final String SELECT = "select c.*, o.name operation_name from campaign c left join operation o on o.id=c.operation_id";
  /** Net paid value per seller: confirmed sales less the refunds paid on them. */
  private static final String NET_SALES = "select s.seller_id, coalesce(sum(s.total_xaf-coalesce(rf.paid,0)),0) sales_xaf from sale s" +
      " left join (select sale_id, sum(amount_xaf) paid from refund where status='PAID' group by sale_id) rf on rf.sale_id=s.id" +
      " where s.total_xaf>0 and s.created_at>=? and s.created_at<? and (? is null or s.operation_id=?)" +
      " and exists (select 1 from payment pm where pm.sale_id=s.id and pm.status='CONFIRMED') group by s.seller_id";
  /** Active sellers a campaign reaches: everyone, or those assigned to its location. */
  private static final String ELIGIBLE = " u.role='SELLER' and u.active=true and (? is null or exists (select 1 from seller_operation x where x.seller_id=u.id and x.operation_id=?))";
  private final JdbcTemplate db;

  public CampaignRepository(JdbcTemplate db) { this.db = db; }

  /** A DATE column as a day. */
  public static LocalDate day(Object v) { return v instanceof Date d ? d.toLocalDate() : LocalDate.parse(String.valueOf(v)); }

  public void insert(String id, String name, String description, LocalDate from, LocalDate to, String operationId, String actorId) {
    db.update("insert into campaign(id,name,description,starts_on,ends_on,operation_id,created_by,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, name, description, Date.valueOf(from), Date.valueOf(to), operationId, actorId);
  }

  public void insertTier(String campaignId, long threshold, long bonus) {
    db.update("insert into campaign_tier(campaign_id,threshold_xaf,bonus_xaf) values(?,?,?)", campaignId, threshold, bonus);
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where c.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> tiers(String id) {
    return db.queryForList("select threshold_xaf, bonus_xaf from campaign_tier where campaign_id=? order by threshold_xaf", id);
  }

  /**
   * Campaigns newest first. state: CURRENT, UPCOMING or ENDED (a cancelled campaign counts as ended); null for all. With a seller, only
   * the campaigns that reach them and are not cancelled.
   */
  public PageResponse<Map<String, Object>> page(String state, String sellerId, LocalDate today, PageRequest req) {
    StringBuilder where = new StringBuilder(" where 1=1");
    List<Object> args = new ArrayList<>();
    Date day = Date.valueOf(today);
    if (state != null) {
      switch (state) {
        case "CURRENT" -> { where.append(" and c.status='ACTIVE' and c.starts_on<=? and c.ends_on>=?"); args.add(day); args.add(day); }
        case "UPCOMING" -> { where.append(" and c.status='ACTIVE' and c.starts_on>?"); args.add(day); }
        default -> { where.append(" and (c.status<>'ACTIVE' or c.ends_on<?)"); args.add(day); }
      }
    }
    if (sellerId != null) {
      where.append(" and c.status='ACTIVE' and (c.operation_id is null or exists (select 1 from seller_operation x where x.seller_id=? and x.operation_id=c.operation_id))");
      args.add(sellerId);
    }
    Long total = db.queryForObject("select count(*) from campaign c" + where, Long.class, args.toArray());
    List<Object> pageArgs = new ArrayList<>(args);
    pageArgs.add(req.size());
    pageArgs.add(req.offset());
    return PageResponse.of(db.queryForList(SELECT + where + " order by c.starts_on desc, c.id limit ? offset ?", pageArgs.toArray()), req, total == null ? 0 : total);
  }

  /** Net sales of one seller inside the campaign's days (and location). */
  public long salesOf(String sellerId, Map<String, Object> c) {
    Timestamp from = Periods.startOf(day(c.get("starts_on"))), to = Periods.endOf(day(c.get("ends_on")));
    return db.queryForList(NET_SALES.replace(" group by s.seller_id", " and s.seller_id=? group by s.seller_id"), from, to, c.get("operation_id"), c.get("operation_id"), sellerId)
        .stream().findFirst().map(r -> ((Number) r.get("sales_xaf")).longValue()).orElse(0L);
  }

  /** Every seller the campaign reaches with their net sales, best first. */
  public PageResponse<Map<String, Object>> standings(Map<String, Object> c, PageRequest req) {
    Timestamp from = Periods.startOf(day(c.get("starts_on"))), to = Periods.endOf(day(c.get("ends_on")));
    Object op = c.get("operation_id");
    String base = "from staff_user u left join (" + NET_SALES + ") x on x.seller_id=u.id where" + ELIGIBLE;
    Long total = db.queryForObject("select count(*) " + base, Long.class, from, to, op, op, op, op);
    List<Map<String, Object>> rows = db.queryForList("select u.id seller_id, u.full_name seller_name, coalesce(x.sales_xaf,0) sales_xaf " + base +
        " order by sales_xaf desc, u.id limit ? offset ?", from, to, op, op, op, op, req.size(), req.offset());
    return PageResponse.of(rows, req, total == null ? 0 : total);
  }

  /** All sellers the campaign reaches with their sales, for settling (no paging: it is processed, not shown). */
  public List<Map<String, Object>> everyone(Map<String, Object> c) {
    Timestamp from = Periods.startOf(day(c.get("starts_on"))), to = Periods.endOf(day(c.get("ends_on")));
    Object op = c.get("operation_id");
    return db.queryForList("select u.id seller_id, u.full_name seller_name, coalesce(x.sales_xaf,0) sales_xaf from staff_user u left join (" + NET_SALES +
        ") x on x.seller_id=u.id where" + ELIGIBLE + " order by u.id", from, to, op, op, op, op);
  }

  /** Whether an active campaign reaches this seller: it has no location, or the seller is assigned to it. */
  public boolean reaches(String sellerId, Map<String, Object> c) {
    return "ACTIVE".equals(c.get("status")) && (c.get("operation_id") == null || Boolean.TRUE.equals(db.queryForObject(
        "select exists (select 1 from seller_operation where seller_id=? and operation_id=?)", Boolean.class, sellerId, c.get("operation_id"))));
  }

  /** Marks the campaign settled; false when somebody settled it first. */
  public boolean markSettled(String id) { return db.update("update campaign set settled_at=UTC_TIMESTAMP() where id=? and settled_at is null", id) == 1; }

  public boolean cancel(String id) { return db.update("update campaign set status='CANCELLED' where id=? and settled_at is null and status='ACTIVE'", id) == 1; }

  public void insertBonus(String campaignId, String sellerId, long sales, long bonus, String adjustmentId) {
    db.update("insert into campaign_bonus(campaign_id,seller_id,sales_xaf,bonus_xaf,adjustment_id,created_at) values(?,?,?,?,?,UTC_TIMESTAMP())", campaignId, sellerId, sales, bonus, adjustmentId);
  }

  public Optional<Map<String, Object>> bonusOf(String campaignId, String sellerId) {
    return db.queryForList("select sales_xaf, bonus_xaf, created_at from campaign_bonus where campaign_id=? and seller_id=?", campaignId, sellerId).stream().findFirst();
  }

  public Map<String, Object> paidTotals(String campaignId) {
    return db.queryForMap("select count(*) sellers_paid, coalesce(sum(bonus_xaf),0) bonus_xaf from campaign_bonus where campaign_id=?", campaignId);
  }
}
