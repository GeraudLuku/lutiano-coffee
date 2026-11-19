package com.lutiano.api.service.product;

import java.util.LinkedHashMap;
import com.lutiano.api.utils.Lists;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.inventory.StockAlerts;
import com.lutiano.api.proxy.ImageStorage;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.search.SearchCandidates;
import java.util.HashMap;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.models.request.product.ProductRequest;
import com.lutiano.api.models.request.product.VariantRequest;

/**
 * Catalogue: simple products, parent products with variants, prices (centrally controlled, with dated history),
 * stock-tracking and eligibility flags, and the add-ons each product allows.
 */
@Service
public class ProductService {
  public static final String SIMPLE = "SIMPLE", PARENT = "PARENT", VARIANT = "VARIANT";




  private final ProductRepository products;
  private final CategoryService categories;
  private final AddonService addons;
  private final ImageStorage images;
  private final AuditService audit;
  private final StockAlerts stockAlerts;
  private final SearchCandidates candidates;

  public ProductService(ProductRepository products, CategoryService categories, AddonService addons, ImageStorage images, AuditService audit, StockAlerts stockAlerts, SearchCandidates candidates) {
    this.candidates = candidates;
    this.stockAlerts = stockAlerts;
    this.products = products; this.categories = categories; this.addons = addons; this.images = images; this.audit = audit;
  }

  /** Sellers get sellable SKUs only (no parents, nothing inactive); staff get the whole catalogue. */
  /**
   * view: "catalog" (products and parents, not variants: the admin list), "sellable" (what can be sold or stocked: no parents),
   * or "all". Search also finds a parent by its variants' SKUs. Sellers always get the active sellable catalogue.
   */
  public PageResponse<Map<String, Object>> list(Actor actor, String q, String view, String category, String active, PageRequest req) {
    List<Map<String, Object>> all = actor.isSeller() ? products.findSellable() : products.findAll();
    String v = actor.isSeller() ? "sellable" : view == null || view.isBlank() ? "all" : view;
    Map<Object, String> variantText = new HashMap<>();
    for (Map<String, Object> p : all)
      if (VARIANT.equals(p.get("kind")) && p.get("parent_id") != null)
        variantText.merge(p.get("parent_id"), p.get("sku") + " " + p.get("variant_name"), (a, b) -> a + " " + b);
    List<Map<String, Object>> scope = all.stream().filter(p -> switch (v) {
      case "catalog" -> !VARIANT.equals(p.get("kind"));
      case "sellable" -> !PARENT.equals(p.get("kind"));
      default -> true;
    }).toList();
    var text = candidates.find(actor, "PRODUCT", q);   // a variant's name and SKU are part of its parent's document
    List<Map<String, Object>> rows = scope.stream().filter(p -> (Lists.inCandidates(text, p, "sku") || (p.get("parent_id") != null && text.ids().contains(p.get("parent_id"))))
        && Lists.is(category, p.get("category")) && Lists.is(active, p.get("active"))).toList();
    Map<String, Object> summary = new LinkedHashMap<>();
    long catalog = all.stream().filter(p -> !VARIANT.equals(p.get("kind"))).count();
    long activeCatalog = all.stream().filter(p -> !VARIANT.equals(p.get("kind")) && Rows.truthy(p.get("active"))).count();
    summary.put("products", catalog);
    summary.put("variants", all.stream().filter(p -> VARIANT.equals(p.get("kind"))).count());
    summary.put("active", activeCatalog);
    summary.put("inactive", catalog - activeCatalog);
    return PageResponse.slice(rows, req).withSummary(summary).withTruncated(text != null && text.truncated());
  }

  public Map<String, Object> get(String id) { return products.findById(id).orElseThrow(() -> Validate.notFound("Product not found.")); }

  /** A product that can hold stock: not a parent, and stock tracking on. */
  public Map<String, Object> requireStockItem(String id) {
    Map<String, Object> p = get(id);
    if (PARENT.equals(p.get("kind"))) throw Validate.bad(p.get("name") + " has variants; choose a specific variant.");
    if (!Boolean.TRUE.equals(p.get("track_stock"))) throw Validate.bad(p.get("name") + " does not track stock.");
    return p;
  }

  public PageResponse<Map<String, Object>> priceHistory(String id, PageRequest req) { get(id); return products.priceHistory(id, req); }

