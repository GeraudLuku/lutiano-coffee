package com.lutiano.api.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.lutiano.api.models.request.PageRequest;

/**
 * One page of results. {@code total} counts every row matching the filters; {@code summary} carries totals computed
 * over all of them (not just this page) for screens that show summary cards.
 */
public record PageResponse<T>(List<T> items, int page, int size, long total, int pages, Map<String, Object> summary,
                      @JsonInclude(Include.NON_DEFAULT) boolean truncated) {

  public static <T> PageResponse<T> of(List<T> items, PageRequest req, long total) { return of(items, req, total, null); }

  public static <T> PageResponse<T> of(List<T> items, PageRequest req, long total, Map<String, Object> summary) {
    return new PageResponse<>(items, req.page(), req.size(), total, (int) Math.max(1, (total + req.size() - 1) / req.size()), summary, false);
  }

  /** Pages an in-memory list (for lists assembled in code rather than by one query). */
  public static <T> PageResponse<T> slice(List<T> all, PageRequest req) {
    int from = Math.min(all.size(), req.offset()), to = Math.min(all.size(), from + req.size());
    return of(all.subList(from, to), req, all.size());
  }

  public <R> PageResponse<R> map(Function<T, R> f) { return new PageResponse<>(items.stream().map(f).toList(), page, size, total, pages, summary, truncated); }

  public PageResponse<T> withSummary(Map<String, Object> s) { return new PageResponse<>(items, page, size, total, pages, s, truncated); }

  /** True when a text search matched more records than the search service returns (the list shows "refine your search"). */
  public PageResponse<T> withTruncated(boolean t) { return new PageResponse<>(items, page, size, total, pages, summary, t); }
}
