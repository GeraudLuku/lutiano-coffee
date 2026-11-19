package com.lutiano.api.service.product;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.repository.earnings.CommissionRepository;
import com.lutiano.api.service.loyalty.EarningCalculator;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.SellerRole;
import com.lutiano.api.entity.EarningLine;

/** What a product earns and how it sells: loyalty points and commission per sellable item, and its sales over a period. */
@Service
public class ProductInsights {
  private static final List<String> SELLER_ROLES = Enums.list(SellerRole.class);

  private final ProductService products;
  private final ProductRepository repo;
  private final CommissionRepository commissions;
  private final LoyaltyRepository loyalty;
  private final EarningCalculator calculator;

  public ProductInsights(ProductService products, ProductRepository repo, CommissionRepository commissions, LoyaltyRepository loyalty, EarningCalculator calculator) {
    this.products = products; this.repo = repo; this.commissions = commissions; this.loyalty = loyalty; this.calculator = calculator;
  }

  /**
   * For each sellable item of the product (itself, or each variant of a parent): the points one unit earns under today's
   * loyalty rules (before any tier multiplier) and the commission rule of each seller role.
   */
  public List<Map<String, Object>> rewards(String id) {
    Map<String, Object> p = products.detail(id);
    List<Map<String, Object>> items = "PARENT".equals(p.get("kind")) ? castList(p.get("variants")) : List.of(p);
    Map<String, Object> settings = loyalty.settings();
    List<Map<String, Object>> rules = loyalty.activeRules(Periods.today());
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> item : items) {
      String itemId = (String) item.get("id"), parentId = (String) item.get("parent_id");
      boolean eligible = Rows.truthy(item.get("loyalty_eligible"));
      long price = Rows.asLong(item.get("price_xaf"));
      int points = eligible ? calculator.calculate(List.of(new EarningLine(itemId, parentId, (String) item.get("name"), (String) item.get("category"), 1, price, true,
          Rows.truthy(item.get("promotion_eligible")))), settings, rules, 100, false).totalPoints() : 0;
      List<Map<String, Object>> commission = new ArrayList<>();
      for (String role : SELLER_ROLES) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("seller_role", role);
        commissions.ruleFor(itemId, parentId, role).ifPresent(r -> { row.put("basis", r.get("basis")); row.put("rate", r.get("rate")); });
        commission.add(row);
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("product_id", itemId); row.put("name", item.get("name")); row.put("variant_name", item.get("variant_name")); row.put("sku", item.get("sku"));
      row.put("price_xaf", price); row.put("loyalty_eligible", eligible); row.put("loyalty_points", points); row.put("commission", commission);
      out.add(row);
    }
    return out;
  }

  /** Units and revenue over the last {@code days} days (Douala days, ending today). */
  public Map<String, Object> performance(String id, Integer days) {
    products.get(id);
    int n = Math.max(1, Math.min(days == null ? 30 : days, 365));
    LocalDate last = Periods.today(), first = last.minusDays(n - 1L);
    Map<String, Object> out = new LinkedHashMap<>(repo.performance(id, Periods.startOf(first), Periods.endOf(last)));
    Map<String, Map<String, Object>> byDay = new LinkedHashMap<>();
    castList(out.get("byDay")).forEach(r -> byDay.put(r.get("d").toString(), r));
    List<Map<String, Object>> series = new ArrayList<>();
    for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
      Map<String, Object> r = byDay.get(d.toString());
      series.add(Map.of("date", d.toString(), "units", r == null ? 0 : r.get("units"), "revenueXaf", r == null ? 0 : r.get("revenue_xaf")));
    }
    out.put("byDay", series);
    out.put("days", n);
    return out;
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> castList(Object o) { return (List<Map<String, Object>>) o; }
}
