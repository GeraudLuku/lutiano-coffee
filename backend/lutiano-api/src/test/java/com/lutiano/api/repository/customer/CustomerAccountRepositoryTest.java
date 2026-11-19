package com.lutiano.api.repository.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.security.CurrentCustomer;
import java.sql.Timestamp;
import java.time.Instant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** A customer record is claimed by its owner, deleted accounts keep their history but not their identity, and a session ends when it should. */
@Testcontainers
class CustomerAccountRepositoryTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static CustomerAccountRepository repo;
  static CurrentCustomer current;

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new CustomerAccountRepository(db, new SearchOutbox(db));
    current = new CurrentCustomer(db);
  }

  private static String create(String id, String phone) {
    repo.insertRegistered(id, "CU-T" + id.hashCode(), "Name " + id, phone, id + "@x.local", "hash", "fr");
    return id;
  }

  @Test void anAdministratorCreatedRecordIsClaimedWithItsPoints() {
    db.update("insert into customer(id,code,full_name,phone,phone_verified,status,loyalty_token,tier_code) values('claim-1','CU-C1','Old Name','+237655000001',false,'ACTIVE','tok-claim','GOLD')");
    assertNull(repo.byPhone("+237655000001").orElseThrow().get("registered_at"));
    repo.claim("claim-1", "New Name", "c1@x.local", "hash", "en");
    var c = repo.byPhone("+237655000001").orElseThrow();
    assertNotNull(c.get("registered_at"));
    assertEquals("GOLD", c.get("tier_code"), "the record keeps its tier and history");
    assertEquals("New Name", c.get("full_name"));
  }

  @Test void phoneAndEmailAreUnique() {
    create("u1", "+237655000011");
    assertThrows(DuplicateKeyException.class, () -> create("u2", "+237655000011"));
    assertTrue(repo.emailTaken("u1@x.local", null));
    assertFalse(repo.emailTaken("u1@x.local", "u1"), "the owner's own email is not 'taken'");
  }

  @Test void deletingAnAccountRemovesWhoTheyAreButNotTheirSales() {
    create("del-1", "+237655000021");
    repo.addIdentity("GOOGLE", "g-1", "del-1", "del-1@x.local");
    repo.addFavourite("del-1", "any-product");
    db.update("insert into device_token(token,user_id,platform) values('dev-del','del-1','ANDROID')");
    repo.anonymise("del-1");
    assertTrue(repo.byId("del-1").isEmpty(), "a deleted account is no longer found");
    assertEquals(0, db.queryForObject("select count(*) from customer_identity where customer_id='del-1'", Integer.class));
    assertEquals(0, db.queryForObject("select count(*) from device_token where user_id='del-1'", Integer.class));
    assertEquals(1, db.queryForObject("select count(*) from customer where id='del-1' and deleted_at is not null and email is null", Integer.class), "the row stays for the accounts");
    create("del-2", "+237655000021");   // the number is free again
  }

  @Test void aGoogleOrAppleAccountBelongsToOneCustomer() {
    create("id-1", "+237655000031");
    create("id-2", "+237655000032");
    repo.addIdentity("APPLE", "a-1", "id-1", null);
    assertEquals("id-1", repo.customerOfIdentity("APPLE", "a-1").orElseThrow());
    assertThrows(DuplicateKeyException.class, () -> repo.addIdentity("APPLE", "a-1", "id-2", null));
    assertEquals(1, repo.removeIdentity("id-1", "APPLE"));
    assertTrue(repo.customerOfIdentity("APPLE", "a-1").isEmpty());
  }

  @Test void favouritesListOnlySellableProducts() {
    db.update("insert into product_category(id,name,active) values('cat-f','FavCat',true)");
    db.update("insert into product(id,sku,name,price_xaf,active,category,kind) values('fp-1','FP1','Latte',2000,true,'FavCat','SIMPLE'),('fp-2','FP2','Hidden',2000,false,'FavCat','SIMPLE'),('fp-3','FP3','Size',2000,true,'FavCat','VARIANT')");
    create("fav-1", "+237655000041");
    assertTrue(repo.favouritable("fp-1"));
    assertFalse(repo.favouritable("fp-2"), "inactive");
    assertFalse(repo.favouritable("fp-3"), "a size variant hangs under its parent");
    repo.addFavourite("fav-1", "fp-1");
    repo.addFavourite("fav-1", "fp-1");   // twice is the same heart
    assertEquals(1, repo.favourites("fav-1", PageRequest.of(1, 10)).total());
  }

  @Test void aSessionEndsWhenTheAccountIsSuspendedDeletedOrSignedOutEverywhere() {
    create("sess-1", "+237655000051");
    var token = new TestingAuthenticationToken("sess-1", "x", "ROLE_CUSTOMER");
    assertEquals("CUSTOMER", current.get(token).role());
    db.update("update customer set status='SUSPENDED' where id='sess-1'");
    assertEquals(401, assertThrows(ResponseStatusException.class, () -> current.get(token)).getStatusCode().value());
    db.update("update customer set status='ACTIVE' where id='sess-1'");
    repo.revokeSessions("sess-1", Timestamp.from(Instant.now().plusSeconds(60)));
    assertEquals(401, assertThrows(ResponseStatusException.class, () -> current.get(token)).getStatusCode().value(), "a token without an issue time or from before the revoke");
  }
}
