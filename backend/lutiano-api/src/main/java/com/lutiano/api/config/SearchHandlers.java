package com.lutiano.api.config;

import static com.lutiano.api.service.search.QueryHandler.iso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.lutiano.api.service.notification.NotificationCatalog;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.lutiano.api.service.search.QueryHandler;
import com.lutiano.api.service.search.SearchDocument;
import com.lutiano.api.service.search.SearchHandler;
import com.lutiano.api.security.Roles;

/**
 * What goes into the index for each type. Allow-list only: names, descriptions, codes and the few filter fields
 * listed in docs/search/. Prices, phone numbers, emails and anything secret are deliberately left out.
 */
@Configuration
public class SearchHandlers {
  private static final List<String> ALL_STAFF = Roles.ALL.stream().sorted().toList();

  private static String status(Object active) { return Boolean.TRUE.equals(active) || Integer.valueOf(1).equals(active) ? "ACTIVE" : "INACTIVE"; }

  private static Map<String, Object> meta(Object... kv) {
    Map<String, Object> m = new HashMap<>();
    for (int i = 0; i < kv.length; i += 2) if (kv[i + 1] != null) m.put((String) kv[i], kv[i + 1]);
    return m;
  }

  private static SearchDocument.Perm perm(Object active, List<String> operationIds) {
    String s = status(active);
    return new SearchDocument.Perm(ALL_STAFF, null, null, operationIds, "ACTIVE".equals(s), s);
  }

  /** Simple and parent products. A variant is searched through its parent: its name and SKU are part of the parent's document. */
  @Bean public SearchHandler productHandler(JdbcTemplate db) {
    return new QueryHandler(db, "PRODUCT", "product",
        "select id, kind, sku, name, category, description, active, created_at, updated_at from product where id = ? and kind <> 'VARIANT'",
        r -> {
          String id = (String) r.get("id");
          List<Map<String, Object>> variants = db.queryForList("select variant_name, sku from product where parent_id = ? and active = true", id);
          List<String> skus = new ArrayList<>(), names = new ArrayList<>();
          for (Map<String, Object> v : variants) { skus.add((String) v.get("sku")); if (v.get("variant_name") != null) names.add((String) v.get("variant_name")); }
          String description = r.get("description") == null ? "" : (String) r.get("description");
          String content = (description + " " + String.join(" ", names)).trim();
          return new SearchDocument("PRODUCT", id, "CATALOGUE", (String) r.get("name"), (String) r.get("category"),
              content.isEmpty() ? null : content, (String) r.get("sku"), skus,
              meta("status", status(r.get("active")), "category", r.get("category"), "kind", r.get("kind"),
                  "createdAt", iso(r.get("created_at")), "updatedAt", iso(r.get("updated_at"))),
              perm(r.get("active"), List.of()), null);
        });
  }

  @Bean public SearchHandler categoryHandler(JdbcTemplate db) {
    return new QueryHandler(db, "CATEGORY", "product_category", "select id, name, description, sort_order, active from product_category where id = ?",
        r -> new SearchDocument("CATEGORY", (String) r.get("id"), "CATALOGUE", (String) r.get("name"), null, (String) r.get("description"), null, List.of(),
            meta("status", status(r.get("active")), "sortOrder", r.get("sort_order")), perm(r.get("active"), List.of()), null));
  }

  @Bean public SearchHandler addonHandler(JdbcTemplate db) {
    return new QueryHandler(db, "ADDON", "addon", "select id, name, sort_order, active from addon where id = ?",
        r -> new SearchDocument("ADDON", (String) r.get("id"), "CATALOGUE", (String) r.get("name"), null, null, null, List.of(),
            meta("status", status(r.get("active")), "sortOrder", r.get("sort_order")), perm(r.get("active"), List.of()), null));
  }

  @Bean public SearchHandler operationHandler(JdbcTemplate db) {
    return new QueryHandler(db, "OPERATION", "operation",
        "select id, name, code, type, address, description, city, region, active, created_at from operation where id = ?",
        r -> {
          String id = (String) r.get("id");
          String content = join(r.get("description"), r.get("city"), r.get("region"));
          return new SearchDocument("OPERATION", id, "LOCATIONS", (String) r.get("name"), (String) r.get("address"),
              content.isEmpty() ? null : content, (String) r.get("code"), List.of(),
              meta("status", status(r.get("active")), "locationType", r.get("type"), "city", r.get("city"), "region", r.get("region"),
                  "operationId", id, "createdAt", iso(r.get("created_at"))),
              perm(r.get("active"), List.of(id)), null);
        });
  }

