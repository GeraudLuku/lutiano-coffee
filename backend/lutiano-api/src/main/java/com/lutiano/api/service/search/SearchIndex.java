package com.lutiano.api.service.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsAliasRequest;
import jakarta.annotation.PostConstruct;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Owns the index versions and the alias. Everything reads and writes through the alias {@code lutiano-search};
 * a mapping change is a new mapping file, a reindex into a fresh index, then an alias switch (see SearchReindexer).
 * Start-up never fails when Elasticsearch is down: search reports unavailable and the outbox keeps the changes.
 */
@Component
public class SearchIndex {
  public static final String ALIAS = "lutiano-search";
  private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
  private static final Logger log = LoggerFactory.getLogger(SearchIndex.class);

  private final ElasticsearchClient es;

  public SearchIndex(ElasticsearchClient es) { this.es = es; }

  @PostConstruct
  void bootstrap() {
    try { ensureAlias(); } catch (Exception e) { log.warn("Search index not bootstrapped (Elasticsearch unavailable?): {}", e.getMessage()); }
  }

  /** Creates the first index and points the alias at it when no alias exists yet. */
  public void ensureAlias() throws Exception {
    if (es.indices().existsAlias(ExistsAliasRequest.of(r -> r.name(ALIAS))).value()) return;
    String name = create();
    es.indices().updateAliases(u -> u.actions(a -> a.add(x -> x.index(name).alias(ALIAS).isWriteIndex(true))));
    log.info("Created search index {} and alias {}", name, ALIAS);
  }

  /** Creates a new timestamped index from the mapping file (resources/search/index-mapping.json), not yet behind the alias. */
  public String create() throws Exception {
    String name = ALIAS + "-" + LocalDateTime.now().format(STAMP);
    String json = new String(new ClassPathResource("search/index-mapping.json").getInputStream().readAllBytes());
    es.indices().create(r -> r.index(name).withJson(new StringReader(json)));
    return name;
  }

  /** Atomically moves the alias to {@code target}; the old indices stay (without the alias) for rollback. */
  public void switchAlias(String target) throws Exception {
    var current = es.indices().getAlias(g -> g.name(ALIAS)).result().keySet();
    es.indices().updateAliases(u -> {
      for (String old : current) u.actions(a -> a.remove(x -> x.index(old).alias(ALIAS)));
      return u.actions(a -> a.add(x -> x.index(target).alias(ALIAS).isWriteIndex(true)));
    });
  }

  /** Deletes older search indexes, keeping {@code keep} (the live one and the one just replaced, for rollback). */
  public void prune(Collection<String> keep) throws Exception {
    for (String name : es.indices().get(g -> g.index(ALIAS + "-*")).result().keySet())
      if (!keep.contains(name)) { es.indices().delete(d -> d.index(name)); log.info("Deleted old search index {}", name); }
  }

  /** The index the alias points at, or null. */
  public String current() throws Exception {
    return es.indices().getAlias(g -> g.name(ALIAS)).result().keySet().stream().findFirst().orElse(null);
  }

  /** Document count per type in the live index. */
  public Map<String, Long> counts(Collection<String> types) throws Exception {
    es.indices().refresh(r -> r.index(ALIAS));
    Map<String, Long> out = new TreeMap<>();
    for (String t : types) out.put(t, es.count(c -> c.index(ALIAS).query(q -> q.term(x -> x.field("type").value(t)))).count());
    return out;
  }

  public boolean available() {
    try { return es.ping().value(); } catch (Exception e) { return false; }
  }
}
