package com.lutiano.api.service.search;

import static org.junit.jupiter.api.Assertions.*;

import com.lutiano.api.security.Actor;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;

/** Security gate: what each caller may see, checked against a real MariaDB with the real migrations. */
@Testcontainers
class SearchPolicyTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static SearchPolicies policies;
  static final String LOC_A = "a0000000-0000-4000-8000-000000000001", LOC_B = "a0000000-0000-4000-8000-000000000002",
      LOC_OFF = "a0000000-0000-4000-8000-000000000003", S1 = "s1", S2 = "s2";

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    policies = new SearchPolicies(db);
    db.update("insert into operation (id,name,code,active,qr_token) values (?,?,?,true,uuid()),(?,?,?,true,uuid()),(?,?,?,false,uuid())",
        LOC_A, "Alpha", "ALPHA", LOC_B, "Beta", "BETA", LOC_OFF, "Closed", "CLOSED");
    db.update("insert into seller_operation values (?,?),(?,?)", S1, LOC_A, S2, LOC_A);
  }

  static SearchPrincipal seller(String id) {
    List<String> ops = db.queryForList("select operation_id from seller_operation where seller_id=?", String.class, id);
    return SearchPrincipal.staff(new Actor(id, id + "@x", id, "SELLER"), ops);
  }

  @Test void sellerSeesOnlyAssignedActiveLocations() {
    assertEquals(Set.of(LOC_A), policies.authorize("OPERATION", seller(S1), List.of(LOC_A, LOC_B, LOC_OFF)));
  }

  @Test void twoSellersAtOneLocationBothSeeTheLocation() {
    assertEquals(Set.of(LOC_A), policies.authorize("OPERATION", seller(S1), List.of(LOC_A)));
    assertEquals(Set.of(LOC_A), policies.authorize("OPERATION", seller(S2), List.of(LOC_A)));
  }

  @Test void revokedAssignmentDisappearsImmediately() {
    db.update("insert into seller_operation values (?,?)", "s3", LOC_B);
    assertEquals(Set.of(LOC_B), policies.authorize("OPERATION", seller("s3"), List.of(LOC_B)));
    db.update("delete from seller_operation where seller_id='s3'");
    assertEquals(Set.of(), policies.authorize("OPERATION", seller("s3"), List.of(LOC_B)));
  }

  @Test void adminSeesInactiveAndPublicOnlyActive() {
    SearchPrincipal admin = SearchPrincipal.staff(new Actor("a", "a@x", "a", "ADMIN"), List.of());
    assertEquals(Set.of(LOC_A, LOC_OFF), policies.authorize("OPERATION", admin, List.of(LOC_A, LOC_OFF, "missing")));
    assertEquals(Set.of(LOC_A, LOC_B), policies.authorize("OPERATION", SearchPrincipal.publicCatalogue(), List.of(LOC_A, LOC_B, LOC_OFF)));
  }

  @Test void unknownRoleAndUnknownTypeSeeNothing() {
    SearchPrincipal odd = SearchPrincipal.staff(new Actor("x", "x@x", "x", "GUEST"), List.of());
    assertEquals(Set.of(), policies.authorize("OPERATION", odd, List.of(LOC_A)));
    assertEquals(Set.of(), policies.authorize("SALE", SearchPrincipal.staff(new Actor("a", "a@x", "a", "ADMIN"), List.of()), List.of("1")));
  }

  @Test void sellerSeesOnlyOwnSalesEvenAtASharedLocation() {
    for (String[] sale : new String[][] {{"sale1", S1}, {"sale2", S2}})
      db.update("insert into sale (id,reference,seller_id,operation_id,shift_id,idempotency_key,total_xaf) values (?,?,?,?,'sh',?,100)", sale[0], "REF-" + sale[0], sale[1], LOC_A, "k-" + sale[0]);
    assertEquals(Set.of("sale1"), policies.authorize("SALE", seller(S1), List.of("sale1", "sale2")));
    assertEquals(Set.of("sale2"), policies.authorize("SALE", seller(S2), List.of("sale1", "sale2")));
    SearchPrincipal admin = SearchPrincipal.staff(new Actor("a", "a@x", "a", "ADMIN"), List.of());
    SearchPrincipal inv = SearchPrincipal.staff(new Actor("i", "i@x", "i", "INVENTORY_MANAGER"), List.of());
    assertEquals(Set.of("sale1", "sale2"), policies.authorize("SALE", admin, List.of("sale1", "sale2")));
    assertEquals(Set.of("sale1", "sale2"), policies.authorize("SALE", inv, List.of("sale1", "sale2")));
    assertEquals(Set.of(), policies.authorize("SALE", SearchPrincipal.publicCatalogue(), List.of("sale1")));
  }

  @Test void paymentsStaffAndCustomersAreAdministratorsOnly() {
    db.update("insert into customer (id,code,full_name,phone,loyalty_token) values ('cu1','C-1','Ann','+237600000001',uuid())");
    SearchPrincipal admin = SearchPrincipal.staff(new Actor("a", "a@x", "a", "ADMIN"), List.of());
    SearchPrincipal inv = SearchPrincipal.staff(new Actor("i", "i@x", "i", "INVENTORY_MANAGER"), List.of());
    assertEquals(Set.of("cu1"), policies.authorize("CUSTOMER", admin, List.of("cu1")));
    for (String type : List.of("CUSTOMER", "STAFF", "PAYMENT", "REFUND")) {
      assertEquals(Set.of(), policies.authorize(type, inv, List.of("cu1")), type);
      assertEquals(Set.of(), policies.authorize(type, seller(S1), List.of("cu1")), type);
    }
  }

  @Test void sellersSeeOwnCashAndStockAtAssignedLocationsOnly() {
    db.update("insert into cash_outflow (id,reference,shift_id,seller_id,amount_xaf,category,description,status) values ('co1','CO-1','sh',?,10,'SUPPLIES','x','REQUESTED'),('co2','CO-2','sh',?,10,'SUPPLIES','x','REQUESTED')", S1, S2);
    assertEquals(Set.of("co1"), policies.authorize("CASH_OUTFLOW", seller(S1), List.of("co1", "co2")));
    db.update("insert into stocktake (id,operation_id,status,created_by) values ('st1',?,'COUNTING','a'),('st2',?,'COUNTING','a'),('st3',null,'COUNTING','a')", LOC_A, LOC_B);
    assertEquals(Set.of("st1"), policies.authorize("STOCKTAKE", seller(S1), List.of("st1", "st2", "st3")));
    SearchPrincipal inv = SearchPrincipal.staff(new Actor("i", "i@x", "i", "INVENTORY_MANAGER"), List.of());
    assertEquals(Set.of("st1", "st2", "st3"), policies.authorize("STOCKTAKE", inv, List.of("st1", "st2", "st3")));
    assertEquals(Set.of(), policies.authorize("CASH_OUTFLOW", inv, List.of("co1")));
    assertEquals(Set.of(), policies.authorize("PAYOUT", seller(S1), List.of("anything")));
  }

  @Test void transfersFollowAssignedLocationsAndOtherNewTypesAreBackOfficeOnly() {
    db.update("insert into stock_allocation (id,operation_id,product_id,sent_quantity,status) values ('t1',?,'p',1,'PENDING'),('t2',?,'p',1,'PENDING')", LOC_A, LOC_B);
    assertEquals(Set.of("t1"), policies.authorize("TRANSFER", seller(S1), List.of("t1", "t2")));
    for (String type : List.of("DISCREPANCY", "COMMISSION_RULE", "DELIVERY")) assertEquals(Set.of(), policies.authorize(type, seller(S1), List.of("t1")), type);
  }

  @Test void listTextSearchUsesCandidateIdsExactContactsAndFlagsTruncation() {
    db.update("insert into customer (id,code,full_name,phone,loyalty_token) values ('h1','C-H1','Hana',?,uuid()),('h2','C-H2','Hugo','+237611111102',uuid()),('h3','C-H3','Ivan','+237611111103',uuid())", "+237611111101");
    String base = "select c.id, c.full_name from customer c";
    // the search service proposed Hana by name; Ivan is found by exact phone; Hugo matches neither
    var byName = new com.lutiano.api.utils.Candidates("hana", Set.of("h1"), false);
    var page = new com.lutiano.api.utils.SqlFilter(base).and("c.id in ('h1','h2','h3')").searchIds(byName, "c.id", "c.phone").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10));
    assertEquals(1, page.total());
    var byPhone = new com.lutiano.api.utils.Candidates("+237 611 111 103", Set.of(), false);
    var phonePage = new com.lutiano.api.utils.SqlFilter(base).and("c.id in ('h1','h2','h3')").searchIds(byPhone, "c.id", "c.phone").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10));
    assertEquals("Ivan", phonePage.items().get(0).get("full_name"));
    // a partial number is not an exact match, so it finds nothing
    var partial = new com.lutiano.api.utils.Candidates("6111111", Set.of(), false);
    assertEquals(0, new com.lutiano.api.utils.SqlFilter(base).and("c.id in ('h1','h2','h3')").searchIds(partial, "c.id", "c.phone").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10)).total());
    // nothing found and no contact match: no rows (not "all rows"); blank text (null) leaves the list alone
    var none = new com.lutiano.api.utils.Candidates("zzz", Set.of(), false);
    assertEquals(0, new com.lutiano.api.utils.SqlFilter(base).and("c.id in ('h1','h2','h3')").searchIds(none, "c.id").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10)).total());
    assertEquals(3, new com.lutiano.api.utils.SqlFilter(base).and("c.id in ('h1','h2','h3')").searchIds(null, "c.id").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10)).total());
    // the truncation flag reaches the page the client receives
    var capped = new com.lutiano.api.utils.Candidates("h", Set.of("h1", "h2"), true);
    assertTrue(new com.lutiano.api.utils.SqlFilter(base).searchIds(capped, "c.id").page(db, "order by c.id", com.lutiano.api.models.request.PageRequest.of(1, 10)).truncated());
  }
}
