package com.lutiano.api.repository.refund;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutiano.api.repository.payout.PayoutRepository;
import com.lutiano.api.repository.search.SearchOutbox;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Refund and payout history rows are written by their repositories, with long free text cut to the column width. */
@Testcontainers
class EventRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");

  @Test void eventsAreStoredWithGeneratedIdsAndClippedDetails() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    SearchOutbox outbox = new SearchOutbox(db);
    new RefundRepository(db, outbox).insertEvent("r1", "REQUESTED", null, "REQUESTED", "a", "x".repeat(600));
    new PayoutRepository(db, outbox).insertEvent("p1", "REQUESTED", null, "REQUESTED", "a", null);
    assertEquals(500, db.queryForObject("select char_length(details) from refund_event where refund_id='r1'", Integer.class));
    assertEquals(36, db.queryForObject("select char_length(id) from refund_event where refund_id='r1'", Integer.class));
    assertEquals(1, db.queryForObject("select count(*) from payout_event where payout_id='p1' and details is null", Integer.class));
  }
}
