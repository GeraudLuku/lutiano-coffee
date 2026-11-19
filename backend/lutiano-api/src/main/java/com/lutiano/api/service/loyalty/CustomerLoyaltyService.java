package com.lutiano.api.service.loyalty;

import java.util.Set;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.repository.loyalty.WalletRepository;

/** What a customer sees of the loyalty programme: their points and tier, history, the rewards on offer, and how points work. */
@Service
public class CustomerLoyaltyService {
  private final PointsRepository points;
  private final LoyaltyRepository config;
  private final WalletRepository wallet;

  public CustomerLoyaltyService(PointsRepository points, LoyaltyRepository config, WalletRepository wallet) { this.points = points; this.config = config; this.wallet = wallet; }

  /** Balance, tier and progress to the next one (tiers follow the last 12 months of spending), points about to expire, rewards waiting in the wallet. */
  public Map<String, Object> overview(Actor me) {
    Map<String, Object> c = points.customer(me.id()).orElseThrow();
    Map<String, Object> settings = config.settings();
    List<Map<String, Object>> tiers = config.tiers().stream().filter(t -> Rows.truthy(t.get("active"))).toList();
    Map<String, Object> current = tiers.stream().filter(t -> t.get("code").equals(c.get("tier_code"))).findFirst().orElse(tiers.isEmpty() ? null : tiers.get(0));
    long spend = points.annualSpend(me.id(), Timestamp.from(ZonedDateTime.now(Periods.DOUALA).minusMonths(12).toInstant()));
    Map<String, Object> next = current == null ? null : tiers.stream().filter(t -> Rows.asLong(t.get("min_annual_spend_xaf")) > Rows.asLong(current.get("min_annual_spend_xaf"))).findFirst().orElse(null);
    int notice = Rows.asInt(settings.get("expiry_notice_days"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("programme_active", Rows.truthy(settings.get("program_active")));
    out.put("points_balance", points.balance(me.id()));
    out.put("tier", current == null ? null : tierView(current, current));
    out.put("next_tier", next == null ? null : tierView(next, current));
    out.put("annual_spend_xaf", spend);
    out.put("remaining_to_next_xaf", next == null ? 0 : Math.max(0, Rows.asLong(next.get("min_annual_spend_xaf")) - spend));
    out.put("expiring_points", points.expiringSoon(me.id(), Timestamp.from(ZonedDateTime.now(Periods.DOUALA).plusDays(notice).toInstant())));
    out.put("expiry_notice_days", notice);
    out.put("rewards_available", wallet.availableCount(me.id()));
    out.put("min_redeem_points", settings.get("min_redeem_points"));
    return out;
  }

  /** Every tier with what it needs and gives; the customer's own is marked. */
  public List<Map<String, Object>> tiers(Actor me) {
    Map<String, Object> c = points.customer(me.id()).orElseThrow();
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> t : config.tiers()) {
      if (!Rows.truthy(t.get("active"))) continue;
      Map<String, Object> v = tierView(t, t);
      v.put("current", t.get("code").equals(c.get("tier_code")));
      out.add(v);
    }
    return out;
  }

  /** Points history, newest first. type: EARNED, USED or EXPIRED (null for all). */
  public PageResponse<Map<String, Object>> history(Actor me, String type, PageRequest req) {
    String entry = type == null || type.isBlank() ? null : switch (Validate.oneOf(type, "Type", Set.of("EARNED", "USED", "EXPIRED"))) {
      case "USED" -> "REDEEMED";
      default -> type;
    };
    return config.ledger(me.id(), entry, null, null, req).map(r -> {
      Map<String, Object> m = new LinkedHashMap<>();
      for (String k : new String[] {"id", "entry_type", "points", "balance_after", "reference", "description", "source_type", "source_id", "created_at"}) m.put(k, r.get(k));
      return m;
    });
  }

  /** Points that will expire, soonest first. */
  public PageResponse<Map<String, Object>> expiries(Actor me, PageRequest req) { return points.expiringBatches(me.id(), req); }

  /** How points work, in the numbers the programme uses now. */
  public Map<String, Object> rules() {
    Map<String, Object> s = config.settings();
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("base_points", s.get("base_points"));
    out.put("base_amount_xaf", s.get("base_amount_xaf"));
    out.put("expiry_months", s.get("expiry_months"));
    out.put("min_redeem_points", s.get("min_redeem_points"));
    List<Map<String, Object>> rules = new ArrayList<>();
    for (Map<String, Object> r : config.activeRules(Periods.today())) {
      Map<String, Object> m = new LinkedHashMap<>();
      for (String k : new String[] {"name", "method", "scope", "category", "product_name", "points", "amount_xaf", "multiplier_pct", "starts_on", "ends_on"}) m.put(k, r.get(k));
      rules.add(m);
    }
    out.put("rules", rules);
    return out;
  }

  /** About how many points a purchase earns (the base rate and the customer's tier; promotions are not counted). */
  public Map<String, Object> estimate(Actor me, Long amountXaf) {
    long amount = Validate.range(amountXaf, "Amount", 1, 100_000_000L);
    Map<String, Object> s = config.settings();
    Map<String, Object> c = points.customer(me.id()).orElseThrow();
    int pct = config.tiers().stream().filter(t -> t.get("code").equals(c.get("tier_code"))).findFirst().map(t -> Rows.asInt(t.get("multiplier_pct"))).orElse(100);
    long base = amount / Math.max(1, Rows.asLong(s.get("base_amount_xaf"))) * Rows.asLong(s.get("base_points"));
    return Map.of("amount_xaf", amount, "points", Rows.truthy(s.get("program_active")) ? base * pct / 100 : 0, "tier_multiplier_pct", pct);
  }

  /** The rewards on offer, cheapest first, each saying whether this customer can redeem it now and if not why. */
  public PageResponse<Map<String, Object>> rewards(Actor me, PageRequest req) {
    Map<String, Object> c = points.customer(me.id()).orElseThrow();
    Map<String, Object> s = config.settings();
    int balance = points.balance(me.id());
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> w : config.rewards()) {
      if (!Rows.truthy(w.get("active"))) continue;
      Map<String, Object> m = new LinkedHashMap<>();
      for (String k : new String[] {"id", "name", "reward_type", "description", "point_cost", "product_name", "image_url", "discount_pct", "min_purchases", "min_spend_xaf"}) m.put(k, w.get(k));
      String why = null;
      if (balance < Rows.asInt(s.get("min_redeem_points"))) why = "MIN_POINTS";
      else if (balance < Rows.asInt(w.get("point_cost"))) why = "NOT_ENOUGH_POINTS";
      else if (Rows.asInt(c.get("purchases")) < Rows.asInt(w.get("min_purchases"))) why = "MIN_PURCHASES";
      else if (Rows.asLong(c.get("lifetime_spend_xaf")) < Rows.asLong(w.get("min_spend_xaf"))) why = "MIN_SPEND";
      m.put("can_redeem", why == null);
      m.put("blocked_by", why);
      out.add(m);
    }
    return PageResponse.slice(out, req);
  }

  public Map<String, Object> reward(Actor me, String id) {
    return rewards(me, PageRequest.of(1, 200)).items().stream().filter(r -> id.equals(r.get("id"))).findFirst().orElseThrow(() -> Validate.notFound("Reward not found."));
  }

  private static Map<String, Object> tierView(Map<String, Object> t, Map<String, Object> from) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (String k : new String[] {"code", "name", "color", "min_annual_spend_xaf", "multiplier_pct", "benefits"}) m.put(k, t.get(k));
    return m;
  }
}
