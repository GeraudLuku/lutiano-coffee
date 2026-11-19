package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.lutiano.api.config.AppProperties;

/**
 * Rebuilds the whole index from MariaDB without stopping search or losing concurrent edits:
 * (1) note the outbox watermark W, (2) fill a new index from current data at revision W, (3) replay every outbox change
 * after W into it (newer revisions win, so order does not matter), (4) briefly hold the worker, replay once more, check the
 * counts, and switch the alias. The old index stays for rollback.
 */
@Component
public class SearchReindexer {
  private static final Logger log = LoggerFactory.getLogger(SearchReindexer.class);

  private final JdbcTemplate db;
  private final ElasticsearchClient es;
  private final SearchIndex index;
  private final SearchIndexer indexer;
  private final int batchSize;
  private final AtomicBoolean running = new AtomicBoolean();
  private volatile Map<String, Object> progress = Map.of("state", "IDLE");

  public SearchReindexer(JdbcTemplate db, ElasticsearchClient es, SearchIndex index, SearchIndexer indexer,
                         AppProperties.Search search) {
    this.db = db; this.es = es; this.index = index; this.indexer = indexer; this.batchSize = search.batchSize();
  }

  public Map<String, Object> progress() { return progress; }

  /** Starts a reindex in the background; false when one is already running. */
  @Async
  public void start() {
    if (!running.compareAndSet(false, true)) return;
    try { run(); } catch (Exception e) {
      log.error("Reindex failed: {}", e.getMessage(), e);
      progress = Map.of("state", "FAILED", "error", String.valueOf(e.getMessage()));
    } finally { indexer.pause(false); running.set(false); }
  }

  public boolean isRunning() { return running.get(); }

  void run() throws Exception {
    Long w = db.queryForObject("select coalesce(max(id),0) from search_outbox", Long.class);
    long watermark = w == null ? 0 : w;
    String target = index.create();
    Map<String, Object> p = new LinkedHashMap<>(Map.of("state", "BACKFILL", "target", target, "watermark", watermark));
    Map<String, Long> expected = new LinkedHashMap<>();
    progress = p;
    for (SearchHandler h : indexer.handlers().values()) {
      long n = 0; String after = null; List<String> ids;
      while (!(ids = h.idsAfter(after, batchSize)).isEmpty()) {
        BulkRequest.Builder bulk = new BulkRequest.Builder();
        for (String id : ids) {
          Optional<SearchDocument> doc = h.load(id);
          if (doc.isPresent()) { SearchIndexer.add(bulk, target, h.type() + ":" + id, doc, watermark); n++; }
        }
        write(bulk);
        after = ids.get(ids.size() - 1);
        p.put(h.type(), n); progress = new LinkedHashMap<>(p);
      }
      expected.put(h.type(), n);
    }
    p.put("state", "REPLAY"); progress = new LinkedHashMap<>(p);
    long seen = replay(target, watermark);
    indexer.pause(true);
    seen = replay(target, seen);   // final pass with the worker held, so nothing slips in between replay and switch
    es.indices().refresh(r -> r.index(target));
    for (Map.Entry<String, Long> e : expected.entrySet()) {
      // replayed changes may add or remove documents, so only an absurd gap (a failed backfill) blocks the switch
      long have = es.count(c -> c.index(target).query(q -> q.term(t -> t.field("type").value(e.getKey())))).count();
      p.put("count." + e.getKey(), have);
      if (Math.abs(have - e.getValue()) > Math.max(5, e.getValue() / 5)) throw new IllegalStateException("Count mismatch for " + e.getKey() + ": index " + have + ", database " + e.getValue());
    }
    String previous = index.current();
    index.switchAlias(target);
    index.prune(previous == null ? List.of(target) : List.of(target, previous));   // keep one generation for rollback
    p.put("state", "DONE"); progress = new LinkedHashMap<>(p);
    log.info("Reindex finished into {}", target);
  }

  /** Applies every outbox change after {@code from} to {@code target}; returns the highest id handled. */
  private long replay(String target, long from) throws Exception {
    long last = from;
    while (true) {
      List<Map<String, Object>> rows = db.queryForList("select id, type, source_id from search_outbox where id > ? order by id limit ?", last, batchSize);
      if (rows.isEmpty()) return last;
      BulkRequest.Builder bulk = new BulkRequest.Builder();
      boolean any = false;
      for (Map<String, Object> r : rows) {
        long id = ((Number) r.get("id")).longValue();
        SearchHandler h = indexer.handlers().get((String) r.get("type"));
        last = id;
        if (h == null) continue;
        SearchIndexer.add(bulk, target, r.get("type") + ":" + r.get("source_id"), h.load((String) r.get("source_id")), id);
        any = true;
      }
      if (any) write(bulk);
    }
  }

  private void write(BulkRequest.Builder bulk) throws Exception {
    var res = es.bulk(bulk.build());
    if (res.errors()) {
      // a 409 means a newer revision is already there, which is fine; anything else is a real failure
      var bad = res.items().stream().filter(i -> i.error() != null && i.status() != 409 && i.status() != 404).findFirst();
      if (bad.isPresent()) throw new IllegalStateException("Bulk write rejected: " + bad.get().error().reason());
    }
  }
}
