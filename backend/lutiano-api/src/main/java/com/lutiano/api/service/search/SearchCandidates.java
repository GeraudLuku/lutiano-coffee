package com.lutiano.api.service.search;

import com.lutiano.api.utils.Candidates;
import com.lutiano.api.security.Actor;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** What list services call for their text box: resolves the caller and asks the search service for candidate ids. */
@Component
public class SearchCandidates {
  private final SearchService search;
  private final JdbcTemplate db;

  public SearchCandidates(SearchService search, JdbcTemplate db) { this.search = search; this.db = db; }

  /** The signed-in staff member as a search principal; sellers carry their assigned locations. */
  public SearchPrincipal principal(Actor actor) {
    List<String> ops = actor.isSeller() ? db.queryForList("select operation_id from seller_operation where seller_id=?", String.class, actor.id()) : List.of();
    return SearchPrincipal.staff(actor, ops);
  }

  /** Candidates for {@code q} among {@code type} records, or null when q is blank. Throws 503 when search is down. */
  public Candidates find(Actor actor, String type, String q) { return search.candidates(principal(actor), type, q); }
}