  private static SearchDocument.Perm adminOnly(String status) { return new SearchDocument.Perm(List.of("ADMIN"), null, null, List.of(), false, status); }

  /** Sales: who, where and what was sold. Amounts, the customer's phone and payment details are never indexed. */
  @Bean public SearchHandler saleHandler(JdbcTemplate db) {
    return new QueryHandler(db, "SALE", "sale", """
        select s.id, s.reference, s.seller_id, s.operation_id, s.customer_id, s.payment_method, s.created_at,
               u.full_name seller_name, o.name operation_name, c.full_name customer_name
          from sale s join staff_user u on u.id = s.seller_id join operation o on o.id = s.operation_id
          left join customer c on c.id = s.customer_id where s.id = ?""",
        r -> {
          String id = (String) r.get("id");
          List<String> products = db.queryForList("select distinct product_name from sale_line where sale_id = ?", String.class, id);
          String content = join(r.get("seller_name"), r.get("customer_name"), String.join(" ", products));
          return new SearchDocument("SALE", id, "SALES", (String) r.get("reference"), (String) r.get("operation_name"), content, (String) r.get("reference"), List.of(),
              meta("operationId", r.get("operation_id"), "sellerId", r.get("seller_id"), "customerId", r.get("customer_id"),
                  "method", r.get("payment_method"), "createdAt", iso(r.get("created_at"))),
              new SearchDocument.Perm(ALL_STAFF, (String) r.get("seller_id"), (String) r.get("customer_id"), List.of((String) r.get("operation_id")), false, "ACTIVE"), null);
        });
  }

  @Bean public SearchHandler paymentHandler(JdbcTemplate db) {
    return new QueryHandler(db, "PAYMENT", "payment", """
        select p.id, p.reference, p.method, p.status, p.created_at, s.reference sale_reference, o.name operation_name, u.full_name seller_name
          from payment p join sale s on s.id = p.sale_id join operation o on o.id = p.operation_id join staff_user u on u.id = p.seller_id where p.id = ?""",
        r -> new SearchDocument("PAYMENT", (String) r.get("id"), "PAYMENTS", (String) r.get("reference"), (String) r.get("sale_reference"),
            (r.get("operation_name") + " " + r.get("seller_name")), (String) r.get("reference"), List.of((String) r.get("sale_reference")),
            meta("status", r.get("status"), "method", r.get("method"), "createdAt", iso(r.get("created_at"))), adminOnly((String) r.get("status")), null));
  }

  @Bean public SearchHandler refundHandler(JdbcTemplate db) {
    return new QueryHandler(db, "REFUND", "refund", """
        select f.id, f.reference, f.reason, f.note, f.status, f.requested_at, s.reference sale_reference
          from refund f join sale s on s.id = f.sale_id where f.id = ?""",
        r -> new SearchDocument("REFUND", (String) r.get("id"), "REFUNDS", (String) r.get("reference"), (String) r.get("sale_reference"),
            (r.get("reason") + (r.get("note") == null ? "" : " " + r.get("note"))), (String) r.get("reference"), List.of((String) r.get("sale_reference")),
            meta("status", r.get("status"), "createdAt", iso(r.get("requested_at"))), adminOnly((String) r.get("status")), null));
  }

  /** Staff: name, area and address only. Email, phone and credentials are never indexed. */
  @Bean public SearchHandler staffHandler(JdbcTemplate db) {
    return new QueryHandler(db, "STAFF", "staff_user", "select id, full_name, role, area, address, active, created_at from staff_user where id = ?",
        r -> new SearchDocument("STAFF", (String) r.get("id"), "PEOPLE", (String) r.get("full_name"), (String) r.get("area"), (String) r.get("address"), null, List.of(),
            meta("role", r.get("role"), "status", status(r.get("active")), "createdAt", iso(r.get("created_at"))), adminOnly(status(r.get("active"))), null));
  }

  /** Customers: name, tier and loyalty code. The phone number is found by exact lookup, never indexed. */
  @Bean public SearchHandler customerHandler(JdbcTemplate db) {
    return new QueryHandler(db, "CUSTOMER", "customer", """
        select c.id, c.code, c.full_name, c.status, c.tier_code, c.created_at, t.name tier_name
          from customer c left join loyalty_tier t on t.code = c.tier_code where c.id = ?""",
        r -> new SearchDocument("CUSTOMER", (String) r.get("id"), "CUSTOMERS", (String) r.get("full_name"), (String) r.get("tier_name"), null, (String) r.get("code"), List.of(),
            meta("status", r.get("status"), "tier", r.get("tier_code"), "createdAt", iso(r.get("created_at"))), adminOnly((String) r.get("status")), null));
  }

