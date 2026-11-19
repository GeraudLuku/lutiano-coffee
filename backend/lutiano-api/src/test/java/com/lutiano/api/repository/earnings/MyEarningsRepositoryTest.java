package com.lutiano.api.repository.earnings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A seller's earnings reads cover their own sales only, split commission by source, and group payouts by what the seller cares about. */
@Testcontainers
class MyEarningsRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static MyEarningsRepository repo;
  static JdbcTemplate db;
  static final LocalDate DAY = LocalDate.of(2026, 3, 10);
  static final Timestamp FROM = Periods.startOf(LocalDate.of(2026, 3, 1)), TO = Periods.endOf(LocalDate.of(2026, 3, 31));

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new MyEarningsRepository(db);
    db.update("insert into product_category (id,name) values ('c1','Hot')");
    db.update("insert into product (id,sku,name,category,price_xaf,active,track_stock) values ('p1','A','Latte','Hot',2000,true,true),('p2','B','Mocha','Hot',2500,true,true)");
    for (String o : List.of("op1", "op2"))
      db.update("insert into operation (id,name,code,type,active,qr_token) values (?,?,?,'MOBILE',true,?)", o, "Unit " + o, o.toUpperCase(), "qr-" + o);
    for (String u : List.of("me", "other")) db.update("insert into staff_user (id,email,password_hash,full_name,role,active) values (?,?,'x',?,'SELLER',true)", u, u + "@x.local", u);
    // me: sale 1 at op1 (Latte with 500 of add-ons, 5% rule), sale 2 at op2 (Mocha, fixed 100 per unit); other: one sale
    sale("s1", "me", "op1", 5000, 20);
    line("s1", 1, "p1", "Latte", 2000, 500, 2);
    commission("s1", 1, "me", "p1", "PERCENT", 500, 2, 5000, 250);
    sale("s2", "me", "op2", 5000, 21);
    line("s2", 1, "p2", "Mocha", 2500, 0, 2);
    commission("s2", 1, "me", "p2", "FIXED_PER_UNIT", 100, 2, 5000, 200);
    sale("s3", "other", "op1", 9000, 22);
    line("s3", 1, "p1", "Latte", 2000, 0, 3);
    commission("s3", 1, "other", "p1", "PERCENT", 500, 3, 6000, 300);
    db.update("insert into earning_adjustment (id,reference,seller_id,adjustment_type,amount_xaf,created_at) values ('a1','EA-1','me','MANUAL',-50,?)", Periods.startOf(DAY));
    db.update("insert into earning_period (id,starts_on,ends_on,status) values ('ep1','2026-03-01','2026-03-31','FINALIZED')");
    payout("po1", "me", "PAID", 1000);
    payout("po2", "me", "PENDING_APPROVAL", 500);
    payout("po3", "me", "REJECTED", 300);
    payout("po4", "other", "PAID", 99000);
  }

  static void sale(String id, String seller, String op, long total, int hour) {
    db.update("insert into sale (id,reference,seller_id,operation_id,shift_id,idempotency_key,total_xaf,created_at) values (?,?,?,?,'sh',?,?,?)", id, "SL-" + id, seller, op, "k-" + id, total,
        Timestamp.from(DAY.atStartOfDay(Periods.DOUALA).plusHours(hour % 20).toInstant()));
  }

  static void line(String sale, int no, String product, String name, long unit, long addons, int qty) {
    db.update("insert into sale_line (sale_id,line_no,product_id,product_name,unit_price_xaf,addons_unit_xaf,quantity) values (?,?,?,?,?,?,?)", sale, no, product, name, unit, addons, qty);
  }

  static void commission(String sale, int no, String seller, String product, String basis, int rate, int qty, long value, long commission) {
    db.update("insert into sale_commission (sale_id,line_no,seller_id,product_id,rule_id,basis,rate,quantity,line_value_xaf,commission_xaf) values (?,?,?,?,'r',?,?,?,?,?)",
        sale, no, seller, product, basis, rate, qty, value, commission);
  }

  static void payout(String id, String seller, String status, long amount) {
    db.update("insert into payout (id,reference,seller_id,period_id,amount_xaf,method,status,requires_approval,requested_by,requested_at) values (?,?,?,'ep1',?,'CASH',?,false,'me',UTC_TIMESTAMP())", id, "PO-" + id, seller, amount, status);
  }

  @Test void totalsCoverOnlyTheSellersOwnSales() {
    var sales = repo.sales("me", FROM, TO);
    assertEquals(2L, ((Number) sales.get("sales_count")).longValue());
    assertEquals(10000L, ((Number) sales.get("sales_xaf")).longValue());
    assertEquals(450L, repo.commission("me", FROM, TO));
    assertEquals(-50L, repo.adjustments("me", FROM, TO));
  }

  @Test void commissionSplitsIntoProductAndAddOnParts() {
    var s = repo.commissionBySource("me", FROM, TO);
    assertEquals(450L, ((Number) s.get("total_xaf")).longValue());
    assertEquals(50L, ((Number) s.get("addons_xaf")).longValue(), "500 of add-ons x 2 units x 5% = 50; the fixed rule has no add-on part");
  }

  @Test void topProductsOperationsAndDays() {
    var top = repo.topProducts("me", FROM, TO, 5);
    assertEquals("Latte", top.get(0).get("product_name"));
    var ops = repo.byOperation("me", FROM, TO);
    assertEquals(2, ops.size());
    assertEquals(250L, ((Number) ops.get(0).get("commission_xaf")).longValue());
    assertEquals(450L, repo.commissionByDay("me", FROM, TO).get(DAY.toString()));
    assertEquals(-50L, repo.adjustmentsByDay("me", FROM, TO).get(DAY.toString()));
  }

  @Test void salesWithCommissionArePagedAndOwn() {
    var page = repo.salesWithCommission("me", FROM, TO, PageRequest.of(1, 1));
    assertEquals(2, page.total());
    assertEquals(1, page.items().size());
    assertTrue(repo.saleOf("s3", "me").isEmpty(), "another seller's sale is not readable");
    assertTrue(repo.saleOf("s1", "me").isPresent());
  }

  @Test void payoutsAreGroupedAndSummarisedOverAllOfTheSellersPayouts() {
    var all = repo.payouts("me", List.of(), PageRequest.of(1, 10));
    assertEquals(3, all.total());
    assertEquals(1L, ((Number) all.summary().get("completed")).longValue());
    assertEquals(1L, ((Number) all.summary().get("pending")).longValue());
    assertEquals(1L, ((Number) all.summary().get("cancelled")).longValue());
    assertEquals(1000L, ((Number) all.summary().get("paid_out_xaf")).longValue(), "another seller's payout never counts");
    var pending = repo.payouts("me", List.of("PENDING_APPROVAL", "APPROVED", "REVERSAL_PENDING"), PageRequest.of(1, 10));
    assertEquals(1, pending.total());
    assertEquals(1L, ((Number) pending.summary().get("completed")).longValue(), "the summary ignores the filter");
    assertTrue(repo.payout("po4", "me").isEmpty());
  }
}
