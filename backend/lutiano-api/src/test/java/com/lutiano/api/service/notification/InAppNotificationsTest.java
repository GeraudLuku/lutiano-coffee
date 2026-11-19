package com.lutiano.api.service.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.Actor;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.lutiano.api.repository.notification.AppNotificationRepository;
import com.lutiano.api.repository.notification.DeviceTokenRepository;
import com.lutiano.api.proxy.PushSender;
import com.lutiano.api.config.AppProperties;

/** A seller's in-app notifications: what is recorded, what the preferences switch off, and read / clear / filter. */
@Testcontainers
class InAppNotificationsTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static JdbcTemplate db;
  static AppNotificationRepository repo;
  static InAppNotifications service;

  static Actor actor(String id) { return new Actor(id, id + "@x.local", id, "SELLER"); }

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    repo = new AppNotificationRepository(db);
    service = new InAppNotifications(repo, new PushSender(new DeviceTokenRepository(db), new org.springframework.core.task.SyncTaskExecutor(), new com.lutiano.api.config.AppProperties.Push("")), new DeviceTokenRepository(db));
  }

  @Test void theBrandPrefixOfAnSmsTextIsDropped() {
    assertEquals("votre demande est approuvée", InAppNotifications.stripBrand("LUTIANO : votre demande est approuvée"));
    assertEquals("your request is approved", InAppNotifications.stripBrand("LUTIANO: your request is approved"));
    assertEquals("no prefix here", InAppNotifications.stripBrand("no prefix here"));
  }

  @Test void eventsAreRecordedWithTheirCategoryAndImportanceButCodesNever() {
    service.record("rec1", NotificationCatalog.STOCK_REQUEST_APPROVED, "LUTIANO: your stock request SR-1 is approved", "en", "stock_request", "11111111-1111-1111-1111-111111111111");
    service.record("rec1", NotificationCatalog.OTP_SIGN_IN, "LUTIANO: your code is 123456", "fr", null, null);
    var page = service.list(actor("rec1"), null, null, null, PageRequest.of(1, 10));
    assertEquals(1, page.total(), "a one-time code never reaches the app");
    var n = page.items().get(0);
    assertEquals("REQUESTS", n.get("category"));
    assertEquals("your stock request SR-1 is approved", n.get("body"));
    assertEquals("stock_request", n.get("related_type"));
  }

  @Test void aSwitchedOffCategoryIsNotRecordedUnlessItCarriesMoney() {
    service.savePreference(actor("rec2"), "INVENTORY", false, null);
    service.record("rec2", NotificationCatalog.STOCK_LOW, "LUTIANO: low stock", "fr", null, null);
    service.record("rec2", NotificationCatalog.STOCK_REQUEST_REJECTED, "LUTIANO: rejected", "fr", null, null);
    assertEquals(1, service.list(actor("rec2"), null, null, null, PageRequest.of(1, 10)).total(), "inventory is off, requests are on");
    assertThrows(ResponseStatusException.class, () -> service.savePreference(actor("rec2"), "EARNINGS", false, null), "payouts cannot be switched off");
    var prefs = service.preferences(actor("rec2"));
    assertEquals(false, prefs.stream().filter(p -> p.get("category").equals("INVENTORY")).findFirst().orElseThrow().get("in_app"));
    assertEquals(true, prefs.stream().filter(p -> p.get("category").equals("EARNINGS")).findFirst().orElseThrow().get("locked"));
  }

  @Test void announcementsReachEveryoneWhenImportantAndOtherwiseRespectSystemOff() {
    service.savePreference(actor("rec3"), "SYSTEM", false, null);
    service.announce("rec3", "SYSTEM", "Menu change", "New drinks", false, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    assertEquals(0, service.list(actor("rec3"), null, null, null, PageRequest.of(1, 10)).total());
    service.announce("rec3", "SYSTEM", "Closed Monday", "Shops are closed", true, "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    var list = service.list(actor("rec3"), null, null, null, PageRequest.of(1, 10));
    assertEquals(1, list.total());
    assertEquals("Closed Monday", list.items().get(0).get("title"));
  }

  @Test void filtersCountsReadAndClear() {
    for (int i = 0; i < 3; i++) service.record("rec4", NotificationCatalog.STOCK_LOW, "low " + i, "fr", null, null);
    service.record("rec4", NotificationCatalog.CASH_HANDOVER, "handover", "fr", null, null);
    var all = service.list(actor("rec4"), null, null, null, PageRequest.of(1, 10));
    assertEquals(4L, ((Number) all.summary().get("total")).longValue());
    assertEquals(4L, ((Number) all.summary().get("unread")).longValue());
    @SuppressWarnings("unchecked") var cats = (Map<String, Map<String, Object>>) all.summary().get("categories");
    assertEquals(3L, ((Number) cats.get("INVENTORY").get("total")).longValue());
    assertEquals(1, service.list(actor("rec4"), "OPERATIONS", null, null, PageRequest.of(1, 10)).total());
    var first = (String) all.items().get(0).get("id");
    assertEquals(1, service.markRead(actor("rec4"), List.of(first)).get("changed"));
    assertEquals(3, service.list(actor("rec4"), null, "UNREAD", null, PageRequest.of(1, 10)).total());
    assertEquals(4L, ((Number) service.list(actor("rec4"), null, "UNREAD", null, PageRequest.of(1, 1)).summary().get("total")).longValue(), "counts ignore the filter");
    assertEquals(1, service.clear(actor("rec4"), null).get("changed"), "clearing with no ids hides what was read");
    assertEquals(3, service.list(actor("rec4"), null, null, null, PageRequest.of(1, 10)).total());
    assertEquals(3L, ((Number) service.unread(actor("rec4")).get("unread")).longValue());
    assertEquals(3, service.markRead(actor("rec4"), null).get("changed"), "no ids marks everything");
    assertTrue(service.list(actor("rec4"), null, "IMPORTANT", null, PageRequest.of(1, 10)).total() > 0, "essential events are important");
  }

  @Test void noOneReadsAnotherPersonsNotification() {
    service.record("rec5", NotificationCatalog.STOCK_LOW, "mine", "fr", null, null);
    String id = (String) service.list(actor("rec5"), null, null, null, PageRequest.of(1, 1)).items().get(0).get("id");
    assertThrows(ResponseStatusException.class, () -> service.get(actor("someone-else"), id));
    assertEquals(0, service.markRead(actor("someone-else"), List.of(id)).get("changed"));
  }

  @Test void clearedNotificationsAreRemovedAfterNinetyDays() {
    service.record("rec6", NotificationCatalog.STOCK_LOW, "old", "fr", null, null);
    service.record("rec6", NotificationCatalog.STOCK_LOW, "recent", "fr", null, null);
    db.update("update app_notification set read_at=UTC_TIMESTAMP(), cleared_at=date_sub(UTC_TIMESTAMP(), interval 100 day) where recipient_id='rec6' and body='old'");
    db.update("update app_notification set read_at=UTC_TIMESTAMP(), cleared_at=date_sub(UTC_TIMESTAMP(), interval 10 day) where recipient_id='rec6' and body='recent'");
    assertEquals(1, repo.purgeCleared(90));
    assertEquals(1, db.queryForObject("select count(*) from app_notification where recipient_id='rec6'", Integer.class));
  }
}