  private static String join(Object... parts) {
    return Arrays.stream(parts).filter(Objects::nonNull).map(Object::toString).filter(x -> !x.isBlank()).collect(Collectors.joining(" "));
  }

  private static String orNull(String s) { return s.isEmpty() ? null : s; }

  /** Payouts: seller, period, the account holder's name and the note. The phone number and amount are never indexed. */
  @Bean public SearchHandler payoutHandler(JdbcTemplate db) {
    return new QueryHandler(db, "PAYOUT", "payout", """
        select p.id, p.reference, p.method, p.status, p.requested_at, p.seller_id, p.note, p.destination_name, u.full_name seller_name,
               concat(e.starts_on, ' ', e.ends_on) period
          from payout p join staff_user u on u.id = p.seller_id left join earning_period e on e.id = p.period_id where p.id = ?""",
        r -> {
          SearchDocument.Perm perm = new SearchDocument.Perm(List.of("ADMIN"), (String) r.get("seller_id"), null, List.of(), false, (String) r.get("status"));
          return new SearchDocument("PAYOUT", (String) r.get("id"), "PAYOUTS", (String) r.get("reference"), (String) r.get("seller_name"),
              orNull(join(r.get("seller_name"), r.get("period"), r.get("destination_name"), r.get("note"))), (String) r.get("reference"), List.of(),
              meta("status", r.get("status"), "method", r.get("method"), "sellerId", r.get("seller_id"), "createdAt", iso(r.get("requested_at"))), perm, null);
        });
  }

  @Bean public SearchHandler payoutDestinationHandler(JdbcTemplate db) {
    return new QueryHandler(db, "PAYOUT_DESTINATION", "payout_destination", """
        select d.id, d.provider, d.account_name, d.status, d.created_at, d.seller_id, u.full_name seller_name
          from payout_destination d join staff_user u on u.id = d.seller_id where d.id = ?""",
        r -> new SearchDocument("PAYOUT_DESTINATION", (String) r.get("id"), "PAYOUTS", (String) r.get("account_name"), (String) r.get("seller_name"), (String) r.get("provider"), null, List.of(),
            meta("status", r.get("status"), "provider", r.get("provider"), "sellerId", r.get("seller_id"), "createdAt", iso(r.get("created_at"))),
            new SearchDocument.Perm(List.of("ADMIN"), (String) r.get("seller_id"), null, List.of(), false, (String) r.get("status")), null));
  }

  @Bean public SearchHandler targetHandler(JdbcTemplate db) {
    return new QueryHandler(db, "TARGET", "seller_target", """
        select t.id, t.name, t.status, t.created_at, t.seller_id, t.product_id, u.full_name seller_name, p.name product_name
          from seller_target t join staff_user u on u.id = t.seller_id left join product p on p.id = t.product_id where t.id = ?""",
        r -> new SearchDocument("TARGET", (String) r.get("id"), "TARGETS", (String) r.get("name"), (String) r.get("seller_name"), (String) r.get("product_name"), null, List.of(),
            meta("status", r.get("status"), "sellerId", r.get("seller_id"), "productId", r.get("product_id"), "createdAt", iso(r.get("created_at"))),
            new SearchDocument.Perm(List.of("ADMIN"), (String) r.get("seller_id"), null, List.of(), false, (String) r.get("status")), null));
  }

  /** Cash records belong to a seller: administrators see all, a seller only their own. */
  private static SearchDocument.Perm cashPerm(Map<String, Object> r) {
    return new SearchDocument.Perm(List.of("ADMIN", "SELLER"), (String) r.get("seller_id"), null, List.of(), false, (String) r.get("status"));
  }

  @Bean public SearchHandler cashOutflowHandler(JdbcTemplate db) {
    return new QueryHandler(db, "CASH_OUTFLOW", "cash_outflow", """
        select o.id, o.reference, o.category, o.description, o.status, o.requested_at, o.seller_id, u.full_name seller_name
          from cash_outflow o join staff_user u on u.id = o.seller_id where o.id = ?""",
        r -> new SearchDocument("CASH_OUTFLOW", (String) r.get("id"), "CASH", (String) r.get("reference"), (String) r.get("seller_name"),
            join(r.get("category"), r.get("description")), (String) r.get("reference"), List.of(),
            meta("status", r.get("status"), "category", r.get("category"), "sellerId", r.get("seller_id"), "createdAt", iso(r.get("requested_at"))), cashPerm(r), null));
  }

