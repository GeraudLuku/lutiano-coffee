package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Policy for a type backed by one table with an {@code active} flag (products, categories, add-ons, sales locations):
 * staff managers see everything, sellers and the public see active records, and, when {@code assignedOnly}, a seller
 * sees only the locations assigned to them.
 */
public class TablePolicy implements SearchPolicy {
  private final JdbcTemplate db;
  private final String type, table;
  private final boolean assignedOnly;

  /** @param assignedOnly sellers see only rows whose id is in their {@code seller_operation} assignments */
  public TablePolicy(JdbcTemplate db, String type, String table, boolean assignedOnly) {
    this.db = db; this.type = type; this.table = table; this.assignedOnly = assignedOnly;
  }

  @Override public String type() { return type; }

  @Override public Query filter(SearchPrincipal who) {
    if (who.isAdmin() || who.isInventoryManager()) return Query.of(q -> q.matchAll(m -> m));
    if (who.isSeller()) {
      if (assignedOnly) {
        if (who.operationIds().isEmpty()) return Query.of(q -> q.matchNone(m -> m));
        List<FieldValue> ids = who.operationIds().stream().map(FieldValue::of).toList();
        return Query.of(q -> q.bool(b -> b.filter(f -> f.term(t -> t.field("perm.status").value("ACTIVE")))
            .filter(f -> f.terms(t -> t.field("perm.operationIds").terms(v -> v.value(ids))))));
      }
      return Query.of(q -> q.term(t -> t.field("perm.status").value("ACTIVE")));
    }
    if (who.isCustomer()) return Query.of(q -> q.term(t -> t.field("perm.public").value(true)));
    return Query.of(q -> q.matchNone(m -> m));
  }

  @Override public Set<String> authorize(SearchPrincipal who, Collection<String> ids) {
    if (ids.isEmpty()) return Set.of();
    if (who.isAdmin() || who.isInventoryManager()) return SearchPolicy.idsWhere(db, table, "1=1", ids);
    if (who.isSeller()) {
      return assignedOnly
          ? SearchPolicy.idsWhere(db, table, "active = true and id in (select operation_id from seller_operation where seller_id = ?)", ids, who.id())
          : SearchPolicy.idsWhere(db, table, "active = true", ids);
    }
    return who.isCustomer() ? SearchPolicy.idsWhere(db, table, "active = true", ids) : Set.of();
  }
}
