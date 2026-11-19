package com.lutiano.api.service.product;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.ImageStorage;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.repository.product.CatalogRepository;
import com.lutiano.api.models.request.product.AddonRequest;
import com.lutiano.api.entity.PricedAddon;

/** Add-ons (extra shot, syrup…) that add a price per unit to a sale line. They do not track stock. */
@Service
public class AddonService {
  /** An add-on as priced for a sale line. */

  private final CatalogRepository catalog;
  private final AuditService audit;
  private final ImageStorage images;

  public AddonService(CatalogRepository catalog, AuditService audit, ImageStorage images) {
    this.catalog = catalog; this.audit = audit; this.images = images;
  }

  public List<Map<String, Object>> list() { return catalog.addons(); }

  public Map<String, Object> get(String id) { return catalog.addon(id).orElseThrow(() -> Validate.notFound("Add-on not found.")); }

  /** Add-ons linked to a SIMPLE or PARENT product. */
  public List<Map<String, Object>> forProduct(String productId) { return catalog.addonsForProduct(productId); }

  /** Active add-ons with the sellable product ids each applies to (parents expanded to their variants), for the Seller app. */
  public List<Map<String, Object>> forSellers() {
    List<Map<String, Object>> rows = catalog.activeAddons();
    for (Map<String, Object> a : rows) a.put("product_ids", catalog.addonProductIds(a.get("id")));
    return rows;
  }

  /**
   * Validates add-ons for a sellable product and returns their current prices.
   * Allowed add-ons are those linked to the product itself or, for a variant, to its parent.
   */
  public List<PricedAddon> priceFor(String productId, String parentId, List<String> addonIds) {
    if (addonIds == null || addonIds.isEmpty()) return List.of();
    String owner = parentId != null ? parentId : productId;
    return addonIds.stream().distinct().sorted().map(id -> catalog.pricedAddon(Validate.id(id, "Add-on"), owner)
        .orElseThrow(() -> Validate.conflict("An add-on in this sale is not available for this product. Refresh the catalogue."))).toList();
  }

  public Map<String, Object> create(Actor actor, AddonRequest in) {
    String id = UUID.randomUUID().toString();
    catalog.insertAddon(id, Validate.text(in.name(), "Name", 80), price(in.priceXaf()), in.sortOrder() == null ? 0 : in.sortOrder(), in.active() == null || in.active());
    audit.log(actor, "ADDON_CREATED", "addon", id, in.name() + " +" + in.priceXaf() + " XAF");
    return get(id);
  }

  public Map<String, Object> update(Actor actor, String id, AddonRequest in) {
    Map<String, Object> before = get(id);
    long price = price(in.priceXaf());
    catalog.updateAddon(id, Validate.text(in.name(), "Name", 80), price, in.sortOrder() == null ? 0 : in.sortOrder(), in.active() == null || in.active());
    long old = ((Number) before.get("price_xaf")).longValue();
    audit.log(actor, "ADDON_UPDATED", "addon", id, old != price ? "price " + old + " -> " + price + " XAF" : null);
    return get(id);
  }

  public String uploadImage(Actor actor, String id, MultipartFile file) throws Exception {
    get(id);
    String url = images.save(ImageStorage.ADDONS, id, file);
    catalog.setAddonImage(id, url);
    audit.log(actor, "ADDON_IMAGE_UPDATED", "addon", id);
    return url;
  }

  private static long price(Long p) {
    if (p == null || p < 0) throw Validate.bad("Price must be zero or more (whole FCFA).");
    return p;
  }
}