  @Bean public SearchHandler cashRemittanceHandler(JdbcTemplate db) {
    return new QueryHandler(db, "CASH_REMITTANCE", "cash_remittance", """
        select m.id, m.reference, m.method, m.note, m.status, m.recorded_at, m.seller_id, u.full_name seller_name
          from cash_remittance m join staff_user u on u.id = m.seller_id where m.id = ?""",
        r -> new SearchDocument("CASH_REMITTANCE", (String) r.get("id"), "CASH", (String) r.get("reference"), (String) r.get("seller_name"),
            orNull(join(r.get("note"))), (String) r.get("reference"), List.of(),
            meta("status", r.get("status"), "method", r.get("method"), "sellerId", r.get("seller_id"), "createdAt", iso(r.get("recorded_at"))), cashPerm(r), null));
  }

  /** Stock records: back-office staff see all, a seller those at their assigned locations. */
  private static SearchDocument.Perm stockPerm(Map<String, Object> r) {
    Object op = r.get("operation_id");
    return new SearchDocument.Perm(List.of("ADMIN", "INVENTORY_MANAGER", "SELLER"), null, null, op == null ? List.of() : List.of((String) op), false, (String) r.get("status"));
  }

  @Bean public SearchHandler stocktakeHandler(JdbcTemplate db) {
    return new QueryHandler(db, "STOCKTAKE", "stocktake", """
        select s.id, s.operation_id, s.status, s.note, s.created_at, o.name operation_name
          from stocktake s left join operation o on o.id = s.operation_id where s.id = ?""",
        r -> new SearchDocument("STOCKTAKE", (String) r.get("id"), "INVENTORY", "Stocktake " + (r.get("operation_name") == null ? "central stock" : r.get("operation_name")),
            (String) r.get("status"), (String) r.get("note"), null, List.of(),
            meta("status", r.get("status"), "operationId", r.get("operation_id"), "createdAt", iso(r.get("created_at"))), stockPerm(r), null));
  }

  @Bean public SearchHandler adjustmentHandler(JdbcTemplate db) {
    return new QueryHandler(db, "ADJUSTMENT", "stock_adjustment", """
        select a.id, a.operation_id, a.reason, a.note, a.status, a.created_at, p.name product_name, o.name operation_name
          from stock_adjustment a join product p on p.id = a.product_id left join operation o on o.id = a.operation_id where a.id = ?""",
        r -> new SearchDocument("ADJUSTMENT", (String) r.get("id"), "INVENTORY", (String) r.get("product_name"), (String) r.get("operation_name"),
            join(r.get("reason"), r.get("note")), null, List.of(),
            meta("status", r.get("status"), "operationId", r.get("operation_id"), "createdAt", iso(r.get("created_at"))), stockPerm(r), null));
  }

  @Bean public SearchHandler rewardHandler(JdbcTemplate db) {
    return new QueryHandler(db, "REWARD", "reward", "select id, name, reward_type, description, active, created_at from reward where id = ?",
        r -> new SearchDocument("REWARD", (String) r.get("id"), "LOYALTY", (String) r.get("name"), null, (String) r.get("description"), null, List.of(),
            meta("status", status(r.get("active")), "kind", r.get("reward_type"), "createdAt", iso(r.get("created_at"))), perm(r.get("active"), List.of()), null));
  }

  @Bean public SearchHandler earningRuleHandler(JdbcTemplate db) {
    return new QueryHandler(db, "EARNING_RULE", "earning_rule", "select id, name, method, category, active, created_at from earning_rule where id = ?",
        r -> new SearchDocument("EARNING_RULE", (String) r.get("id"), "LOYALTY", (String) r.get("name"), (String) r.get("category"), null, null, List.of(),
            meta("status", status(r.get("active")), "method", r.get("method"), "createdAt", iso(r.get("created_at"))), adminOnly(status(r.get("active"))), null));
  }

  @Bean public SearchHandler redemptionHandler(JdbcTemplate db) {
    return new QueryHandler(db, "REDEMPTION", "reward_redemption", """
        select r.id, r.reference, r.reward_name, r.status, r.created_at, r.customer_id, c.full_name customer_name
          from reward_redemption r join customer c on c.id = r.customer_id where r.id = ?""",
        r -> new SearchDocument("REDEMPTION", (String) r.get("id"), "LOYALTY", (String) r.get("reference"), (String) r.get("reward_name"), (String) r.get("customer_name"),
            (String) r.get("reference"), List.of(), meta("status", r.get("status"), "customerId", r.get("customer_id"), "createdAt", iso(r.get("created_at"))),
            new SearchDocument.Perm(List.of("ADMIN"), null, (String) r.get("customer_id"), List.of(), false, (String) r.get("status")), null));
  }

