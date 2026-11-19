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
import java.time.LocalDate;
import com.lutiano.api.definitions.enums.SearchType;

/** Programme configuration: settings, earning rules, tiers and rewards. */
@Repository
public class LoyaltyRepository {
  private static final String RULE_SELECT = "select r.*, p.name product_name from earning_rule r left join product p on p.id=r.product_id";
  private static final String REWARD_SELECT = "select w.*, p.name product_name, p.image_url," +
      " (select count(*) from reward_redemption x where x.reward_id=w.id and x.status='COMPLETED') redemptions" +
      " from reward w left join product p on p.id=w.product_id";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public LoyaltyRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- settings -----

  public Map<String, Object> settings() { return db.queryForMap("select * from loyalty_settings where id=1"); }

  public void updateSettings(boolean active, int basePoints, long baseAmount, int pointValue, int expiryMonths, int noticeDays, int minRedeem, String actorId) {
    db.update("update loyalty_settings set program_active=?,base_points=?,base_amount_xaf=?,point_value_xaf=?,expiry_months=?,expiry_notice_days=?," +
        "min_redeem_points=?,updated_by=?,updated_at=UTC_TIMESTAMP() where id=1", active, basePoints, baseAmount, pointValue, expiryMonths, noticeDays, minRedeem, actorId);
  }

  // ----- earning rules -----

  public List<Map<String, Object>> rules() { return db.queryForList(RULE_SELECT + " order by r.active desc, field(r.method,'RATE','PER_UNIT','MULTIPLIER','FIRST_PURCHASE'), r.name"); }

  public Optional<Map<String, Object>> rule(String id) { return db.queryForList(RULE_SELECT + " where r.id=?", id).stream().findFirst(); }

  /** Rules active on a business day (dates inclusive). */
  public List<Map<String, Object>> activeRules(LocalDate day) {
    return db.queryForList("select * from earning_rule where active=TRUE and (starts_on is null or starts_on<=?) and (ends_on is null or ends_on>=?)", day, day);
  }

  public void saveRule(String id, boolean insert, String name, String method, String scope, String productId, String category, int points, Long amount,
                       Integer multiplier, LocalDate startsOn, LocalDate endsOn, boolean active) {
    if (insert)
      db.update("insert into earning_rule(id,name,method,scope,product_id,category,points,amount_xaf,multiplier_pct,starts_on,ends_on,active,created_at)" +
          " values(?,?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", id, name, method, scope, productId, category, points, amount, multiplier, startsOn, endsOn, active);
    else
      db.update("update earning_rule set name=?,method=?,scope=?,product_id=?,category=?,points=?,amount_xaf=?,multiplier_pct=?,starts_on=?,ends_on=?,active=?," +
          "updated_at=UTC_TIMESTAMP() where id=?", name, method, scope, productId, category, points, amount, multiplier, startsOn, endsOn, active, id);
    search.changed(SearchType.EARNING_RULE, id);
  }

  public void deleteRule(String id) { db.update("delete from earning_rule where id=?", id); search.changed(SearchType.EARNING_RULE, id); }

  // ----- tiers -----

  public List<Map<String, Object>> tiers() {
    return db.queryForList("select t.*, (select count(*) from customer c where c.tier_code=t.code) members from loyalty_tier t order by t.sort_order");
  }

  public void updateTier(String code, String name, String color, long minSpend, int multiplier, String benefits, boolean active) {
    db.update("update loyalty_tier set name=?,color=?,min_annual_spend_xaf=?,multiplier_pct=?,benefits=?,active=? where code=?",
        name, color, minSpend, multiplier, benefits, active, code);
  }

  // ----- rewards -----

  public List<Map<String, Object>> rewards() { return db.queryForList(REWARD_SELECT + " order by w.active desc, w.sort_order, w.point_cost"); }

  public Optional<Map<String, Object>> reward(String id) { return db.queryForList(REWARD_SELECT + " where w.id=?", id).stream().findFirst(); }

