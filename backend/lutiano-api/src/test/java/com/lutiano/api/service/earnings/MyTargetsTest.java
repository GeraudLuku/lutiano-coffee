package com.lutiano.api.service.earnings;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.repository.search.SearchOutbox;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.repository.earnings.EarningsRepository;

/** What a bonus is called at each stage, and which targets a seller can list for a month or a state. */
@Testcontainers
class MyTargetsTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static EarningsRepository repo;
  static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

  static Map<String, Object> target(long bonus, Object paid, boolean reversed, boolean reached, String state) {
    Map<String, Object> t = new HashMap<>();
    t.put("bonus_xaf", bonus); t.put("bonus_paid_xaf", paid); t.put("bonus_reversed", reversed); t.put("reached", reached); t.put("state", state);
    return t;
  }

  @Test void bonusStatesFollowTheLifeOfABonus() {
    assertEquals("NONE", MyTargetsService.bonusState(target(0, null, false, true, "ENDED")), "a target without a bonus has none");
    assertEquals("IN_PROGRESS", MyTargetsService.bonusState(target(50000, null, false, false, "CURRENT")));
    assertEquals("NOT_REACHED", MyTargetsService.bonusState(target(50000, null, false, false, "ENDED")));
    assertEquals("PENDING", MyTargetsService.bonusState(target(50000, null, false, true, "CURRENT")), "reached, not yet in a finalised month");
    assertEquals("INCLUDED", MyTargetsService.bonusState(target(50000, 50000L, false, true, "ENDED")));
    assertEquals("REVERSED", MyTargetsService.bonusState(target(50000, 50000L, true, false, "ENDED")), "taken back wins over included");
  }

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new EarningsRepository(db, new References(db), new SearchOutbox(db));
    for (String u : List.of("me", "other")) db.update("insert into staff_user (id,email,password_hash,full_name,role,active) values (?,?,'x',?,'SELLER',true)", u, u + "@x.local", u);
    insert(db, "t-oct", "me", "October", "2026-10-01", "2026-10-31", "ACTIVE");
    insert(db, "t-nov", "me", "November", "2026-11-01", "2026-11-30", "ACTIVE");
    insert(db, "t-dec", "me", "December", "2026-12-01", "2026-12-31", "ACTIVE");
    insert(db, "t-cancelled", "me", "Cancelled", "2026-11-01", "2026-11-30", "CANCELLED");
    insert(db, "t-other", "other", "Someone else's", "2026-11-01", "2026-11-30", "ACTIVE");
  }

  static void insert(JdbcTemplate db, String id, String seller, String name, String from, String to, String status) {
    db.update("insert into seller_target (id,seller_id,name,metric,period_type,starts_on,ends_on,target_value,bonus_type,bonus_xaf,status) values (?,?,?,'VALUE','MONTHLY',?,?,1000000,'FIXED',50000,?)", id, seller, name, from, to, status);
  }

  @Test void listsOnlyTheSellersOwnActiveTargetsByStateAndMonth() {
    var all = repo.activeTargetsOf("me", null, LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31), TODAY, PageRequest.of(1, 10));
    assertEquals(3, all.total(), "cancelled and other sellers' targets never show");
    assertEquals("December", all.items().get(0).get("name"), "newest first");
    assertEquals(1, repo.activeTargetsOf("me", "CURRENT", LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31), TODAY, PageRequest.of(1, 10)).total());
    assertEquals(1, repo.activeTargetsOf("me", "UPCOMING", LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31), TODAY, PageRequest.of(1, 10)).total());
    assertEquals(1, repo.activeTargetsOf("me", "ENDED", LocalDate.of(1970, 1, 1), LocalDate.of(2999, 12, 31), TODAY, PageRequest.of(1, 10)).total());
    var october = repo.activeTargetsOf("me", null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), TODAY, PageRequest.of(1, 10));
    assertEquals(1, october.total());
    assertEquals(2, repo.activeTargetsOverlapping("me", LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15)).size(), "a target overlaps a range it only partly covers");
  }
}