  @Bean public SearchHandler pointsAdjustmentHandler(JdbcTemplate db) {
    return new QueryHandler(db, "POINTS_ADJUSTMENT", "points_adjustment", """
        select a.id, a.reference, a.reason, a.note, a.created_at, a.customer_id, c.full_name customer_name
          from points_adjustment a join customer c on c.id = a.customer_id where a.id = ?""",
        r -> new SearchDocument("POINTS_ADJUSTMENT", (String) r.get("id"), "LOYALTY", (String) r.get("reference"), (String) r.get("customer_name"),
            join(r.get("reason"), r.get("note")), (String) r.get("reference"), List.of(),
            meta("customerId", r.get("customer_id"), "createdAt", iso(r.get("created_at"))),
            new SearchDocument.Perm(List.of("ADMIN"), null, (String) r.get("customer_id"), List.of(), false, "ACTIVE"), null));
  }

  /** A transfer of stock to a location: back-office staff see all, a seller those to their assigned locations. */
  @Bean public SearchHandler transferHandler(JdbcTemplate db) {
    return new QueryHandler(db, "TRANSFER", "stock_allocation", """
        select a.id, a.operation_id, a.status, a.request_note, a.decision_note, a.created_at, p.name product_name, o.name operation_name
          from stock_allocation a join product p on p.id = a.product_id join operation o on o.id = a.operation_id where a.id = ?""",
        r -> new SearchDocument("TRANSFER", (String) r.get("id"), "INVENTORY", (String) r.get("product_name"), (String) r.get("operation_name"),
            orNull(join(r.get("request_note"), r.get("decision_note"))), null, List.of(),
            meta("status", r.get("status"), "operationId", r.get("operation_id"), "createdAt", iso(r.get("created_at"))), stockPerm(r), null));
  }

  /** Quantity differences on a transfer: back-office only (the table has no location column to scope a seller by). */
  @Bean public SearchHandler discrepancyHandler(JdbcTemplate db) {
    return new QueryHandler(db, "DISCREPANCY", "stock_discrepancy", """
        select d.id, d.note, d.created_at, p.name product_name, o.name operation_name
          from stock_discrepancy d join stock_allocation a on a.id = d.allocation_id join product p on p.id = a.product_id
          join operation o on o.id = a.operation_id where d.id = ?""",
        r -> new SearchDocument("DISCREPANCY", (String) r.get("id"), "INVENTORY", (String) r.get("product_name"), (String) r.get("operation_name"),
            (String) r.get("note"), null, List.of(), meta("createdAt", iso(r.get("created_at"))),
            new SearchDocument.Perm(List.of("ADMIN", "INVENTORY_MANAGER"), null, null, List.of(), false, "ACTIVE"), null));
  }

  @Bean public SearchHandler commissionRuleHandler(JdbcTemplate db) {
    return new QueryHandler(db, "COMMISSION_RULE", "commission_rule", """
        select c.id, c.seller_role, c.note, c.created_at, c.superseded_at, p.name product_name
          from commission_rule c join product p on p.id = c.product_id where c.id = ?""",
        r -> new SearchDocument("COMMISSION_RULE", (String) r.get("id"), "COMMISSIONS", (String) r.get("product_name"), (String) r.get("seller_role"),
            (String) r.get("note"), null, List.of(), meta("status", r.get("superseded_at") == null ? "ACTIVE" : "INACTIVE", "role", r.get("seller_role"), "createdAt", iso(r.get("created_at"))),
            adminOnly(r.get("superseded_at") == null ? "ACTIVE" : "INACTIVE"), null));
  }

  /** SMS delivery log. Messages that carry a one-time code are never indexed, whatever their stored text. */
  @Bean public SearchHandler deliveryHandler(JdbcTemplate db) {
    return new QueryHandler(db, "DELIVERY", "notification_delivery",
        "select id, event_key, recipient_name, body, status, created_at from notification_delivery where id = ?",
        r -> {
          String key = (String) r.get("event_key");
          if (NotificationCatalog.exists(key) && NotificationCatalog.get(key).oneTimeCode()) return null;
          String name = NotificationCatalog.exists(key) ? NotificationCatalog.get(key).name() : key;
          return new SearchDocument("DELIVERY", (String) r.get("id"), "NOTIFICATIONS", name, (String) r.get("recipient_name"), (String) r.get("body"), null, List.of(),
              meta("status", r.get("status"), "event", key, "createdAt", iso(r.get("created_at"))), adminOnly((String) r.get("status")), null);
        });
  }
}