  public void saveReward(String id, boolean insert, String name, String type, String description, int cost, String productId, boolean requiresStock,
                         Integer discountPct, int minPurchases, long minSpend, boolean active) {
    if (insert)
      db.update("insert into reward(id,name,reward_type,description,point_cost,product_id,requires_stock,discount_pct,min_purchases,min_spend_xaf,active,created_at)" +
          " values(?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())", id, name, type, description, cost, productId, requiresStock, discountPct, minPurchases, minSpend, active);
    else
      db.update("update reward set name=?,reward_type=?,description=?,point_cost=?,product_id=?,requires_stock=?,discount_pct=?,min_purchases=?,min_spend_xaf=?," +
          "active=?,updated_at=UTC_TIMESTAMP() where id=?", name, type, description, cost, productId, requiresStock, discountPct, minPurchases, minSpend, active, id);
    search.changed(SearchType.REWARD, id);
  }

  // ----- reporting -----

  public Map<String, Object> overview(Timestamp from, Timestamp to, Timestamp now) {
    return db.queryForMap("select" +
        " (select count(*) from customer where status='ACTIVE') members," +
        " (select count(distinct customer_id) from loyalty_ledger where created_at>=? and created_at<?) active_members," +
        " (select coalesce(sum(points),0) from loyalty_ledger where entry_type='EARNED' and created_at>=? and created_at<?) points_earned," +
        " (select coalesce(-sum(points),0) from loyalty_ledger where entry_type in ('REDEEMED','REDEMPTION_REVERSED') and created_at>=? and created_at<?) points_redeemed," +
        " (select coalesce(-sum(points),0) from loyalty_ledger where entry_type='EXPIRED' and created_at>=? and created_at<?) points_expired," +
        " (select coalesce(sum(remaining),0) from point_batch where expires_at>?) points_balance," +
        " (select count(*) from reward_redemption where status='COMPLETED' and created_at>=? and created_at<?) redemptions",
        from, to, from, to, from, to, from, to, now, from, to);
  }

  /** Points earned and redeemed per Douala business day. */
  public List<Map<String, Object>> daily(Timestamp from, Timestamp to) {
    return db.queryForList("select DATE(DATE_ADD(created_at, INTERVAL 1 HOUR)) day," +
        " coalesce(sum(case when entry_type='EARNED' then points end),0) earned," +
        " coalesce(-sum(case when entry_type in ('REDEEMED','REDEMPTION_REVERSED') then points end),0) redeemed" +
        " from loyalty_ledger where created_at>=? and created_at<? group by day order by day", from, to);
  }

  public List<Map<String, Object>> redemptionsByType(Timestamp from, Timestamp to) {
    return db.queryForList("select w.reward_type, count(*) redemptions, sum(x.points) points from reward_redemption x join reward w on w.id=x.reward_id" +
        " where x.status='COMPLETED' and x.created_at>=? and x.created_at<? group by w.reward_type order by points desc", from, to);
  }

  public List<Map<String, Object>> membersByTier() {
    return db.queryForList("select t.code, t.name, t.color, count(c.id) members from loyalty_tier t left join customer c on c.tier_code=t.code and c.status='ACTIVE'" +
        " group by t.code, t.name, t.color, t.sort_order order by t.sort_order");
  }

  private static SqlFilter ledgerFilter(String customerId, String entryType, Timestamp from, Timestamp to) {
    return new SqlFilter("select l.*, c.full_name customer_name, c.code customer_code, u.full_name actor_name from loyalty_ledger l" +
        " join customer c on c.id=l.customer_id left join staff_user u on u.id=l.actor_id")
        .and("l.customer_id=?", customerId).and("l.entry_type=?", entryType).and("l.created_at>=?", from).and("l.created_at<?", to);
  }


  public PageResponse<Map<String, Object>> ledger(String customerId, String entryType, Timestamp from, Timestamp to, PageRequest req) {
    return ledgerFilter(customerId, entryType, from, to).page(db, "order by l.created_at desc, l.id desc", req);
  }
}
