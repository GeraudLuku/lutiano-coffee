package com.lutiano.api.repository.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import java.time.LocalDate;
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
import com.lutiano.api.service.campaign.CampaignService;

/** The highest tier reached pays; sellers only see campaigns that reach them; a campaign is settled once. */
@Testcontainers
class CampaignRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static CampaignRepository repo;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new CampaignRepository(db);
    db.update("insert into staff_user (id,email,password_hash,full_name,role,active) values ('s1','s1@x.local','x','S One','SELLER',true),('s2','s2@x.local','x','S Two','SELLER',true),('a1','a1@x.local','x','Admin','ADMIN',true)");
    db.update("insert into operation (id,name,code,type,active,qr_token) values ('op1','Cafe One','C1','FIXED_POS',true,'t1'),('op2','Cafe Two','C2','FIXED_POS',true,'t2')");
    db.update("insert into seller_operation (seller_id,operation_id) values ('s1','op1')");
  }

  private static List<Map<String, Object>> tiers() {
    return List.of(Map.of("threshold_xaf", 100_000L, "bonus_xaf", 5_000L), Map.of("threshold_xaf", 300_000L, "bonus_xaf", 20_000L));
  }

  @Test void theHighestTierReachedPaysAndTiersDoNotAddUp() {
    assertEquals(0, CampaignService.bonusFor(tiers(), 99_999));
    assertEquals(5_000, CampaignService.bonusFor(tiers(), 100_000));
    assertEquals(5_000, CampaignService.bonusFor(tiers(), 299_999));
    assertEquals(20_000, CampaignService.bonusFor(tiers(), 900_000));
  }

  @Test void aSellerSeesCampaignsForEveryoneAndForTheirOwnLocationOnly() {
    LocalDate today = LocalDate.now();
    repo.insert("c-all", "For all", null, today, today.plusDays(5), null, "a1");
    repo.insert("c-op1", "Cafe One only", null, today, today.plusDays(5), "op1", "a1");
    repo.insert("c-op2", "Cafe Two only", null, today, today.plusDays(5), "op2", "a1");
    repo.insert("c-old", "Last month", null, today.minusDays(40), today.minusDays(10), null, "a1");
    var mine = repo.page(null, "s1", today, PageRequest.of(1, 50)).items().stream().map(r -> r.get("id")).toList();
    assertTrue(mine.contains("c-all") && mine.contains("c-op1") && mine.contains("c-old"));
    assertFalse(mine.contains("c-op2"), "another location's campaign stays hidden");
    var ended = repo.page("ENDED", null, today, PageRequest.of(1, 50)).items().stream().map(r -> r.get("id")).toList();
    assertEquals(List.of("c-old"), ended);
    assertTrue(repo.reaches("s1", repo.find("c-op1").orElseThrow()));
    assertFalse(repo.reaches("s2", repo.find("c-op1").orElseThrow()));
  }

  @Test void aCampaignIsSettledOnlyOnceAndCancelledOnlyBeforeThat() {
    LocalDate today = LocalDate.now();
    repo.insert("c-set", "Settle me", null, today.minusDays(5), today.minusDays(1), null, "a1");
    assertTrue(repo.markSettled("c-set"));
    assertFalse(repo.markSettled("c-set"));
    assertFalse(repo.cancel("c-set"), "a settled campaign cannot be cancelled");
    repo.insert("c-cancel", "Cancel me", null, today, today.plusDays(2), null, "a1");
    assertTrue(repo.cancel("c-cancel"));
    assertFalse(repo.cancel("c-cancel"));
  }

  @Test void standingsListEveryEligibleSellerEvenWithoutSales() {
    LocalDate today = LocalDate.now();
    repo.insert("c-st", "Standings", null, today, today.plusDays(2), "op1", "a1");
    var page = repo.standings(repo.find("c-st").orElseThrow(), PageRequest.of(1, 50));
    assertEquals(1, page.total());
    assertEquals("s1", page.items().get(0).get("seller_id"));
    assertEquals(0L, ((Number) page.items().get(0).get("sales_xaf")).longValue());
  }
}
