package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Validate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.lutiano.api.utils.Candidates;
import java.util.LinkedHashSet;
import co.elastic.clients.elasticsearch.core.search.HighlighterEncoder;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.lutiano.api.config.AppProperties;

/**
 * Text search across the indexed types. Elasticsearch only proposes candidates (most relevant first, at most
 * {@code verification-cap}); every candidate is then re-checked against MariaDB by its type's policy, and results, counts
 * and highlights come only from what passed. Counts are exact below the cap and reported as capped above it.
 */
@Service
public class SearchService {
  private static final Logger log = LoggerFactory.getLogger(SearchService.class);
  private final ElasticsearchClient es;
  private final SearchPolicies policies;
  private final ContactLookup contacts;
  private final int cap, maxQuery, candidateCap;

  public SearchService(ElasticsearchClient es, SearchPolicies policies, ContactLookup contacts,
                       AppProperties.Search search) {
    this.es = es; this.policies = policies; this.contacts = contacts; this.cap = search.verificationCap(); this.maxQuery = search.maxQueryLength(); this.candidateCap = search.candidateCap();
  }

  /** A trimmed query, or null when blank. Over-long queries are rejected. */
  public String clean(String q) {
    if (q == null || q.isBlank()) return null;
    q = q.trim();
    if (q.length() > maxQuery) throw Validate.bad("Search text is limited to " + maxQuery + " characters.");
    return q;
  }

  /** Types the caller asked for (all when empty); unknown names are rejected. */
  public List<String> types(Collection<String> requested) {
    if (requested == null || requested.isEmpty()) return policies.types().stream().sorted().toList();
    for (String t : requested) if (!policies.types().contains(t)) throw Validate.bad("Unknown search type: " + t);
    return List.copyOf(requested);
  }

  /** Verified results in relevance order, grouped and counted. */
  public PageResponse<Map<String, Object>> search(SearchPrincipal who, String q, List<String> types, PageRequest paging) {
    String text = clean(q);
    if (text == null) return PageResponse.<Map<String, Object>>slice(List.of(), paging).withSummary(Map.of("counts", Map.of(), "capped", false));
    List<String> wanted = types(types);
    Verified found = verified(who, text, wanted, cap, false);
    List<Map<String, Object>> verified = new ArrayList<>(found.rows());
    // exact phone/email matches (not indexed) go first, without duplicating what the index already found
    List<Map<String, Object>> exact = wanted.contains("STAFF") || wanted.contains("CUSTOMER") ? contacts.find(who, text) : List.of();
    exact = exact.stream().filter(e -> wanted.contains(e.get("type")) && verified.stream().noneMatch(v -> v.get("id").equals(e.get("id")))).toList();
    verified.addAll(0, exact);
    Map<String, Long> counts = verified.stream().collect(Collectors.groupingBy(r -> (String) r.get("type"), LinkedHashMap::new, Collectors.counting()));
    return PageResponse.slice(verified, paging).withSummary(Map.of("counts", counts, "capped", found.capped()));
  }

  /** A few best verified matches per type, for the header dropdown. */
  public List<Map<String, Object>> suggest(SearchPrincipal who, String q, int perType) {
    String text = clean(q);
    if (text == null) return List.of();
    List<Map<String, Object>> out = new ArrayList<>();
    Map<String, Integer> seen = new LinkedHashMap<>();
    for (Map<String, Object> r : verified(who, text, types(null), 40, true).rows())
      if (seen.merge((String) r.get("type"), 1, Integer::sum) <= perType) out.add(r);
    return out;
  }

  /**
   * For a list page's text box: the ids of {@code type} that match, most relevant first, up to the candidate cap. These are not
   * individually verified: the list applies its own permission scoping in MariaDB. Null when the text is blank.
   */
  public Candidates candidates(SearchPrincipal who, String type, String q) {
    String text = clean(q);
    if (text == null) return null;
    Query query = Query.of(x -> x.bool(b -> b.must(textQuery(text, false)).filter(policies.filter(type, who))
        .filter(f -> f.term(t -> t.field("type").value(type)))));
    try {
      var res = es.search(s -> s.index(SearchIndex.ALIAS).query(query).size(candidateCap).source(src -> src.fetch(false)).trackTotalHits(t -> t.enabled(false)), SearchDocument.class);
      String prefix = type + ":";
      Set<String> ids = new LinkedHashSet<>();
      for (Hit<SearchDocument> h : res.hits().hits()) ids.add(h.id().startsWith(prefix) ? h.id().substring(prefix.length()) : h.id());
      return new Candidates(text, ids, ids.size() >= candidateCap);
    } catch (Exception e) {
      throw unavailable(e);
    }
  }

