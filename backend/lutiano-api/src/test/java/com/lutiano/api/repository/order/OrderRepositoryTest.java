package com.lutiano.api.repository.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** An order moves on only from the status it is in, so two sellers cannot both accept it; lists group statuses and count what is waiting. */
@Testcontainers
class OrderRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static OrderRepository repo;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new OrderRepository(db);
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code) values('cu1','CU-O1','Ada','+237655100001',true,'ACTIVE','tok1','BRONZE'),('cu2','CU-O2','Bob','+237655100002',true,'ACTIVE','tok2','BRONZE')");
    db.update("insert into operation(id,name,code,type,active,qr_token) values('opA','A','OA','FIXED_POS',true,'qa'),('opB','B','OB','FIXED_POS',true,'qb')");
  }

  private static String order(String id, String customer, String op) {
    repo.insert(id, "ORD-" + id, customer, op, 1000, null, "key-" + id);
    repo.insertLine(id, 1, "p1", "Latte", 1000, 0, 1);
    return id;
  }

  @Test void onlyOneSellerCanAcceptAndTheStatusDecidesWhatIsNext() {
    order("t1", "cu1", "opA");
    assertTrue(repo.transition("t1", List.of("PLACED"), "ACCEPTED", "accepted_at", "seller1", null));
    assertFalse(repo.transition("t1", List.of("PLACED"), "ACCEPTED", "accepted_at", "seller2", null), "already accepted");
    assertFalse(repo.transition("t1", List.of("PLACED"), "CANCELLED", "closed_at", null, "late"), "a customer cannot cancel an accepted order");
    assertEquals("seller1", repo.find("t1").orElseThrow().get("handled_by"));
    assertTrue(repo.transition("t1", List.of("ACCEPTED", "READY"), "COLLECTED", "closed_at", "seller1", null));
    assertEquals("COMPLETED", repo.find("t1").orElseThrow().get("status_group"));
  }

  @Test void aRetryWithTheSameKeyFindsTheOrder() {
    order("t2", "cu1", "opA");
    assertEquals("t2", repo.byKey("key-t2").orElseThrow().get("id"));
  }

  @Test void customersSeeOnlyTheirOrdersGroupedByProgress() {
    order("g1", "cu2", "opA");
    order("g2", "cu2", "opA");
    repo.transition("g2", List.of("PLACED"), "REJECTED", "closed_at", "s", "No stock");
    assertEquals(1, repo.ofCustomer("cu2", "PENDING", PageRequest.of(1, 10)).total());
    assertEquals(1, repo.ofCustomer("cu2", "CANCELLED", PageRequest.of(1, 10)).total());
    assertEquals(2, repo.ofCustomer("cu2", null, PageRequest.of(1, 10)).total());
    assertEquals(0, repo.ofCustomer("cu1", "CANCELLED", PageRequest.of(1, 10)).items().stream().filter(o -> o.get("id").equals("g2")).count());
  }

  @Test void aSellerSeesOnlyTheirSalesPointsWithWaitingOrdersFirstAndCounts() {
    order("s1", "cu1", "opB");
    order("s2", "cu1", "opB");
    repo.transition("s2", List.of("PLACED"), "ACCEPTED", "accepted_at", "s", null);
    var page = repo.atOperations(List.of("opB"), null, null, null, PageRequest.of(1, 10));
    assertEquals(2, page.total());
    assertEquals(1L, ((Number) page.summary().get("placed")).longValue());
    assertEquals(1L, ((Number) page.summary().get("accepted")).longValue());
    assertTrue(page.items().stream().allMatch(o -> "opB".equals(o.get("operation_id"))));
  }

  @Test void ordersNobodyAcceptedInTimeAreFound() {
    order("e1", "cu1", "opA");
    db.update("update customer_order set placed_at = DATE_SUB(UTC_TIMESTAMP(), INTERVAL 45 MINUTE) where id='e1'");
    assertTrue(repo.unhandledSince(30).stream().anyMatch(o -> "e1".equals(o.get("id"))));
    assertFalse(repo.unhandledSince(60).stream().anyMatch(o -> "e1".equals(o.get("id"))));
  }
}