  public Map<String, Object> detail(String id) {
    Map<String, Object> p = get(id);
    p.put("stats", products.salesStats(id));
    p.put("stock", products.stockByOperation(id));
    p.put("variants", PARENT.equals(p.get("kind")) ? products.findVariants(id) : List.of());
    String addonOwner = VARIANT.equals(p.get("kind")) ? (String) p.get("parent_id") : id;
    p.put("addon_ids", products.addonIds(addonOwner));
    p.put("addons", addons.forProduct(addonOwner));
    return p;
  }

  @Transactional
  public Map<String, Object> create(Actor actor, ProductRequest in) {
    Clean c = clean(in);
    String id = UUID.randomUUID().toString();
    products.insert(id, c.kind, null, null, c.sku, c.name, c.category, c.description, c.unit, c.price, c.threshold, c.active,
        c.trackStock, c.loyalty, c.promotion, null);
    if (SIMPLE.equals(c.kind)) products.insertPrice(UUID.randomUUID().toString(), id, c.price, null, actor.id());
    audit.log(actor, "PRODUCT_CREATED", "product", id, PARENT.equals(c.kind) ? c.name + " (with variants)" : c.name + " at " + c.price + " XAF");
    return get(id);
  }

  @Transactional
  public Map<String, Object> update(Actor actor, String id, ProductRequest in) {
    Map<String, Object> before = get(id);
    if (VARIANT.equals(before.get("kind"))) throw Validate.bad("Edit variants from their parent product.");
    Clean c = clean(in);
    if (!c.kind.equals(before.get("kind"))) {
      if (PARENT.equals(c.kind) && (products.stockAnywhere(id) > 0 || products.hasSales(id)))
        throw Validate.conflict("This product already has stock or sales, so it cannot be turned into a product with variants. Create a new product instead.");
      if (SIMPLE.equals(c.kind) && Rows.asInt(before.get("variant_count")) > 0)
        throw Validate.conflict("Remove or deactivate the variants before selling this product on its own.");
    }
    if (SIMPLE.equals(c.kind)) requireTrackingChangeAllowed(id, before, c.trackStock);
    products.update(id, c.kind, c.sku, c.name, c.category, c.description, c.unit, c.price, c.threshold, c.active, c.trackStock, c.loyalty, c.promotion);
    if (PARENT.equals(c.kind)) products.syncVariantsWithParent(id);
    long oldPrice = Rows.asLong(before.get("price_xaf"));
    // Price changes apply to new sales only; completed sales keep their line price snapshot.
    if (SIMPLE.equals(c.kind) && oldPrice != c.price) products.insertPrice(UUID.randomUUID().toString(), id, c.price, oldPrice, actor.id());
    audit.log(actor, "PRODUCT_UPDATED", "product", id, SIMPLE.equals(c.kind) && oldPrice != c.price ? "price " + oldPrice + " -> " + c.price + " XAF" : null);
    if (!Objects.equals(before.get("active"), c.active)) audit.log(actor, c.active ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "product", id);
    stockAlerts.recheck(id);
    return get(id);
  }

  @Transactional
  public Map<String, Object> addVariant(Actor actor, String parentId, VariantRequest in) {
    Map<String, Object> parent = requireParent(parentId);
    CleanVariant v = cleanVariant(in);
    String id = UUID.randomUUID().toString();
    products.insert(id, VARIANT, parentId, v.variantName, v.sku, parent.get("name") + " – " + v.variantName, (String) parent.get("category"),
        (String) parent.get("description"), v.unit, v.price, v.threshold, v.active, v.trackStock,
        Boolean.TRUE.equals(parent.get("loyalty_eligible")), Boolean.TRUE.equals(parent.get("promotion_eligible")), (String) parent.get("image_url"));
    products.insertPrice(UUID.randomUUID().toString(), id, v.price, null, actor.id());
    audit.log(actor, "VARIANT_CREATED", "product", parentId, v.variantName + " at " + v.price + " XAF");
    return get(id);
  }

  @Transactional
  public Map<String, Object> updateVariant(Actor actor, String parentId, String variantId, VariantRequest in) {
    Map<String, Object> parent = requireParent(parentId);
    Map<String, Object> before = get(variantId);
    if (!parentId.equals(before.get("parent_id"))) throw Validate.notFound("Variant not found for this product.");
    CleanVariant v = cleanVariant(in);
    requireTrackingChangeAllowed(variantId, before, v.trackStock);
    products.updateVariant(variantId, v.variantName, parent.get("name") + " – " + v.variantName, v.sku, v.unit, v.price, v.threshold, v.active, v.trackStock);
    long oldPrice = Rows.asLong(before.get("price_xaf"));
    if (oldPrice != v.price) products.insertPrice(UUID.randomUUID().toString(), variantId, v.price, oldPrice, actor.id());
    audit.log(actor, "VARIANT_UPDATED", "product", parentId, v.variantName + (oldPrice != v.price ? ": price " + oldPrice + " -> " + v.price + " XAF" : ""));
    stockAlerts.recheck(variantId);
    return get(variantId);
  }