  /** Search is a read path that must never take the caller down: log once, answer 503. */
  private static ResponseStatusException unavailable(Exception e) {
    log.warn("Search unavailable: {}", e.toString());
    return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Search is unavailable. Try again shortly.");
  }

  /** Verified rows in relevance order; {@code capped} when Elasticsearch returned as many candidates as asked for. */
  private record Verified(List<Map<String, Object>> rows, boolean capped) {}

  private Verified verified(SearchPrincipal who, String text, List<String> types, int size, boolean suggest) {
    Query query = Query.of(q -> q.bool(b -> b
        .must(textQuery(text, suggest))
        .filter(f -> f.bool(t -> { types.forEach(ty -> t.should(s -> s.bool(x -> x
            .filter(a -> a.term(m -> m.field("type").value(ty))).filter(policies.filter(ty, who))))); return t.minimumShouldMatch("1"); }))));
    SearchResponse<SearchDocument> res;
    try {
      res = es.search(s -> s.index(SearchIndex.ALIAS).query(query).size(size).trackTotalHits(t -> t.enabled(false))
          .source(src -> src.filter(f -> f.includes("type", "sourceId", "title", "subtitle", "reference")))
          .highlight(h -> h.preTags("<mark>").postTags("</mark>").encoder(HighlighterEncoder.Html).fields("title", f -> f.numberOfFragments(0)).fields("title.autocomplete", f -> f.numberOfFragments(0))), SearchDocument.class);
    } catch (Exception e) {
      throw unavailable(e);
    }
    List<Hit<SearchDocument>> hits = res.hits().hits();
    Map<String, List<String>> idsByType = new LinkedHashMap<>();
    for (Hit<SearchDocument> h : hits) idsByType.computeIfAbsent(h.source().type(), k -> new ArrayList<>()).add(h.source().sourceId());
    Map<String, Set<String>> allowed = new LinkedHashMap<>();
    idsByType.forEach((t, ids) -> allowed.put(t, policies.authorize(t, who, ids)));
    List<Map<String, Object>> out = new ArrayList<>();
    for (Hit<SearchDocument> h : hits) {
      SearchDocument d = h.source();
      if (!allowed.get(d.type()).contains(d.sourceId())) continue;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("type", d.type()); row.put("id", d.sourceId()); row.put("title", d.title()); row.put("subtitle", d.subtitle());
      row.put("reference", d.reference());
      List<String> hl = h.highlight().get("title");
      if (hl == null || hl.isEmpty()) hl = h.highlight().get("title.autocomplete");
      row.put("highlight", hl == null || hl.isEmpty() ? null : hl.get(0));
      out.add(row);
    }
    return new Verified(out, hits.size() >= size);
  }

  /** Words in title and content (typo-tolerant), as-you-type prefixes, and exact boosts for references, SKUs and codes. */
  private static Query textQuery(String text, boolean suggest) {
    String ref = text.toLowerCase();
    return Query.of(q -> q.bool(b -> {
      b.should(s -> s.multiMatch(m -> m.query(text).fields("title.autocomplete^3").operator(Operator.And)));
      b.should(s -> s.term(t -> t.field("reference").value(ref).boost(10f)));
      b.should(s -> s.prefix(t -> t.field("reference").value(ref).boost(4f)));
      b.should(s -> s.term(t -> t.field("tags").value(text).boost(8f)));
      if (!suggest) {
        // fuzziness only on words, never on references or codes
        b.should(s -> s.multiMatch(m -> m.query(text).fields("title^4", "title.stem^1.5", "subtitle", "content", "content.stem^0.5")
            .fuzziness("AUTO").prefixLength(1)));
      }
      return b.minimumShouldMatch("1");
    }));
  }
}
