package com.lutiano.api.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.search.SearchOutbox;
import java.lang.reflect.Method;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.service.inventory.StockRequestService;

/** A request carries several products, a seller sees only their own, and counts per status cover all of them. */
@Testcontainers
class StockRequestRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static StockRequestRepository requests;
  static TransferRepository transfers;
  static JdbcTemplate db;
  static final String OP = "op-req", ME = "seller-me", OTHER = "seller-other";

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    requests = new StockRequestRepository(db);
    transfers = new TransferRepository(db, new SearchOutbox(db));
    db.update("insert into product_category (id,name) values ('c1','Hot')");
    for (String p : List.of("pa", "pb", "pc"))
      db.update("insert into product (id,sku,name,category,price_xaf,active,track_stock) values (?,?,?,?,1000,true,true)", p, p.toUpperCase(), "Product " + p, "Hot");
    db.update("insert into operation (id,name,code,type,active,qr_token) values (?,?,?,?,true,?)", OP, "Bonapriso Cafe", "LTN-REQ", "MOBILE", "qr-req");
    for (String u : List.of(ME, OTHER))
      db.update("insert into staff_user (id,email,password_hash,full_name,role,active) values (?,?,'x',?,'SELLER',true)", u, u + "@x.local", u);
    request("r1", "SR-2026-00001", ME, "PENDING_APPROVAL", List.of("pa", "pb"));
    request("r2", "SR-2026-00002", ME, "APPROVED", List.of("pc"));
    request("r3", "SR-2026-00003", OTHER, "PENDING_APPROVAL", List.of("pa"));
  }

  static void request(String id, String reference, String by, String status, List<String> productIds) {
    requests.insert(id, reference, OP, by, null, null, null);
    db.update("update stock_request set status=? where id=?", status, id);
    int qty = 10;
    for (String p : productIds) transfers.insertRequest(id + "-" + p, id, OP, p, qty += 10, by, null);
  }

  @Test void aRequestHasItsProductsAndTotals() {
    var r = requests.find("r1").orElseThrow();
    assertEquals(2L, ((Number) r.get("item_count")).longValue());
    assertEquals(50L, ((Number) r.get("total_quantity")).longValue());
    var items = (List<?>) requests.withItems(r, transfers.ofRequest("r1")).get("items");
    assertEquals(2, items.size());
  }

  @Test void aSellerSeesOnlyTheirOwnRequestsAndCountsCoverAllOfThem() {
    var page = requests.forSeller(ME, OP, "PENDING_APPROVAL", null, PageRequest.of(1, 5));
    assertEquals(1, page.total());
    assertEquals("SR-2026-00001", page.items().get(0).get("reference"));
    assertEquals(2, ((List<?>) page.items().get(0).get("items")).size());
    assertEquals(2L, ((Number) page.summary().get("total")).longValue(), "the counts ignore the status filter but never include other sellers");
    assertEquals(1L, ((Number) page.summary().get("pending")).longValue());
    assertEquals(1L, ((Number) page.summary().get("approved")).longValue());
  }

  @Test void searchesByReferencePrefixOnly() {
    assertEquals(1, requests.forSeller(ME, OP, null, "sr-2026-0000", PageRequest.of(1, 1)).items().size());
    assertEquals(2, requests.forSeller(ME, OP, null, "SR-2026-0000", PageRequest.of(1, 5)).total());
    assertEquals(0, requests.forSeller(ME, OP, null, "SR-9999", PageRequest.of(1, 5)).total());
    assertEquals(2, requests.forSeller(ME, OP, null, "%", PageRequest.of(1, 5)).total(), "wildcards typed by the person are not wildcards");
  }

  @Test void requestLinesStayOutOfTheSingleTransferLists() {
    assertTrue(transfers.search("PENDING_APPROVAL", OP).isEmpty());
    assertEquals(0, transfers.page("PENDING_APPROVAL", OP, null, PageRequest.of(1, 5)).total());
    assertEquals(4, db.queryForObject("select count(*) from stock_allocation where status='PENDING_APPROVAL' and operation_id=?", Integer.class, OP), "the rows themselves exist");
  }

  @Test void theApprovingEntryPointsRunInOneTransaction() throws Exception {
    for (String name : List.of("create", "approve", "reject", "dispatch", "cancel")) {
      Method m = java.util.Arrays.stream(StockRequestService.class.getDeclaredMethods()).filter(x -> x.getName().equals(name)).findFirst().orElseThrow();
      assertTrue(m.isAnnotationPresent(Transactional.class), name + " must be transactional (the ledger needs an open transaction)");
    }
  }
}
