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
import com.lutiano.api.models.request.product.CategoryRequest;

/** Managed product categories. Renaming cascades to products through the foreign key on product.category. */
@Service
public class CategoryService {

  private final CatalogRepository catalog;
  private final AuditService audit;
  private final ImageStorage images;

  public CategoryService(CatalogRepository catalog, AuditService audit, ImageStorage images) {
    this.catalog = catalog; this.audit = audit; this.images = images;
  }

  public List<Map<String, Object>> list() { return catalog.categories(); }

  public Map<String, Object> get(String id) { return catalog.category(id).orElseThrow(() -> Validate.notFound("Category not found.")); }

  /** The stored name of an active category (case-insensitive match), for product forms. */
  public String requireActiveName(String name) {
    String n = Validate.text(name, "Category", 60);
    return catalog.activeCategoryName(n)
        .orElseThrow(() -> Validate.bad("Choose an active category, or create \"" + n + "\" in Product Categories first."));
  }

  public Map<String, Object> create(Actor actor, CategoryRequest in) {
    String id = UUID.randomUUID().toString();
    catalog.insertCategory(id, Validate.text(in.name(), "Name", 60), Validate.optional(in.description(), "Description", 255),
        in.sortOrder() == null ? 0 : in.sortOrder(), in.active() == null || in.active());
    audit.log(actor, "CATEGORY_CREATED", "category", id, in.name());
    return get(id);
  }

  public String uploadImage(Actor actor, String id, MultipartFile file) throws Exception {
    get(id);
    String url = images.save(ImageStorage.CATEGORIES, id, file);
    catalog.setCategoryImage(id, url);
    audit.log(actor, "CATEGORY_IMAGE_UPDATED", "category", id);
    return url;
  }

  public Map<String, Object> update(Actor actor, String id, CategoryRequest in) {
    String before = (String) get(id).get("name");
    String name = Validate.text(in.name(), "Name", 60);
    catalog.updateCategory(id, name, Validate.optional(in.description(), "Description", 255), in.sortOrder() == null ? 0 : in.sortOrder(),
        in.active() == null || in.active());
    audit.log(actor, "CATEGORY_UPDATED", "category", id, before.equals(name) ? null : before + " -> " + name);
    return get(id);
  }
}
