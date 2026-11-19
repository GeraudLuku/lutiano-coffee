package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.Collection;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * What one type of document a caller may see. {@link #filter} only narrows Elasticsearch candidates (it can lag behind
 * a permission change); {@link #authorize} is the authority: it re-checks the candidates in MariaDB, so a revoked
 * assignment or a deactivated record disappears immediately, before the outbox has caught up.
 */
public interface SearchPolicy {
  String type();

  /** Elasticsearch filter for this caller. A match-nothing query when the caller may not see this type at all. */
  Query filter(SearchPrincipal who);

  /** The subset of {@code ids} the caller may see right now. One batched query, never one per result. */
  Set<String> authorize(SearchPrincipal who, Collection<String> ids);

  /** The ids (of {@code ids}) in {@code table} that also satisfy {@code rule}; {@code ruleArgs} fill the rule's placeholders. Table and rule are constants, never caller input. */
  static Set<String> idsWhere(JdbcTemplate db, String table, String rule, Collection<String> ids, Object... ruleArgs) {
    List<Object> args = new ArrayList<>(ids);
    args.addAll(List.of(ruleArgs));
    String marks = String.join(",", Collections.nCopies(ids.size(), "?"));
    return new HashSet<>(db.queryForList("select id from " + table + " where id in (" + marks + ") and " + rule, String.class, args.toArray()));
  }
}
