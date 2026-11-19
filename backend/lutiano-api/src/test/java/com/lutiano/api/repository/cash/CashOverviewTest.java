package com.lutiano.api.repository.cash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutiano.api.repository.search.SearchOutbox;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A seller's cash overview counts only their own undecided outflows, unconfirmed or disputed remittances and recounts asked for. */
@Testcontainers
class CashOverviewTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static CashRepository cash;
  static JdbcTemplate db;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    cash = new CashRepository(db, new SearchOutbox(db));
    for (String u : new String[] {"me", "other"})
      db.update("insert into staff_user (id,email,password_hash,full_name,role,active) values (?,?,'x',?,'SELLER',true)", u, u + "@x.local", u);
    shift("s-me-1", "me", "RECOUNT_REQUESTED");
    shift("s-me-2", "me", "RESOLVED");
    shift("s-other", "other", "RECOUNT_REQUESTED");
    cash.insertOutflow("o1", "CO-1", "s-me-1", "me", 5000, "SUPPLIES", "Milk");
    cash.insertOutflow("o2", "CO-2", "s-other", "other", 7000, "TRANSPORT", "Taxi");
    remittance("r1", "me", "OUTSTANDING", 20000);
    remittance("r2", "me", "OUTSTANDING", 10000);
    remittance("r3", "me", "DISPUTED", 3000);
    remittance("r4", "me", "ACKNOWLEDGED", 9000);
    remittance("r5", "other", "OUTSTANDING", 99000);
  }

  static void shift(String id, String seller, String review) {
    db.update("insert into cash_shift (id,seller_id,opening_float,expected_cash,status,review_status) values (?,?,0,0,'CLOSED',?)", id, seller, review);
  }

  static void remittance(String id, String seller, String status, long amount) {
    db.update("insert into cash_remittance (id,reference,shift_id,seller_id,amount_xaf,method,status) values (?,?,?, ?,?,'BANK_DEPOSIT',?)", id, "RM-" + id, "s-me-1", seller, amount, status);
  }

  @Test void countsOnlyTheSellersOwnItems() {
    var mine = cash.sellerOverview("me");
    assertEquals(1L, ((Number) mine.get("pending_outflows")).longValue());
    assertEquals(2L, ((Number) mine.get("outstanding_remittances")).longValue());
    assertEquals(30000L, ((Number) mine.get("outstanding_remittances_xaf")).longValue());
    assertEquals(1L, ((Number) mine.get("disputed_remittances")).longValue());
    assertEquals(1L, ((Number) mine.get("recounts_requested")).longValue());
  }

  @Test void aSellerWithNothingGetsZeros() {
    var none = cash.sellerOverview("nobody");
    assertEquals(0L, ((Number) none.get("pending_outflows")).longValue());
    assertEquals(0L, ((Number) none.get("outstanding_remittances_xaf")).longValue());
  }
}
