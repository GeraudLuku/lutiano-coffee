package com.lutiano.api.service.loyalty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Rows;
import org.springframework.stereotype.Component;
import com.lutiano.api.entity.EarningLine;

/**
 * Points for a sale, from the rules active that day:
 * <ol>
 *   <li>Base points per line at the most specific rate: product RATE rule, else category RATE rule, else the default
 *       (base_points per base_amount_xaf). Only loyalty-eligible products earn.</li>
 *   <li>The member's tier multiplier applies to the base points.</li>
 *   <li>Bonuses stack: PER_UNIT points per unit, MULTIPLIER promotions on promotion-eligible lines, FIRST_PURCHASE once.</li>
 * </ol>
 * Integer arithmetic only; each part rounds down.
 */
@Component
public class EarningCalculator {
  public record Result(long eligibleXaf, int basePoints, int tierPct, int bonusPoints, int totalPoints, String breakdown) {}

  public Result calculate(List<EarningLine> lines, Map<String, Object> settings, List<Map<String, Object>> rules, int tierPct, boolean firstPurchase) {
    int defaultPoints = Rows.asInt(settings.get("base_points"));
    long defaultAmount = Rows.asLong(settings.get("base_amount_xaf"));
    long eligible = 0;
    long base = 0, bonus = 0;
    List<String> notes = new ArrayList<>();
    for (EarningLine l : lines) {
      if (!l.loyaltyEligible()) continue;
      eligible += l.totalXaf();
      Map<String, Object> rate = pick(rules, "RATE", l);
      int pts = rate == null ? defaultPoints : Rows.asInt(rate.get("points"));
      long per = rate == null ? defaultAmount : Math.max(1, Rows.asLong(rate.get("amount_xaf")));
      long lineBase = l.totalXaf() * pts / per;
      base += lineBase;
      if (rate != null) notes.add(l.name() + ": " + rate.get("name") + " (" + pts + " pts / " + per + " XAF)");
      for (Map<String, Object> r : rules) {
        if (!matches(r, l)) continue;
        if ("PER_UNIT".equals(r.get("method"))) {
          long b = (long) Rows.asInt(r.get("points")) * l.quantity();
          bonus += b;
          notes.add(r.get("name") + ": +" + b);
        } else if ("MULTIPLIER".equals(r.get("method")) && l.promotionEligible()) {
          long b = lineBase * (Rows.asInt(r.get("multiplier_pct")) - 100) / 100;
          if (b > 0) { bonus += b; notes.add(r.get("name") + ": +" + b); }
        }
      }
    }
    long tiered = base * tierPct / 100;
    if (tierPct != 100 && tiered != base) notes.add("Tier ×" + (tierPct / 100.0) + ": +" + (tiered - base));
    if (firstPurchase && eligible > 0)
      for (Map<String, Object> r : rules)
        if ("FIRST_PURCHASE".equals(r.get("method"))) { bonus += Rows.asInt(r.get("points")); notes.add(r.get("name") + ": +" + r.get("points")); }
    int total = Math.toIntExact(tiered + bonus);
    String breakdown = String.join("; ", notes);
    return new Result(eligible, (int) tiered, tierPct, (int) bonus, total, Rows.clip(breakdown, 1000));
  }

  /** The most specific rule of a method for a line: product (or its parent) first, then category. */
  private static Map<String, Object> pick(List<Map<String, Object>> rules, String method, EarningLine l) {
    Map<String, Object> category = null;
    for (Map<String, Object> r : rules) {
      if (!method.equals(r.get("method"))) continue;
      if ("PRODUCT".equals(r.get("scope")) && (l.productId().equals(r.get("product_id")) || (l.parentId() != null && l.parentId().equals(r.get("product_id"))))) return r;
      if ("CATEGORY".equals(r.get("scope")) && l.category() != null && l.category().equals(r.get("category")) && category == null) category = r;
    }
    return category;
  }

  private static boolean matches(Map<String, Object> r, EarningLine l) {
    return switch ((String) r.get("scope")) {
      case "ALL" -> true;
      case "PRODUCT" -> l.productId().equals(r.get("product_id")) || (l.parentId() != null && l.parentId().equals(r.get("product_id")));
      case "CATEGORY" -> l.category() != null && l.category().equals(r.get("category"));
      default -> false;
    };
  }

}
