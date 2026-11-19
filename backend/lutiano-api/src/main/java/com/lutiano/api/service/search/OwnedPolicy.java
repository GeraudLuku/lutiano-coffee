package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import co.elastic.clients.elasticsearch._types.FieldValue;

/**
 * Policy for records that belong to a seller (sales) or are back-office only (payments, refunds, staff, customers):
 * administrators see all, inventory managers only when {@code inventoryManagers}, and a seller only the rows that
 * {@code sellerRule} (SQL with one ? for the seller's id) allows, when a rule is given: their own rows, or (by location) the
 * rows at their assigned locations. Being assigned to the same location grants nothing.
 * The public and customers see none of these (customer self-service search is not specified yet).
 */
public class OwnedPolicy implements SearchPolicy {
  private final JdbcTemplate db;
  private final String type, table, sellerRule;
  private final boolean inventoryManagers, byLocation;

  /** Own rows: {@code sellerColumn} names the column holding the seller's id. */
  public OwnedPolicy(JdbcTemplate db, String type, String table, boolean inventoryManagers, String sellerColumn) {
    this(db, type, table, inventoryManagers, sellerColumn == null ? null : sellerColumn + " = ?", false);
  }

  /** Rows at the seller's assigned locations (stock records): {@code operation_id} must be in their assignments. */
  public static OwnedPolicy atAssignedLocations(JdbcTemplate db, String type, String table) {
    return new OwnedPolicy(db, type, table, true, "operation_id in (select operation_id from seller_operation where seller_id = ?)", true);
  }

  private OwnedPolicy(JdbcTemplate db, String type, String table, boolean inventoryManagers, String sellerRule, boolean byLocation) {
    this.db = db; this.type = type; this.table = table; this.inventoryManagers = inventoryManagers; this.sellerRule = sellerRule; this.byLocation = byLocation;
  }

  @Override public String type() { return type; }

  @Override public Query filter(SearchPrincipal who) {
    if (who.isAdmin() || (who.isInventoryManager() && inventoryManagers)) return Query.of(q -> q.matchAll(m -> m));
    if (who.isSeller() && sellerRule != null) {
      if (!byLocation) return Query.of(q -> q.term(t -> t.field("perm.sellerId").value(who.id())));
      if (who.operationIds().isEmpty()) return Query.of(q -> q.matchNone(m -> m));
      List<FieldValue> ops = who.operationIds().stream().map(FieldValue::of).toList();
      return Query.of(q -> q.terms(t -> t.field("perm.operationIds").terms(v -> v.value(ops))));
    }
    return Query.of(q -> q.matchNone(m -> m));
  }

  @Override public Set<String> authorize(SearchPrincipal who, Collection<String> ids) {
    if (ids.isEmpty()) return Set.of();
    if (who.isAdmin() || (who.isInventoryManager() && inventoryManagers)) return SearchPolicy.idsWhere(db, table, "1=1", ids);
    if (who.isSeller() && sellerRule != null) return SearchPolicy.idsWhere(db, table, sellerRule, ids, who.id());
    return Set.of();
  }
}
