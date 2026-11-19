package com.lutiano.api.repository.product;

import com.lutiano.api.utils.SqlFilter;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.utils.Periods;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import com.lutiano.api.definitions.enums.SearchType;

/**
 * Products are SIMPLE (sold directly), PARENT (groups variants; never sold or stocked) or VARIANT
 * (a sellable SKU under a parent). Every sellable item is one row, so stock and sales stay per SKU.
 */
@Repository
public class ProductRepository {
  /** The product and, for a parent, all its variants (bind the product id twice). */
  private static final String FAMILY = "(select fp.id from product fp where fp.id=? or fp.parent_id=?)";
  /** Variant rollups are only meaningful for PARENT rows. */
  private static final String SELECT =
      "select p.*, coalesce(c.quantity,0) central_quantity, pp.name parent_name, pp.active parent_active," +
      " (select count(*) from product v where v.parent_id=p.id) variant_count," +
      " (select count(*) from product v where v.parent_id=p.id and v.active=true) active_variant_count," +
      " (select min(v.price_xaf) from product v where v.parent_id=p.id and v.active=true) min_variant_price," +
      " (select coalesce(sum(cv.quantity),0) from product v join central_inventory cv on cv.product_id=v.id where v.parent_id=p.id) variants_central_quantity" +
      " from product p left join central_inventory c on c.product_id=p.id left join product pp on pp.id=p.parent_id";
  /** Sellable = not a parent, active, and (if a variant) its parent is active. */
  static final String SELLABLE = "p.kind<>'PARENT' and p.active=true and (pp.id is null or pp.active=true)";
  private final JdbcTemplate db;
  private final SearchOutbox search;

  public ProductRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  /** Queues the product for re-indexing; a variant is indexed inside its parent, so the parent is queued too. */
  private void indexed(String id) {
    search.changed(SearchType.PRODUCT, id);
    db.queryForList("select parent_id from product where id=? and parent_id is not null", String.class, id).forEach(p -> search.changed(SearchType.PRODUCT, p));
  }

  public List<Map<String, Object>> findAll() { return db.queryForList(SELECT + " order by p.name"); }

  public List<Map<String, Object>> findSellable() { return db.queryForList(SELECT + " where " + SELLABLE + " order by p.category,p.name"); }

  public Optional<Map<String, Object>> findById(String id) { return db.queryForList(SELECT + " where p.id=?", id).stream().findFirst(); }

  public List<Map<String, Object>> findVariants(String parentId) {
    return db.queryForList(SELECT + " where p.parent_id=? order by p.sort_order, p.price_xaf, p.variant_name", parentId);
  }

  /** Name, current price, stock-tracking flag and parent of a sellable product, or empty. */
  public Optional<Map<String, Object>> findSellable(String id) {
    return db.queryForList("select p.id,p.name,p.price_xaf,p.track_stock,p.parent_id from product p left join product pp on pp.id=p.parent_id where p.id=? and " + SELLABLE, id)
        .stream().findFirst();
  }

