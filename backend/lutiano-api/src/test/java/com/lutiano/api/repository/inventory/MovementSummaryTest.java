package com.lutiano.api.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.util.List;
import java.util.Map;

/** The period summary and the date-bounded movement list a seller reads for the Inventory screens. */
@Testcontainers
class MovementSummaryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static MovementRepository movements;
  static JdbcTemplate db;
  static final String OP = "op-summary";
  static final LocalDate DAY1 = LocalDate.of(2026, 3, 1), DAY2 = LocalDate.of(2026, 3, 2);

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    movements = new MovementRepository(db);
    db.update("insert into product_category (id,name) values ('c1','Hot'),('c2','Pastry')");
    db.update("insert into product (id,sku,name,category,price_xaf,active,track_stock) values ('pa','A','Americano','Hot',1000,true,true),('pb','B','Brioche','Pastry',500,true,true)");
    // day 1: +50 Americano received, -2 sold; day 2: -3 Americano sold, -7 Brioche sold, +10 Brioche received
    add("pa", 50, 50, "TRANSFER_IN", DAY1, 9);
    add("pa", -2, 48, "SALE", DAY1, 10);
    add("pa", -3, 45, "SALE", DAY2, 10);
    add("pb", 10, 10, "TRANSFER_IN", DAY2, 8);
    add("pb", -7, 3, "SALE", DAY2, 11);
    // another operation must never leak in
    db.update("insert into stock_movement(id,operation_id,product_id,quantity_delta,balance_after,movement_type,created_at) values (uuid(),'other','pa',-99,0,'SALE',?)", Periods.startOf(DAY2));
  }

  static void add(String product, int delta, int after, String type, LocalDate day, int hour) {
    Timestamp at = Timestamp.from(day.atStartOfDay(Periods.DOUALA).plusHours(hour).toInstant());
    db.update("insert into stock_movement(id,operation_id,product_id,quantity_delta,balance_after,movement_type,created_at) values (uuid(),?,?,?,?,?,?)", OP, product, delta, after, type, at);
  }

  @Test void totalsAndTopConsumedForAWholePeriod() {
    var s = movements.summary(OP, null, Periods.startOf(DAY1), Periods.endOf(DAY2), 5);
    assertEquals(60L, s.get("received"));
    assertEquals(12L, s.get("consumed"));
    assertEquals(48L, s.get("net"));
    var top = (java.util.List<?>) s.get("top_consumed");
    assertEquals(2, top.size());
    var first = (java.util.Map<?, ?>) top.get(0);
    assertEquals("Brioche", first.get("product_name"), "Brioche sold 7, Americano 5");
    assertEquals(7L, ((Number) first.get("quantity")).longValue());
  }

  @Test void aSingleDayAndASingleProduct() {
    var day1 = movements.summary(OP, null, Periods.startOf(DAY1), Periods.endOf(DAY1), 5);
    assertEquals(50L, day1.get("received"));
    assertEquals(2L, day1.get("consumed"));
    var americano = movements.summary(OP, "pa", null, null, 5);
    assertEquals(50L, americano.get("received"), "no period: everything ever");
    assertEquals(5L, americano.get("consumed"));
  }

  @Test void movementsAreBoundedByDayAndCarryTheBalanceAfter() {
    var page = movements.forOperation(OP, "pa", "OUT", Periods.startOf(DAY2), Periods.endOf(DAY2), PageRequest.of(1, 10));
    assertEquals(1, page.total());
    assertEquals(3L, ((Number) page.items().get(0).get("quantity")).longValue());
    assertEquals(45, ((Number) page.items().get(0).get("balance_after")).intValue());
  }
}
