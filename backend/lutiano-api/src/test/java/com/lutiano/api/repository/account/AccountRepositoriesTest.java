package com.lutiano.api.repository.account;

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

/** Support requests belong to their sender, the team sees all, and a phone change waits for its code. */
@Testcontainers
class AccountRepositoriesTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static SupportRepository support;
  static AccountRepository account;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    support = new SupportRepository(db);
    account = new AccountRepository(db);
    for (String u : List.of("me", "other", "admin"))
      db.update("insert into staff_user (id,email,password_hash,full_name,role,active,phone) values (?,?,'x',?,?,true,?)", u, u + "@x.local", u, u.equals("admin") ? "ADMIN" : "SELLER", u.equals("me") ? "+237690000001" : null);
  }

  @Test void aCustomerRequestShowsTheCustomersNameAndCanBeFilteredByWhoSentIt() {
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code) values('cust-s','CU-S1','Ada Customer','+237655300001',true,'ACTIVE','ts1','BRONZE')");
    String id = support.insert("SP-2026-00009", "cust-s", "CUSTOMER", "ORDER", "Order is late", "Still waiting", null, null, null);
    assertEquals("Ada Customer", support.find(id).orElseThrow().get("user_name"));
    assertEquals("CUSTOMER", support.find(id).orElseThrow().get("requester_type"));
    assertTrue(support.all(null, "CUSTOMER", PageRequest.of(1, 50)).items().stream().allMatch(r -> "CUSTOMER".equals(r.get("requester_type"))));
    assertEquals(1, support.ofUser("cust-s", null, PageRequest.of(1, 10)).total());
  }

  @Test void aSellerSeesOnlyTheirOwnRequestsAndTheTeamSeesAllWithCounts() {
    String mine = support.insert("SP-2026-00001", "me", "STAFF", "ISSUE", "Cannot record a sale", "It fails", null, null, null);
    support.insert("SP-2026-00002", "other", "STAFF", "QUESTION", "How do I…", "?", "support/other/f1", "shot.png", "image/png");
    assertEquals(1, support.ofUser("me", null, PageRequest.of(1, 10)).total());
    var all = support.all(null, null, PageRequest.of(1, 10));
    assertEquals(2, all.total());
    assertEquals(2L, ((Number) all.summary().get("open_count")).longValue());
    assertTrue(support.attachment(mine).isEmpty(), "no file, no attachment");
    assertTrue(support.find(mine).orElseThrow().get("reply") == null);
  }

  @Test void aReplySetsTheTextTheStatusAndWhoAnswered() {
    String id = support.insert("SP-2026-00003", "me", "STAFF", "ACCOUNT", "Change my area", "Please", null, null, null);
    support.reply(id, "RESOLVED", "Done, it is changed.", "admin");
    var r = support.find(id).orElseThrow();
    assertEquals("RESOLVED", r.get("status"));
    assertEquals("Done, it is changed.", r.get("reply"));
    assertEquals("admin", r.get("replied_by_name"));
    support.reply(id, "OPEN", null, "admin");
    assertEquals("Done, it is changed.", support.find(id).orElseThrow().get("reply"), "changing only the status keeps the reply");
  }

  @Test void aPhoneChangeWaitsForItsCodeAndNumbersAreNotShared() {
    account.savePendingPhone("me", "+237690000009");
    assertEquals("+237690000009", account.pendingPhone("me").orElseThrow());
    db.update("update phone_change set created_at=date_sub(UTC_TIMESTAMP(), interval 2 hour) where user_id='me'");
    assertTrue(account.pendingPhone("me").isEmpty(), "an old request expires");
    assertTrue(account.phoneTakenByOther("+237690000001", "other"));
    assertFalse(account.phoneTakenByOther("+237690000001", "me"));
    account.setPhone("other", "+237690000002");
    assertTrue(account.phoneTakenByOther("+237690000002", "me"));
  }

  @Test void activityListsOnlyTheirOwnSignInsAndSecurityChanges() {
    db.update("insert into audit_event (id,actor_id,action,entity_type,entity_id,details,created_at) values ('e1','me','SIGNED_IN','staff_user','me','Seller app',UTC_TIMESTAMP())");
    db.update("insert into audit_event (id,actor_id,action,entity_type,entity_id,details,created_at) values ('e2','me','SALE_RECORDED','sale','s','x',UTC_TIMESTAMP())");
    db.update("insert into audit_event (id,actor_id,action,entity_type,entity_id,details,created_at) values ('e3','other','SIGNED_IN','staff_user','other','Chrome',UTC_TIMESTAMP())");
    var page = account.activity("me", List.of("SIGNED_IN", "PASSWORD_CHANGED"), PageRequest.of(1, 10));
    assertEquals(1, page.total());
    assertEquals("Seller app", page.items().get(0).get("client"));
  }
}
