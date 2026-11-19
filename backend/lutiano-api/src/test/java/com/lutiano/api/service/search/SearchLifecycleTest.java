package com.lutiano.api.service.search;

import static org.junit.jupiter.api.Assertions.*;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.security.Actor;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.config.SearchHandlers;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.definitions.enums.SearchType;

/** The index lifecycle against real MariaDB and Elasticsearch (same version as docker-compose): create, change, deactivate, delete, outage, reindex. */
@Testcontainers
class SearchLifecycleTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  @Container static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.15.5")
      .withEnv("xpack.security.enabled", "false").withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

  /** indexing on, batches of 50, 3 attempts, 5,000 candidates, 500 verified results, 100-character queries */
  static final AppProperties.Search SEARCH = new AppProperties.Search(true, 50, 3, 5000, 500, 100);

  static JdbcTemplate db;
  static ElasticsearchClient es;
  static SearchIndex index;
  static SearchOutbox outbox;
  static SearchIndexer indexer;
  static final String LOC = "b0000000-0000-4000-8000-000000000001";

  static ElasticsearchClient client(String host, int port) {
    return new ElasticsearchClient(new RestClientTransport(RestClient.builder(new HttpHost(host, port, "http")).build(), new JacksonJsonpMapper()));
  }

  static SearchIndexer indexer(ElasticsearchClient c) { return new SearchIndexer(db, c, outbox, handlers(), SEARCH); }

  static List<SearchHandler> handlers() {
    SearchHandlers h = new SearchHandlers();
    return List.of(h.productHandler(db), h.categoryHandler(db), h.addonHandler(db), h.operationHandler(db), h.deliveryHandler(db));
  }

  @BeforeAll static void setUp() throws Exception {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    es = client(elastic.getHost(), elastic.getMappedPort(9200));
    index = new SearchIndex(es);
    index.ensureAlias();
    outbox = new SearchOutbox(db);
    indexer = indexer(es);
  }

  long count(String type) throws Exception {
    es.indices().refresh(r -> r.index(SearchIndex.ALIAS));
    return es.count(c -> c.index(SearchIndex.ALIAS).query(q -> q.term(t -> t.field("type").value(type)))).count();
  }

  String status(String id) throws Exception {
    var r = es.get(g -> g.index(SearchIndex.ALIAS).id("OPERATION:" + id), SearchDocument.class);
    return r.found() ? r.source().perm().status() : null;
  }

  @Test void operationLifecycle() throws Exception {
    db.update("insert into operation (id,name,code,active,qr_token,city) values (?,?,?,true,uuid(),'Douala')", LOC, "Café Central", "CC-1");
    outbox.changed(SearchType.OPERATION, LOC);
    outbox.changed(SearchType.OPERATION, LOC);   // coalesces with the first
    assertEquals(1, db.queryForObject("select count(*) from search_outbox where source_id=? and status='PENDING'", Integer.class, LOC));
    indexer.runBatch();
    assertEquals("ACTIVE", status(LOC));

    db.update("update operation set active=false where id=?", LOC);
    outbox.changed(SearchType.OPERATION, LOC);
    indexer.runBatch();
    assertEquals("INACTIVE", status(LOC));

    db.update("delete from operation where id=?", LOC);
    outbox.changed(SearchType.OPERATION, LOC);
    indexer.runBatch();
    assertNull(status(LOC));
  }

  @Test void staleRevisionNeverOverwritesNewer() throws Exception {
    String id = "b0000000-0000-4000-8000-000000000002";
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid())", id, "Newer", "N-1");
    db.update("insert into search_outbox (id,type,source_id) values (900000,'OPERATION',?)", id);
    indexer.runBatch();
    // an older change (lower revision) replayed late must not overwrite the newer document
    db.update("update operation set name='Changed' where id=?", id);
    db.update("insert into search_outbox (id,type,source_id) values (800000,'OPERATION',?)", id);
    indexer.runBatch();
    var doc = es.get(g -> g.index(SearchIndex.ALIAS).id("OPERATION:" + id), SearchDocument.class).source();
    assertEquals("Newer", doc.title());
  }

  @Test void outageKeepsTheChangeAndRecovers() throws Exception {
    String id = "b0000000-0000-4000-8000-000000000003";
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid())", id, "Outage", "O-1");
    outbox.changed(SearchType.OPERATION, id);
    assertThrows(Exception.class, () -> indexer(client("localhost", 1)).runBatch());
    assertEquals(1, db.queryForObject("select count(*) from search_outbox where source_id=? and status='PENDING' and attempts=1", Integer.class, id));
    db.update("update search_outbox set next_attempt_at = now() where source_id=?", id);
    indexer.runBatch();
    assertEquals("ACTIVE", status(id));
  }

  @Test void reindexRebuildsFromDatabaseAndSwitchesAlias() throws Exception {
    String id = "b0000000-0000-4000-8000-000000000004";
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid())", id, "Reindexed", "R-1");   // never queued
    String before = index.current();
    new SearchReindexer(db, es, index, indexer, SEARCH).run();
    assertNotEquals(before, index.current());
    assertEquals("ACTIVE", status(id));
  }

  @Test void searchMatchesAccentsTyposAndCodesAndHidesWhatTheCallerMayNotSee() throws Exception {
    String mine = "c0000000-0000-4000-8000-000000000001", other = "c0000000-0000-4000-8000-000000000002";
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid()),(?,?,?,true,uuid())",
        mine, "Zéphyr Kiosque", "ZEPH-01", other, "Zéphyr Entrepôt", "ZEPH-02");
    db.update("insert into seller_operation values ('sx', ?)", mine);
    outbox.changed(SearchType.OPERATION, mine); outbox.changed(SearchType.OPERATION, other);
    indexer.runBatch();
    es.indices().refresh(r -> r.index(SearchIndex.ALIAS));
    SearchService svc = new SearchService(es, new SearchPolicies(db), new ContactLookup(db, null), SEARCH);
    SearchPrincipal admin = SearchPrincipal.staff(new com.lutiano.api.security.Actor("a", "a@x", "a", "ADMIN"), List.of());
    SearchPrincipal seller = SearchPrincipal.staff(new com.lutiano.api.security.Actor("sx", "s@x", "s", "SELLER"), List.of(mine));
    var req = com.lutiano.api.models.request.PageRequest.of(1, 50);

    assertEquals(2, svc.search(admin, "zephyr", null, req).total());                    // accent-insensitive
    assertEquals(2, svc.search(admin, "zephir", null, req).total());                    // typo
    assertEquals("ZEPH-02", svc.search(admin, "ZEPH-02", null, req).items().get(0).get("reference"));   // exact code ranks first, case-insensitive
    assertEquals(1, svc.search(seller, "zephyr", null, req).total());                   // only the assigned location
    // assignment revoked: gone immediately, before any reindex
    db.update("delete from seller_operation where seller_id='sx'");
    assertEquals(0, svc.search(seller, "zephyr", null, req).total());
    assertEquals(2, svc.search(SearchPrincipal.publicCatalogue(), "zeph", List.of("OPERATION"), req).total());   // prefix
    assertFalse(svc.suggest(admin, "zeph", 3).isEmpty());
    assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> svc.search(admin, "x".repeat(101), null, req));
  }

  @Test void messagesWithOneTimeCodesAreNeverIndexed() throws Exception {
    db.update("insert into notification_delivery (id,event_key,recipient_name,language,body,status) values ('d1','OTP_SIGN_IN','Ann','fr','votre code','LOGGED'),('d2','PAYOUT_PAID','Ann','fr','Votre paiement est effectué','LOGGED')");
    outbox.changed(SearchType.DELIVERY, "d1"); outbox.changed(SearchType.DELIVERY, "d2");
    indexer.runBatch();
    assertNull(es.get(g -> g.index(SearchIndex.ALIAS).id("DELIVERY:d1"), SearchDocument.class).source());
    assertNotNull(es.get(g -> g.index(SearchIndex.ALIAS).id("DELIVERY:d2"), SearchDocument.class).source());
    assertEquals(0, db.queryForObject("select count(*) from search_outbox where status='FAILED'", Integer.class));
  }

  /** Edits made while a reindex runs must not be lost: the last write wins in the new index, whichever path delivered it. */
  @Test void editsDuringReindexEndUpInTheNewIndex() throws Exception {
    String id = "b0000000-0000-4000-8000-000000000009";
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid())", id, "Edit 0", "E-0");
    outbox.changed(SearchType.OPERATION, id);
    indexer.runBatch();
    for (int i = 0; i < 30; i++) db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid())",
        "b1000000-0000-4000-8000-0000000000" + String.format("%02d", i), "Filler " + i, "F-" + i);   // make the backfill take a moment
    Thread reindex = new Thread(() -> { try { new SearchReindexer(db, es, index, indexer, new AppProperties.Search(true, 5, 3, 5000, 500, 100)).run(); } catch (Exception e) { throw new RuntimeException(e); } });
    reindex.start();
    String last = null;
    for (int i = 1; i <= 15; i++) {
      last = "Edit " + i;
      db.update("update operation set name=? where id=?", last, id);
      outbox.changed(SearchType.OPERATION, id);
      indexer.runBatch();
    }
    reindex.join(60_000);
    indexer.runBatch();   // anything queued after the switch goes to the live alias
    es.indices().refresh(r -> r.index(SearchIndex.ALIAS));
    assertEquals(last, es.get(g -> g.index(SearchIndex.ALIAS).id("OPERATION:" + id), SearchDocument.class).source().title());
  }
}
