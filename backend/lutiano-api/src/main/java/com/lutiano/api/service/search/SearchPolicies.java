package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Registry of the per-type policies. A type with no policy is invisible to everyone (fail closed). */
@Component
public class SearchPolicies {
  private final Map<String, SearchPolicy> byType;

  public SearchPolicies(JdbcTemplate db) {
    byType = List.<SearchPolicy>of(
        new TablePolicy(db, "PRODUCT", "product", false),
        new TablePolicy(db, "CATEGORY", "product_category", false),
        new TablePolicy(db, "ADDON", "addon", false),
        new TablePolicy(db, "OPERATION", "operation", true),
        new OwnedPolicy(db, "SALE", "sale", true, "seller_id"),
        new OwnedPolicy(db, "PAYMENT", "payment", false, null),
        new OwnedPolicy(db, "REFUND", "refund", false, null),
        new OwnedPolicy(db, "STAFF", "staff_user", false, null),
        new OwnedPolicy(db, "CUSTOMER", "customer", false, null),
        new OwnedPolicy(db, "PAYOUT", "payout", false, null),
        new OwnedPolicy(db, "PAYOUT_DESTINATION", "payout_destination", false, null),
        new OwnedPolicy(db, "TARGET", "seller_target", false, null),
        new OwnedPolicy(db, "CASH_OUTFLOW", "cash_outflow", false, "seller_id"),
        new OwnedPolicy(db, "CASH_REMITTANCE", "cash_remittance", false, "seller_id"),
        OwnedPolicy.atAssignedLocations(db, "STOCKTAKE", "stocktake"),
        OwnedPolicy.atAssignedLocations(db, "ADJUSTMENT", "stock_adjustment"),
        new TablePolicy(db, "REWARD", "reward", false),
        new OwnedPolicy(db, "EARNING_RULE", "earning_rule", false, null),
        new OwnedPolicy(db, "REDEMPTION", "reward_redemption", false, null),
        new OwnedPolicy(db, "POINTS_ADJUSTMENT", "points_adjustment", false, null),
        OwnedPolicy.atAssignedLocations(db, "TRANSFER", "stock_allocation"),
        new OwnedPolicy(db, "DISCREPANCY", "stock_discrepancy", true, null),
        new OwnedPolicy(db, "COMMISSION_RULE", "commission_rule", false, null),
        new OwnedPolicy(db, "DELIVERY", "notification_delivery", false, null))
        .stream().collect(Collectors.toUnmodifiableMap(SearchPolicy::type, Function.identity()));
  }

  public Set<String> types() { return byType.keySet(); }

  /** The caller's Elasticsearch filter for {@code type}: match nothing for an unknown type. */
  public Query filter(String type, SearchPrincipal who) {
    SearchPolicy p = byType.get(type);
    return p == null ? Query.of(q -> q.matchNone(m -> m)) : p.filter(who);
  }

  /** The ids of {@code type} the caller may see right now (authoritative, from MariaDB). */
  public Set<String> authorize(String type, SearchPrincipal who, Collection<String> ids) {
    SearchPolicy p = byType.get(type);
    return p == null ? new HashSet<>() : p.authorize(who, ids);
  }
}
