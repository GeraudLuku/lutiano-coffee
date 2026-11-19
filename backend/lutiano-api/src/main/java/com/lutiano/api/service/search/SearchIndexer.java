package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.VersionType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.config.AppProperties;

/**
 * Copies outbox changes into Elasticsearch in small bulk batches. Each document is rebuilt from current MariaDB state
 * and written with the outbox id as its external version ({@code external_gte}), so a stale or repeated change can never
 * overwrite newer data. Transient failures back off and retry; permanent ones (a rejected document) are marked FAILED.
 */
@Component
public class SearchIndexer {
  private static final Logger log = LoggerFactory.getLogger(SearchIndexer.class);

  private final JdbcTemplate db;
  private final ElasticsearchClient es;
  private final SearchOutbox outbox;
  private final Map<String, SearchHandler> handlers;
  private final boolean enabled;
  private final int batchSize, maxAttempts;

  public SearchIndexer(JdbcTemplate db, ElasticsearchClient es, SearchOutbox outbox, List<SearchHandler> handlers,
                       AppProperties.Search search) {
    this.db = db; this.es = es; this.outbox = outbox; this.enabled = search.indexingEnabled(); this.batchSize = search.batchSize(); this.maxAttempts = search.retryMaxAttempts();
    this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(SearchHandler::type, Function.identity()));
  }

  private volatile boolean paused;

  /** Holds the worker while a reindex switches the alias. */
  void pause(boolean p) { paused = p; }

  @Scheduled(fixedDelay = 2000, initialDelay = 5000)
  public void tick() {
    if (!enabled || paused) return;
    try { while (runBatch() == batchSize) { /* drain the backlog */ } } catch (Exception e) { log.warn("Search indexing paused: {}", e.getMessage()); }
  }

  @Scheduled(cron = "0 15 3 * * *", zone = "Africa/Douala")
  public void housekeeping() { outbox.purge(); }

  /** Processes up to one batch of due changes; returns how many rows it picked up. */
  public int runBatch() throws Exception {
    List<Map<String, Object>> rows = db.queryForList(
        "select id, type, source_id from search_outbox where status='PENDING' and next_attempt_at <= now() order by id limit ?", batchSize);
    if (rows.isEmpty()) return 0;
    BulkRequest.Builder bulk = new BulkRequest.Builder();
    List<Map<String, Object>> sent = new ArrayList<>();
    for (Map<String, Object> r : rows) {
      long rev = ((Number) r.get("id")).longValue();
      String type = (String) r.get("type"), id = (String) r.get("source_id");
      SearchHandler h = handlers.get(type);
      if (h == null) { settle(rev, "FAILED", "No handler for type " + type); continue; }
      Optional<SearchDocument> doc;
      try { doc = h.load(id); } catch (Exception e) { retry(rev, e.getMessage()); continue; }
      add(bulk, SearchIndex.ALIAS, type + ":" + id, doc, rev);
      sent.add(r);
    }
    if (sent.isEmpty()) return rows.size();
    BulkResponse res;
    try { res = es.bulk(bulk.build()); } catch (Exception e) { sent.forEach(r -> retry(rev(r), e.getMessage())); throw e; }
    List<BulkResponseItem> items = res.items();
    for (int i = 0; i < sent.size(); i++) {
      long rev = rev(sent.get(i));
      BulkResponseItem it = items.get(i);
      // 404: deleting what was never indexed; 409: a newer revision is already indexed
      if (it.error() == null || it.status() == 404 || it.status() == 409) settle(rev, "DONE", null);
      else if (it.status() == 400 || it.status() == 413) settle(rev, "FAILED", it.error().reason());   // permanent: bad document
      else retry(rev, it.error().reason());
    }
    return rows.size();
  }

  /** Adds one index-or-delete operation, versioned with the outbox revision so older data can never replace newer. */
  static void add(BulkRequest.Builder bulk, String index, String docId, Optional<SearchDocument> doc, long rev) {
    if (doc.isPresent()) {
      SearchDocument d = withTimestamp(doc.get());
      bulk.operations(o -> o.index(i -> i.index(index).id(docId).document(d).version(rev).versionType(VersionType.ExternalGte)));
    } else {
      bulk.operations(o -> o.delete(d -> d.index(index).id(docId).version(rev).versionType(VersionType.ExternalGte)));
    }
  }

  Map<String, SearchHandler> handlers() { return handlers; }

  private static long rev(Map<String, Object> r) { return ((Number) r.get("id")).longValue(); }

  private static SearchDocument withTimestamp(SearchDocument d) {
    return new SearchDocument(d.type(), d.sourceId(), d.module(), d.title(), d.subtitle(), d.content(), d.reference(), d.tags(), d.meta(), d.perm(), Instant.now().toString());
  }

  private void settle(long id, String status, String error) {
    db.update("update search_outbox set status=?, error=? where id=?", status, Rows.clip(error, 500), id);
  }

  /** Exponential backoff (5 s, 10 s, 20 s, ...) up to the attempt limit, then FAILED until an administrator re-queues it. */
  private void retry(long id, String error) {
    Integer attempts = db.queryForObject("select attempts from search_outbox where id=?", Integer.class, id);
    int next = (attempts == null ? 0 : attempts) + 1;
    String msg = Rows.clip(error == null ? "error" : error, 500);
    if (next >= maxAttempts) db.update("update search_outbox set status='FAILED', attempts=?, error=? where id=?", next, msg, id);
    else db.update("update search_outbox set attempts=?, error=?, next_attempt_at=now() + interval ? second where id=?", next, msg, 5 << Math.min(next - 1, 8), id);
  }

  /** Pending and failed counts for the administrator status screen. */
  public java.util.Map<String, Object> outboxStatus() { return outbox.status(); }

  /** Puts failed changes back in the queue; returns how many. */
  public int retryFailed() { return outbox.retryFailed(); }
}
