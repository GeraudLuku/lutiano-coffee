package com.lutiano.api.repository.catalogue;

import java.util.ArrayList;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * What the Customer app may read without signing in: the menu (categories, products with their sizes and add-ons) and the sales
 * locations. Only active records, and nothing about stock quantities, costs or staff.
 */
@Repository
public class PublicCatalogueRepository {
  /** A product shown on the menu: not a size variant, active, and (for a group of sizes) with at least one active size. */
  private static final String MENU = "select p.id,p.name,p.category,p.description,p.image_url,p.kind," +
      " case when p.kind='PARENT' then (select min(v.price_xaf) from product v where v.parent_id=p.id and v.active=true) else p.price_xaf end price_xaf" +
      " from product p";
  private static final String MENU_RULE = "p.kind<>'VARIANT' and p.active=true and (p.kind<>'PARENT' or exists (select 1 from product v where v.parent_id=p.id and v.active=true))" +
      " and exists (select 1 from product_category c where c.name=p.category and c.active=true)";
  private static final String OPERATION = "select o.id,o.name,o.code,o.type,o.address,o.description,o.image_url,o.city,o.region,o.latitude,o.longitude from operation o";
  private final JdbcTemplate db;

  public PublicCatalogueRepository(JdbcTemplate db) { this.db = db; }

  public PageResponse<Map<String, Object>> categories(PageRequest req) {
    return new SqlFilter("select c.id,c.name,c.description,c.image_url from product_category c").and("c.active=true").page(db, "order by c.sort_order, c.name, c.id", req);
  }

  public PageResponse<Map<String, Object>> products(String category, Candidates text, PageRequest req) {
    return new SqlFilter(MENU).and(MENU_RULE).and("p.category=?", category).searchIds(text, "p.id").page(db, "order by p.sort_order, p.name, p.id", req);
  }

  public Optional<Map<String, Object>> product(String id) {
    return db.queryForList(MENU + " where p.id=? and " + MENU_RULE, id).stream().findFirst();
  }

  /** The active sizes of each given group of sizes, by parent id. */
  public Map<String, List<Map<String, Object>>> variants(List<String> parentIds) {
    Map<String, List<Map<String, Object>>> out = new HashMap<>();
    if (parentIds.isEmpty()) return out;
    String in = String.join(",", parentIds.stream().map(i -> "?").toList());
    for (Map<String, Object> v : db.queryForList("select v.id,v.parent_id,v.variant_name,v.price_xaf,v.image_url from product v where v.active=true and v.parent_id in (" + in + ") order by v.sort_order, v.price_xaf, v.id", parentIds.toArray()))
      out.computeIfAbsent((String) v.get("parent_id"), k -> new ArrayList<>()).add(v);
    return out;
  }

  /** The active add-ons offered with each given product, by product id. */
  public Map<String, List<Map<String, Object>>> addons(List<String> productIds) {
    Map<String, List<Map<String, Object>>> out = new HashMap<>();
    if (productIds.isEmpty()) return out;
    String in = String.join(",", productIds.stream().map(i -> "?").toList());
    for (Map<String, Object> a : db.queryForList("select pa.product_id,a.id,a.name,a.price_xaf,a.image_url from product_addon pa join addon a on a.id=pa.addon_id where a.active=true and pa.product_id in (" + in + ") order by a.sort_order, a.name, a.id", productIds.toArray()))
      out.computeIfAbsent((String) a.get("product_id"), k -> new ArrayList<>()).add(a);
    return out;
  }

  // ----- sales locations -----

  public PageResponse<Map<String, Object>> operations(String q, PageRequest req) {
    return new SqlFilter(OPERATION).and("o.active=true").search(q, "o.name", "o.city", "o.address").page(db, "order by o.name, o.id", req);
  }

  public Optional<Map<String, Object>> operation(String id) { return db.queryForList(OPERATION + " where o.id=? and o.active=true", id).stream().findFirst(); }

  public Optional<Map<String, Object>> operationByQr(String qrToken) { return db.queryForList(OPERATION + " where o.qr_token=? and o.active=true", qrToken).stream().findFirst(); }

  /** Payment methods the location accepts that are switched on in the business. */
  public List<String> paymentMethods(String operationId) {
    return db.queryForList("select m.method from operation_payment_method m join payment_method pm on pm.code=m.method where m.operation_id=? and pm.available=true and pm.enabled=true order by pm.sort_order", String.class, operationId);
  }

  /** Whether each given product can be had at the location now (stock is not tracked, or some is left). Groups of sizes count when any size is. */
  public Map<String, Boolean> availability(String operationId, List<String> productIds) {
    Map<String, Boolean> out = new HashMap<>();
    if (productIds.isEmpty()) return out;
    String in = String.join(",", productIds.stream().map(i -> "?").toList());
    List<Object> args = new ArrayList<>();
    args.add(operationId);
    args.addAll(productIds);
    for (Map<String, Object> r : db.queryForList("select p.id, exists (select 1 from product s left join inventory_balance b on b.product_id=s.id and b.operation_id=?" +
        " where s.active=true and (s.id=p.id or s.parent_id=p.id) and s.kind<>'PARENT' and (s.track_stock=false or coalesce(b.quantity,0)>0)) available from product p where p.id in (" + in + ")", args.toArray()))
      out.put((String) r.get("id"), Boolean.TRUE.equals(r.get("available")) || Integer.valueOf(1).equals(r.get("available")));
    return out;
  }
}
