package com.lutiano.api.service.loyalty;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.repository.product.CatalogRepository;
import com.lutiano.api.repository.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.models.request.loyalty.EarningRuleRequest;
import com.lutiano.api.models.request.loyalty.LoyaltySettingsRequest;
import com.lutiano.api.models.request.loyalty.RewardRequest;
import com.lutiano.api.models.request.loyalty.TierRequest;
import com.lutiano.api.definitions.enums.EarningMethod;
import com.lutiano.api.definitions.enums.EarningScope;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.RewardType;

/** Programme settings, earning rules, tiers and rewards. Every change is audited and applies to future sales only. */
@Service
public class LoyaltyConfigService {
  public static final Set<String> METHODS = Enums.names(EarningMethod.class);
  public static final Set<String> SCOPES = Enums.names(EarningScope.class);
  public static final Set<String> REWARD_TYPES = Enums.names(RewardType.class);


  private final LoyaltyRepository repo;
  private final ProductRepository products;
  private final CatalogRepository catalog;
  private final AuditService audit;

  public LoyaltyConfigService(LoyaltyRepository repo, ProductRepository products, CatalogRepository catalog, AuditService audit) {
    this.repo = repo; this.products = products; this.catalog = catalog; this.audit = audit;
  }

  // ----- settings -----

  public Map<String, Object> settings() { return repo.settings(); }

  @Transactional
  public Map<String, Object> updateSettings(Actor actor, LoyaltySettingsRequest in) {
    int basePoints = Validate.range(in.basePoints(), "Base points", 0, 100000);
    long baseAmount = Validate.range(in.baseAmountXaf(), "Spend per base points", 1, 10_000_000);
    int pointValue = Validate.range(in.pointValueXaf(), "Point value", 0, 100000);
    int months = Validate.range(in.expiryMonths(), "Expiry", 1, 120);
    int notice = Validate.range(in.expiryNoticeDays(), "Expiry reminder", 0, 365);
    int minRedeem = Validate.range(in.minRedeemPoints(), "Minimum points to redeem", 0, 10_000_000);
    repo.updateSettings(!Boolean.FALSE.equals(in.programActive()), basePoints, baseAmount, pointValue, months, notice, minRedeem, actor.id());
    audit.log(actor, "LOYALTY_SETTINGS_UPDATED", "loyalty_settings", null,
        basePoints + " pts / " + baseAmount + " XAF, 1 pt = " + pointValue + " XAF, expiry " + months + " months");
    return repo.settings();
  }

  // ----- rules -----

  public List<Map<String, Object>> rules() { return repo.rules(); }

  @Transactional
  public Map<String, Object> saveRule(Actor actor, String id, EarningRuleRequest in) {
    boolean insert = id == null;
    if (!insert) repo.rule(Validate.id(id, "Rule")).orElseThrow(() -> Validate.notFound("Earning rule not found."));
    String ruleId = insert ? UUID.randomUUID().toString() : id;
    String name = Validate.text(in.name(), "Rule name", 100);
    String method = Validate.oneOf(in.method(), "Earning method", METHODS);
    String scope = "FIRST_PURCHASE".equals(method) ? "ALL" : Validate.oneOf(in.scope(), "Applies to", SCOPES);
    if (("RATE".equals(method) || "PER_UNIT".equals(method)) && "ALL".equals(scope))
      throw Validate.bad("Rate and per-unit rules apply to a product or a category. Change the default rate in Point Settings instead.");
    String productId = null, category = null;
    if ("PRODUCT".equals(scope)) {
      productId = Validate.id(in.productId(), "Product");
      if (products.findById(productId).isEmpty()) throw Validate.bad("Product not found.");
    } else if ("CATEGORY".equals(scope)) {
      category = Validate.text(in.category(), "Category", 60);
      if (!catalog.categoryExists(category)) throw Validate.bad("Category not found.");
    }
    int points = 0; Long amount = null; Integer multiplier = null;
    switch (method) {
      case "RATE" -> { points = Validate.range(in.points(), "Points", 1, 100000); amount = Validate.range(in.amountXaf(), "Per amount", 1, 10_000_000); }
      case "PER_UNIT", "FIRST_PURCHASE" -> points = Validate.range(in.points(), "Points", 1, 1_000_000);
      case "MULTIPLIER" -> multiplier = Validate.range(in.multiplierPct(), "Multiplier", 101, 1000);
      default -> { }
    }
    if (in.startsOn() != null && in.endsOn() != null && in.endsOn().isBefore(in.startsOn())) throw Validate.bad("The end date is before the start date.");
    repo.saveRule(ruleId, insert, name, method, scope, productId, category, points, amount, multiplier, in.startsOn(), in.endsOn(), !Boolean.FALSE.equals(in.active()));
    audit.log(actor, insert ? "EARNING_RULE_CREATED" : "EARNING_RULE_UPDATED", "earning_rule", ruleId, name + " (" + method + ")");
    return repo.rule(ruleId).orElseThrow();
  }

