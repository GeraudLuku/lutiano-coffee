package com.lutiano.api.controller.search;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.search.SearchCandidates;
import com.lutiano.api.service.search.SearchIndex;
import com.lutiano.api.service.search.SearchPolicies;
import com.lutiano.api.service.search.SearchPrincipal;
import com.lutiano.api.service.search.SearchRateLimiter;
import com.lutiano.api.service.search.SearchReindexer;
import com.lutiano.api.service.search.SearchService;
import com.lutiano.api.models.request.search.SearchRequest;
import com.lutiano.api.definitions.enums.SearchType;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.search.StatusResponse;
import com.lutiano.api.models.response.search.SuggestionResponse;
import com.lutiano.api.service.search.SearchIndexer;

@RestController
public class SearchController {
  private static final List<String> PUBLIC_TYPES = Stream.of(SearchType.PRODUCT, SearchType.CATEGORY, SearchType.ADDON, SearchType.OPERATION).map(Enum::name).toList();

  private final SearchService search;
  private final SearchRateLimiter limiter;
  private final CurrentUser current;
  private final SearchIndex index;
  private final SearchIndexer indexer;
  private final SearchReindexer reindexer;
  private final SearchPolicies policies;
  private final SearchCandidates candidates;

  public SearchController(SearchService search, SearchRateLimiter limiter, CurrentUser current, SearchIndex index,
                          SearchIndexer indexer, SearchReindexer reindexer, SearchPolicies policies, SearchCandidates candidates) {
    this.search = search; this.limiter = limiter; this.current = current; this.index = index;
    this.indexer = indexer; this.reindexer = reindexer; this.policies = policies; this.candidates = candidates;
  }


  private SearchPrincipal principal(Authentication a) { return candidates.principal(current.get(a)); }

  @PostMapping("/search")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> all(@RequestBody SearchRequest body, PageRequest paging, Authentication a) {
    SearchPrincipal who = principal(a);
    limiter.check("search", who.id(), 60);
    return Responses.ok(search.search(who, body.q(), body.types(), paging));
  }

  @PostMapping("/search/{type}")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> oneType(@PathVariable String type, @RequestBody SearchRequest body, PageRequest paging, Authentication a) {
    SearchPrincipal who = principal(a);
    limiter.check("search", who.id(), 60);
    return Responses.ok(search.search(who, body.q(), List.of(type.toUpperCase()), paging));
  }

  @GetMapping("/search/suggestions")
  public ResponseEntity<GenericApiResponse<List<SuggestionResponse>>> suggestions(@RequestParam String q, Authentication a) {
    SearchPrincipal who = principal(a);
    limiter.check("suggest", who.id(), 120);
    return Responses.list(search.suggest(who, q, 3), SuggestionResponse.class);
  }

  /** Public catalogue (no sign-in): active products, categories, add-ons and sales locations only. */
  @PostMapping("/catalogue/search")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> catalogue(@RequestBody SearchRequest body, PageRequest paging, HttpServletRequest req) {
    limiter.check("catalogue", req.getRemoteAddr(), 30);
    return Responses.ok(search.search(SearchPrincipal.publicCatalogue(), body.q(), PUBLIC_TYPES, paging));
  }

  // ----- administration -----

  @GetMapping("/admin/search/status")
  public ResponseEntity<GenericApiResponse<StatusResponse>> status(Authentication a) throws Exception {
    current.require(a, Roles.ADMIN);
    Map<String, Object> out = new LinkedHashMap<>();
    boolean up = index.available();
    out.put("available", up);
    out.put("outbox", indexer.outboxStatus());
    out.put("reindex", reindexer.progress());
    if (up) {
      out.put("index", index.current());
      out.put("documents", index.counts(policies.types()));
    }
    return Responses.ok(out, StatusResponse.class);
  }

  @PostMapping("/admin/search/reindex")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reindex(Authentication a) {
    current.require(a, Roles.ADMIN);
    if (reindexer.isRunning()) return Responses.ok(Map.of("started", false, "message", "A reindex is already running."));
    reindexer.start();
    return Responses.ok(Map.of("started", true));
  }

  @PostMapping("/admin/search/retry-failed")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> retryFailed(Authentication a) {
    current.require(a, Roles.ADMIN);
    return Responses.ok(Map.of("requeued", indexer.retryFailed()));
  }
}