  public void setActive(Actor actor, String id, boolean active) {
    if (products.setActive(id, active) != 1) throw Validate.notFound("Product not found.");
    stockAlerts.recheck(id);
    audit.log(actor, active ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "product", id);
  }

  /** Which add-ons can be ordered with this product (set on simple products and on parents for all variants). */
  @Transactional
  public Map<String, Object> setAddons(Actor actor, String id, List<String> addonIds) {
    Map<String, Object> p = get(id);
    if (VARIANT.equals(p.get("kind"))) throw Validate.bad("Set add-ons on the parent product; its variants share them.");
    Set<String> ids = new LinkedHashSet<>(addonIds == null ? List.of() : addonIds);
    for (String a : ids) addons.get(Validate.id(a, "Add-on"));
    products.replaceAddons(id, ids);
    audit.log(actor, "PRODUCT_ADDONS_UPDATED", "product", id, ids.size() + " add-ons allowed");
    return detail(id);
  }

  public String uploadImage(Actor actor, String id, MultipartFile file) throws Exception {
    get(id);
    String url = images.save(ImageStorage.PRODUCTS, id, file);
    products.setImageUrl(id, url);
    audit.log(actor, "PRODUCT_IMAGE_UPDATED", "product", id);
    return url;
  }

  /** Stock tracking can only be switched off when nothing is held or in transit, so balances never go stale. */
  private void requireTrackingChangeAllowed(String id, Map<String, Object> before, boolean trackStock) {
    if (Boolean.TRUE.equals(before.get("track_stock")) && !trackStock && products.stockAnywhere(id) > 0)
      throw Validate.conflict("Stock tracking can only be switched off when no stock is held or in transit. Adjust the stock to zero first.");
  }

  private Map<String, Object> requireParent(String parentId) {
    Map<String, Object> parent = get(Validate.id(parentId, "Product"));
    if (!PARENT.equals(parent.get("kind"))) throw Validate.bad("Turn on \"Require variant\" for this product before adding variants.");
    return parent;
  }

  private record Clean(String kind, String sku, String name, String category, String description, String unit, long price, int threshold,
                       boolean active, boolean trackStock, boolean loyalty, boolean promotion) {}

  private Clean clean(ProductRequest in) {
    boolean parent = Boolean.TRUE.equals(in.requireVariants());
    long price = 0;
    if (!parent) {
      if (in.priceXaf() == null || in.priceXaf() < 0) throw Validate.bad("Price must be zero or more (whole FCFA).");
      price = in.priceXaf();
    }
    return new Clean(parent ? PARENT : SIMPLE, Validate.text(in.sku(), "SKU", 80).toUpperCase(), Validate.text(in.name(), "Product name", 150),
        categories.requireActiveName(in.category()), Validate.optional(in.description(), "Description", 500), Validate.optional(in.unit(), "Unit", 30),
        price, threshold(in.lowStockThreshold()), in.active() == null || in.active(), in.trackStock() == null || in.trackStock(),
        in.loyaltyEligible() == null || in.loyaltyEligible(), in.promotionEligible() == null || in.promotionEligible());
  }

  private record CleanVariant(String variantName, String sku, String unit, long price, int threshold, boolean trackStock, boolean active) {}

  private CleanVariant cleanVariant(VariantRequest in) {
    if (in.priceXaf() == null || in.priceXaf() < 0) throw Validate.bad("Price must be zero or more (whole FCFA).");
    return new CleanVariant(Validate.text(in.variantName(), "Variant name", 80), Validate.text(in.sku(), "SKU", 80).toUpperCase(),
        Validate.optional(in.unit(), "Unit", 30), in.priceXaf(), threshold(in.lowStockThreshold()),
        in.trackStock() == null || in.trackStock(), in.active() == null || in.active());
  }

  private static int threshold(Integer t) {
    int value = t == null ? 10 : t;
    if (value < 0) throw Validate.bad("Low-stock threshold cannot be negative.");
    return value;
  }
}