  public void insert(String id, String kind, String parentId, String variantName, String sku, String name, String category, String description,
                     String unit, long price, int threshold, boolean active, boolean trackStock, boolean loyalty, boolean promotion, String imageUrl) {
    db.update("insert into product(id,kind,parent_id,variant_name,sku,name,category,description,unit,price_xaf,low_stock_threshold,active,track_stock," +
        "loyalty_eligible,promotion_eligible,image_url,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, kind, parentId, variantName, sku, name, category, description, unit, price, threshold, active, trackStock, loyalty, promotion, imageUrl);
    if (!"PARENT".equals(kind)) db.update("insert ignore into central_inventory(product_id,quantity) values(?,0)", id);
    indexed(id);
  }

  public void update(String id, String kind, String sku, String name, String category, String description, String unit, long price,
                     int threshold, boolean active, boolean trackStock, boolean loyalty, boolean promotion) {
    db.update("update product set kind=?,sku=?,name=?,category=?,description=?,unit=?,price_xaf=?,low_stock_threshold=?,active=?,track_stock=?," +
        "loyalty_eligible=?,promotion_eligible=?,updated_at=UTC_TIMESTAMP() where id=?",
        kind, sku, name, category, description, unit, price, threshold, active, trackStock, loyalty, promotion, id);
    if (!"PARENT".equals(kind)) db.update("insert ignore into central_inventory(product_id,quantity) values(?,0)", id);
    indexed(id);
  }

  public void updateVariant(String id, String variantName, String name, String sku, String unit, long price, int threshold, boolean active, boolean trackStock) {
    db.update("update product set variant_name=?,name=?,sku=?,unit=?,price_xaf=?,low_stock_threshold=?,active=?,track_stock=?,updated_at=UTC_TIMESTAMP() where id=?",
        variantName, name, sku, unit, price, threshold, active, trackStock, id);
    indexed(id);
  }

  /** Variants follow their parent's name, category, description and eligibility flags. */
  public void syncVariantsWithParent(String parentId) {
    db.update("update product v join product p on p.id=v.parent_id set v.name=concat(p.name,' – ',v.variant_name), v.category=p.category," +
        " v.description=p.description, v.loyalty_eligible=p.loyalty_eligible, v.promotion_eligible=p.promotion_eligible where v.parent_id=?", parentId);
    indexed(parentId);
  }

  public int setActive(String id, boolean active) {
    int n = db.update("update product set active=?,updated_at=UTC_TIMESTAMP() where id=?", active, id);
    indexed(id);
    return n;
  }

  /** Sets the image on a product and, for a parent, on all its variants. */
  public void setImageUrl(String id, String url) { db.update("update product set image_url=? where id=? or parent_id=?", url, id, id); }

  /** Units held anywhere (central + locations) plus units in transfers not yet received. */
  public int stockAnywhere(String id) {
    Integer n = db.queryForObject("select coalesce((select quantity from central_inventory where product_id=?),0)" +
        " + coalesce((select sum(quantity) from inventory_balance where product_id=?),0)" +
        " + coalesce((select sum(sent_quantity) from stock_allocation where product_id=? and status in ('APPROVED','IN_TRANSIT')),0)", Integer.class, id, id, id);
    return n == null ? 0 : n;
  }

  public boolean hasSales(String id) {
    Integer n = db.queryForObject("select count(*) from sale_line where product_id=?", Integer.class, id);
    return n != null && n > 0;
  }

  /** Sales for a product, or for all variants of a parent. Revenue includes add-ons. */
  public Map<String, Object> salesStats(String id) {
    return db.queryForMap("select coalesce(sum(quantity),0) units_sold, coalesce(sum(quantity*(unit_price_xaf+addons_unit_xaf)),0) revenue_xaf," +
        " count(distinct sale_id) transactions," +
        " (select coalesce(sum(commission_xaf-reversed_xaf),0) from sale_commission where product_id in " + FAMILY + ") commission_xaf," +
        " (select coalesce(round(sum(a.total_points*(l.quantity*(l.unit_price_xaf+l.addons_unit_xaf))/s.total_xaf)),0) from sale_line l join sale s on s.id=l.sale_id" +
        "   join loyalty_award a on a.sale_id=s.id where s.total_xaf>0 and l.product_id in " + FAMILY + ") points_awarded" +
        " from sale_line where product_id in " + FAMILY, id, id, id, id, id, id);
  }

  /** Sales of a product (or all variants of a parent) in a period: by day, by location and by variant. */
  public Map<String, Object> performance(String id, Timestamp from, Timestamp to) {
    String sold = " from sale_line l join sale s on s.id=l.sale_id where s.created_at>=? and s.created_at<? and l.product_id in " + FAMILY;
    String revenue = "sum(l.quantity*(l.unit_price_xaf+l.addons_unit_xaf))";
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("totals", db.queryForMap("select coalesce(sum(l.quantity),0) units, coalesce(" + revenue + ",0) revenue_xaf, count(distinct l.sale_id) transactions" + sold, from, to, id, id));
    out.put("byDay", db.queryForList("select " + Periods.businessDate("s.created_at") + " d, sum(l.quantity) units, " + revenue + " revenue_xaf" + sold + " group by d order by d", from, to, id, id));
    out.put("byLocation", db.queryForList("select o.id operation_id, o.name operation_name, sum(l.quantity) units, " + revenue + " revenue_xaf" + sold.replace(" where ", " join operation o on o.id=s.operation_id where ") + " group by o.id, o.name order by revenue_xaf desc, o.name limit 5", from, to, id, id));
    out.put("byVariant", db.queryForList("select p.id product_id, coalesce(p.variant_name, p.name) name, sum(l.quantity) units, " + revenue + " revenue_xaf" + sold.replace(" where ", " join product p on p.id=l.product_id where ") + " group by p.id, p.variant_name, p.name order by revenue_xaf desc, name", from, to, id, id));
    return out;
  }

  public List<Map<String, Object>> stockByOperation(String id) {
    return db.queryForList("select o.id operation_id,o.name operation_name,o.code,p.id product_id,p.name product_name,p.variant_name,b.quantity" +
        " from inventory_balance b join operation o on o.id=b.operation_id join product p on p.id=b.product_id" +
        " where p.id=? or p.parent_id=? order by o.name,p.variant_name", id, id);
  }

  // ----- price history -----

  public void insertPrice(String id, String productId, long price, Long previous, String actorId) {
    db.update("insert into product_price_history(id,product_id,price_xaf,previous_price_xaf,changed_by,changed_at) values(?,?,?,?,?,UTC_TIMESTAMP())",
        id, productId, price, previous, actorId);
  }

  /** Price changes of the product and, for a parent, its variants; newest first. */
  public PageResponse<Map<String, Object>> priceHistory(String id, PageRequest req) {
    return new SqlFilter("select h.*,p.name product_name,p.variant_name,u.full_name changed_by_name from product_price_history h" +
        " join product p on p.id=h.product_id left join staff_user u on u.id=h.changed_by")
        .and("? in (p.id,p.parent_id)", id).page(db, "order by h.changed_at desc, h.id desc", req);
  }

  // ----- allowed add-ons (on SIMPLE or PARENT products) -----

  public List<String> addonIds(String productId) {
    return db.queryForList("select addon_id from product_addon where product_id=?", String.class, productId);
  }

  public void replaceAddons(String productId, Collection<String> addonIds) {
    db.update("delete from product_addon where product_id=?", productId);
    for (String a : addonIds) db.update("insert into product_addon(product_id,addon_id) values(?,?)", productId, a);
  }
}
