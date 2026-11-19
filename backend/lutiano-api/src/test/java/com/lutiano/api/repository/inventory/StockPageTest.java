package com.lutiano.api.repository.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The seller's stock list is paged, filterable, and carries totals for the whole operation. */
@Testcontainers
class StockPageTest {
  @Container static MariaDBContainer<?> maria = new MariaDBContainer<>("mariadb:11.4");
  static InventoryRepository inventory;
  static final String OP = "op-stock";

  @BeforeAll static void setUp() {
    Flyway.configure().dataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()).load().migrate();
    JdbcTemplate db = new JdbcTemplate(new DriverManagerDataSource(maria.getJdbcUrl(), maria.getUsername(), maria.getPassword()));
    inventory = new InventoryRepository(db, new SearchOutbox(db));
    db.update("insert into product_category (id,name) values ('c1','Hot'),('c2','Cold')");
    // 12 sellable products: quantities 0 (out), 2 (low, threshold 5) and 50 (healthy)
    for (int i = 1; i <= 12; i++) {
      String id = "p" + String.format("%02d", i);
      db.update("insert into product (id,sku,name,category,price_xaf,low_stock_threshold,active,track_stock) values (?,?,?,?,1000,5,true,true)",
          id, "SKU" + i, (i % 2 == 0 ? "Latte " : "Mocha ") + i, i <= 6 ? "Hot" : "Cold");
      int qty = i <= 2 ? 0 : (i <= 4 ? 2 : 50);
      if (qty > 0) db.update("insert into inventory_balance (operation_id,product_id,quantity) values (?,?,?)", OP, id, qty);
    }
  }

  @Test void pagesTheWholeListWithoutLosingOrRepeatingRows() {
    var first = inventory.operationBalancesPage(OP, null, null, null, null, false, PageRequest.of(1, 5));
    var second = inventory.operationBalancesPage(OP, null, null, null, null, false, PageRequest.of(2, 5));
    var third = inventory.operationBalancesPage(OP, null, null, null, null, false, PageRequest.of(3, 5));
    assertTrue(first.total() >= 12);
    assertEquals(5, first.items().size());
    assertEquals(first.pages(), (int) Math.ceil(first.total() / 5.0));
    var ids = new java.util.HashSet<Object>();
    for (var page : List.of(first, second, third)) for (var r : page.items()) assertTrue(ids.add(r.get("product_id")), "a row appeared twice");
  }

  @Test void filtersByTextCategoryLevelAndIds() {
    assertEquals(6, inventory.operationBalancesPage(OP, "Latte", null, null, null, false, PageRequest.of(1, 50)).items().stream().filter(r -> ((String) r.get("name")).startsWith("Latte")).count());
    var cold = inventory.operationBalancesPage(OP, null, "Cold", null, List.of(), false, PageRequest.of(1, 50));
    assertTrue(cold.items().stream().allMatch(r -> "Cold".equals(r.get("category"))));
    assertEquals(2, inventory.operationBalancesPage(OP, null, null, "out", List.of("p01", "p02", "p03", "p05"), false, PageRequest.of(1, 50)).total());
    assertEquals(1, inventory.operationBalancesPage(OP, null, null, "low", List.of("p03", "p05"), false, PageRequest.of(1, 50)).total());
    var byIds = inventory.operationBalancesPage(OP, null, null, null, List.of("p05", "p06"), false, PageRequest.of(1, 50));
    assertEquals(2, byIds.total());
  }

  @Test void summaryDescribesTheWholeOperationWhateverTheFilter() {
    var filtered = inventory.operationBalancesPage(OP, "Latte", "Hot", null, null, false, PageRequest.of(1, 2));
    var all = inventory.operationBalancesPage(OP, null, null, null, null, false, PageRequest.of(1, 2));
    assertEquals(all.summary(), filtered.summary());
    assertEquals(((Number) all.summary().get("products")).longValue(), all.total());
    var rows = inventory.operationBalancesPage(OP, null, null, null, null, false, PageRequest.of(1, 200)).items();   // the demo products are in the list too
    long units = rows.stream().mapToLong(r -> Math.max(0, ((Number) r.get("quantity")).longValue())).sum();
    long attention = rows.stream().filter(r -> ((Number) r.get("quantity")).longValue() <= ((Number) r.get("low_stock_threshold")).longValue()).count();
    assertEquals(units, ((Number) all.summary().get("units")).longValue());
    assertEquals(attention, ((Number) all.summary().get("attention")).longValue());
    assertTrue(((List<?>) all.summary().get("categories")).containsAll(List.of("Cold", "Hot")));
  }
}
