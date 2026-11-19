package com.lutiano.api.repository.loyalty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.service.loyalty.WalletService;

/** A wallet reward is available until used or out of date; staff redemptions made before the wallet existed count as used. */
@Testcontainers
class WalletRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static WalletRepository repo;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new WalletRepository(db);
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code) values('w1','CU-W1','Ada','+237655200001',true,'ACTIVE','wt1','BRONZE')");
    db.update("insert into reward(id,name,reward_type,point_cost,requires_stock,active) values('rw1','Free Latte','FREE_PRODUCT',500,false,true)");
  }

  private static void redemption(String id, String ref) {
    db.update("insert into reward_redemption(id,reference,customer_id,reward_id,reward_name,points,status,created_by) values(?,?, 'w1','rw1','Free Latte',500,'COMPLETED','w1')", id, ref);
  }

  @Test void aStaffRedemptionIsAlreadyUsedAndAWalletOneIsAvailable() {
    redemption("x1", "RWD-1");
    assertEquals("USED", WalletService.state(db.queryForMap("select * from reward_redemption where id='x1'")));
    redemption("x2", "RWD-2");
    repo.makeWalletItem("x2", "tok-2", Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS)));
    assertEquals("AVAILABLE", WalletService.state(db.queryForMap("select * from reward_redemption where id='x2'")));
    assertEquals(1, repo.availableCount("w1"));
  }

  @Test void usingMarksItUsedAndItsTokenFindsIt() {
    redemption("x3", "RWD-3");
    repo.makeWalletItem("x3", "tok-3", Timestamp.from(Instant.now().plus(5, ChronoUnit.DAYS)));
    Map<String, Object> found = repo.lockByToken("tok-3").orElseThrow();
    assertEquals("x3", found.get("id"));
    repo.markUsed("x3", "seller", "op");
    assertEquals("USED", WalletService.state(repo.lockByToken("tok-3").orElseThrow()));
    assertEquals(1, repo.ofCustomer("w1", "USED", PageRequest.of(1, 50)).items().stream().filter(r -> "x3".equals(r.get("id"))).count());
  }

  @Test void anOutOfDateRewardIsExpiredAndFoundByTheSweep() {
    redemption("x4", "RWD-4");
    repo.makeWalletItem("x4", "tok-4", Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)));
    assertEquals("EXPIRED", WalletService.state(repo.lockByToken("tok-4").orElseThrow()));
    assertTrue(repo.due().stream().anyMatch(r -> "x4".equals(r.get("id"))));
    assertTrue(repo.expire("x4"));
    assertFalse(repo.expire("x4"), "only once");
    assertTrue(repo.ofCustomer("w1", "EXPIRED", PageRequest.of(1, 50)).items().stream().anyMatch(r -> "x4".equals(r.get("id"))));
  }
}
