package com.lutiano.api.repository.product;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import java.util.ArrayList;
import java.util.HashMap;
import com.lutiano.api.definitions.enums.SearchType;
import com.lutiano.api.entity.PricedAddon;

/** Catalogue settings: product categories and add-ons. */
@Repository
public class CatalogRepository {
  private static final String CATEGORY_SELECT =
      "select c.*, (select count(*) from product p where p.category=c.name and p.kind<>'VARIANT') product_count from product_category c";
  private static final String ADDON_SELECT = "select a.*, (select count(*) from product_addon pa where pa.addon_id=a.id) product_count from addon a";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public CatalogRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  // ----- categories -----

  public List<Map<String, Object>> categories() { return db.queryForList(CATEGORY_SELECT + " order by c.sort_order, c.name"); }

  public Optional<Map<String, Object>> category(String id) { return db.queryForList(CATEGORY_SELECT + " where c.id=?", id).stream().findFirst(); }

  /** The stored name of an active category matching {@code name} case-insensitively. */
  public Optional<String> activeCategoryName(String name) {
    return db.queryForList("select name from product_category where lower(name)=lower(?) and active=true", String.class, name).stream().findFirst();
  }

  public boolean categoryExists(String name) {
    Integer n = db.queryForObject("select count(*) from product_category where name=?", Integer.class, name);
    return n != null && n > 0;
  }

  public void insertCategory(String id, String name, String description, int sortOrder, boolean active) {
    db.update("insert into product_category(id,name,description,sort_order,active,created_at) values(?,?,?,?,?,UTC_TIMESTAMP())",
        id, name, description, sortOrder, active);
    search.changed(SearchType.CATEGORY, id);
  }

  public void setCategoryImage(String id, String url) { db.update("update product_category set image_url=? where id=?", url, id); }

  public void updateCategory(String id, String name, String description, int sortOrder, boolean active) {
    db.update("update product_category set name=?,description=?,sort_order=?,active=? where id=?", name, description, sortOrder, active, id);
    search.changed(SearchType.CATEGORY, id);
    // products embed the category name, and a rename cascades to them in the database
    search.changedAll(SearchType.PRODUCT, db.queryForList("select p.id from product p join product_category c on c.name=p.category where c.id=? and p.kind <> 'VARIANT'", String.class, id));
  }

  // ----- add-ons -----

  /** Every add-on with the products that offer it ({@code products}: id and name, by name). */
  public List<Map<String, Object>> addons() {
    List<Map<String, Object>> rows = db.queryForList(ADDON_SELECT + " order by a.sort_order, a.name");
    Map<Object, List<Map<String, Object>>> offered = new HashMap<>();
    for (Map<String, Object> l : db.queryForList("select pa.addon_id, p.id, p.name from product_addon pa join product p on p.id=pa.product_id order by p.name, p.id"))
      offered.computeIfAbsent(l.remove("addon_id"), k -> new ArrayList<>()).add(l);
    for (Map<String, Object> a : rows) a.put("products", offered.getOrDefault(a.get("id"), List.of()));
    return rows;
  }

  public Optional<Map<String, Object>> addon(String id) { return db.queryForList(ADDON_SELECT + " where a.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> addonsForProduct(String productId) {
    return db.queryForList("select a.* from addon a join product_addon pa on pa.addon_id=a.id where pa.product_id=? order by a.sort_order,a.name", productId);
  }

  public List<Map<String, Object>> activeAddons() {
    return db.queryForList("select id,name,price_xaf,sort_order,image_url from addon where active=true order by sort_order,name");
  }

  /** Active sellable products an add-on applies to (a parent's link covers its variants). */
  public List<String> addonProductIds(Object addonId) {
    return db.queryForList("select p.id from product_addon pa join product p on (p.id=pa.product_id and p.kind='SIMPLE') or (p.parent_id=pa.product_id)" +
        " where pa.addon_id=? and p.active=true", String.class, addonId);
  }

  /** An active add-on linked to {@code ownerProductId}, as priced now. */
  public Optional<PricedAddon> pricedAddon(String addonId, String ownerProductId) {
    return db.query("select a.id,a.name,a.price_xaf from addon a join product_addon pa on pa.addon_id=a.id where a.id=? and pa.product_id=? and a.active=true",
        (rs, n) -> new PricedAddon(rs.getString(1), rs.getString(2), rs.getLong(3)), addonId, ownerProductId).stream().findFirst();
  }

  public void insertAddon(String id, String name, long price, int sortOrder, boolean active) {
    db.update("insert into addon(id,name,price_xaf,sort_order,active,created_at) values(?,?,?,?,?,UTC_TIMESTAMP())", id, name, price, sortOrder, active);
    search.changed(SearchType.ADDON, id);
  }

  public void setAddonImage(String id, String url) { db.update("update addon set image_url=? where id=?", url, id); }

  public void updateAddon(String id, String name, long price, int sortOrder, boolean active) {
    db.update("update addon set name=?,price_xaf=?,sort_order=?,active=? where id=?", name, price, sortOrder, active, id);
    search.changed(SearchType.ADDON, id);
  }
}