  @Transactional
  public void deleteRule(Actor actor, String id) {
    Map<String, Object> r = repo.rule(Validate.id(id, "Rule")).orElseThrow(() -> Validate.notFound("Earning rule not found."));
    repo.deleteRule(id);
    audit.log(actor, "EARNING_RULE_DELETED", "earning_rule", id, (String) r.get("name"));
  }

  // ----- tiers -----

  public List<Map<String, Object>> tiers() { return repo.tiers(); }

  @Transactional
  public List<Map<String, Object>> updateTier(Actor actor, String code, TierRequest in) {
    Map<String, Object> tier = repo.tiers().stream().filter(t -> t.get("code").equals(code)).findFirst().orElseThrow(() -> Validate.notFound("Tier not found."));
    long minSpend = Validate.range(in.minAnnualSpendXaf(), "Qualifying annual spend", 0, 1_000_000_000L);
    if ("BRONZE".equals(code) && minSpend != 0) throw Validate.bad("The entry tier must start at 0 XAF.");
    String color = in.color() == null || !in.color().matches("^#[0-9A-Fa-f]{6}$") ? (String) tier.get("color") : in.color();
    repo.updateTier(code, Validate.text(in.name(), "Tier name", 40), color, minSpend, Validate.range(in.multiplierPct(), "Multiplier", 100, 1000),
        Validate.optional(in.benefits(), "Benefits", 500), "BRONZE".equals(code) || !Boolean.FALSE.equals(in.active()));
    audit.log(actor, "LOYALTY_TIER_UPDATED", "loyalty_tier", null, code + ": from " + minSpend + " XAF, ×" + in.multiplierPct() + "%");
    return repo.tiers();
  }

  // ----- rewards -----

  public List<Map<String, Object>> rewards() { return repo.rewards(); }

  @Transactional
  public Map<String, Object> saveReward(Actor actor, String id, RewardRequest in) {
    boolean insert = id == null;
    if (!insert) repo.reward(Validate.id(id, "Reward")).orElseThrow(() -> Validate.notFound("Reward not found."));
    String rewardId = insert ? UUID.randomUUID().toString() : id;
    String type = Validate.oneOf(in.rewardType(), "Reward type", REWARD_TYPES);
    String productId = in.productId() == null || in.productId().isBlank() ? null : Validate.id(in.productId(), "Product");
    boolean requiresStock = Boolean.TRUE.equals(in.requiresStock());
    Integer discount = null;
    if ("DISCOUNT_VOUCHER".equals(type)) { discount = Validate.range(in.discountPct(), "Discount", 1, 100); productId = null; requiresStock = false; }
    else if (!"MERCHANDISE".equals(type) && productId == null) throw Validate.bad("Choose the product given with this reward.");
    if (requiresStock) {
      if (productId == null) throw Validate.bad("Choose the product whose stock this reward uses.");
      boolean tracked = products.findById(productId).map(x -> Rows.truthy(x.get("track_stock"))).orElse(false);
      if (!tracked) throw Validate.bad("That product is not stock-tracked, so the reward cannot take stock.");
    }
    repo.saveReward(rewardId, insert, Validate.text(in.name(), "Reward name", 100), type, Validate.optional(in.description(), "Description", 500),
        Validate.range(in.pointCost(), "Point cost", 1, 10_000_000), productId, requiresStock, discount,
        in.minPurchases() == null ? 0 : Validate.range(in.minPurchases(), "Minimum purchases", 0, 100000),
        in.minSpendXaf() == null ? 0 : Validate.range(in.minSpendXaf(), "Minimum spend", 0, 1_000_000_000L), !Boolean.FALSE.equals(in.active()));
    audit.log(actor, insert ? "REWARD_CREATED" : "REWARD_UPDATED", "reward", rewardId, in.name() + " (" + in.pointCost() + " pts)");
    return repo.reward(rewardId).orElseThrow();
  }


}
