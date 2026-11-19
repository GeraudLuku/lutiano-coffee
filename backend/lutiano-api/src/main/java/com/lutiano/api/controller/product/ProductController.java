package com.lutiano.api.controller.product;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.product.CategoryService;
import com.lutiano.api.service.product.ProductImportService;
import com.lutiano.api.service.product.ProductInsights;
import com.lutiano.api.service.product.ProductService;
import com.lutiano.api.models.request.product.AddonSelectionRequest;
import com.lutiano.api.models.request.product.ProductRequest;
import com.lutiano.api.models.request.product.VariantRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.product.ProductPerformanceResponse;
import com.lutiano.api.models.response.product.PriceHistoryItemResponse;
import com.lutiano.api.models.response.product.ProductItemV2Response;
import com.lutiano.api.models.response.product.ProductResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.product.ProductRewardResponse;

@RestController
@RequestMapping("/products")
public class ProductController {
  private final ProductService service;
  private final ProductInsights insights;
  private final ProductImportService importer;
  private final CategoryService categories;
  private final CurrentUser current;

  public ProductController(ProductService service, CategoryService categories, CurrentUser current, ProductInsights insights, ProductImportService importer) {
    this.service = service; this.categories = categories; this.current = current; this.insights = insights; this.importer = importer;
  }


  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<ProductItemV2Response>>> list(@RequestParam(required = false) String q, @RequestParam(required = false) String view,
                                        @RequestParam(required = false) String category, @RequestParam(required = false) String active,
                                        PageRequest paging, Authentication a) {
    return Responses.page(service.list(current.get(a), q, view, category, active, paging), ProductItemV2Response.class);
  }

  /** Active category names, kept for older clients; use /product-categories for management. */
  @GetMapping("/categories")
  public ResponseEntity<GenericApiResponse<List<Object>>> categoryNames(Authentication a) {
    current.get(a);
    return Responses.ok(categories.list().stream().filter(c -> Boolean.TRUE.equals(c.get("active"))).map(c -> c.get("name")).toList());
  }

  @GetMapping("/{id:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<ProductResponse>> detail(@PathVariable String id, Authentication a) {
    current.require(a, Roles.STAFF);
    return Responses.ok(service.detail(id), ProductResponse.class);
  }

  @GetMapping("/{id:[0-9a-fA-F-]+}/price-history")
  public ResponseEntity<GenericApiResponse<PageResponse<PriceHistoryItemResponse>>> priceHistory(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, Roles.STAFF);
    return Responses.page(service.priceHistory(id, paging), PriceHistoryItemResponse.class);
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody ProductRequest body, Authentication a) {
    return Responses.created(service.create(current.require(a, ADMIN), body));
  }

  @PutMapping("/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@PathVariable String id, @RequestBody ProductRequest body, Authentication a) {
    return Responses.ok(service.update(current.require(a, ADMIN), id, body));
  }

  @PostMapping("/{id}/variants")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> addVariant(@PathVariable String id, @RequestBody VariantRequest body, Authentication a) {
    return Responses.created(service.addVariant(current.require(a, ADMIN), id, body));
  }

  @PutMapping("/{id}/variants/{variantId}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateVariant(@PathVariable String id, @PathVariable String variantId, @RequestBody VariantRequest body, Authentication a) {
    return Responses.ok(service.updateVariant(current.require(a, ADMIN), id, variantId, body));
  }

  @PutMapping("/{id}/addons")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> setAddons(@PathVariable String id, @RequestBody AddonSelectionRequest body, Authentication a) {
    return Responses.ok(service.setAddons(current.require(a, ADMIN), id, body.addonIds()));
  }

  @PostMapping("/{id}/deactivate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> deactivate(@PathVariable String id, Authentication a) {
    service.setActive(current.require(a, ADMIN), id, false);
    return Responses.ok(Map.of("status", "inactive"));
  }

  @PostMapping("/{id}/activate")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> activate(@PathVariable String id, Authentication a) {
    service.setActive(current.require(a, ADMIN), id, true);
    return Responses.ok(Map.of("status", "active"));
  }

  /** Loyalty points per unit and the commission of each seller role, per sellable item of the product. */
  @GetMapping("/{id:[0-9a-fA-F-]+}/rewards")
  public ResponseEntity<GenericApiResponse<List<ProductRewardResponse>>> rewards(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.list(insights.rewards(id), ProductRewardResponse.class);
  }

  /** Units and revenue over the last {@code days} days, by day, location and variant. */
  @GetMapping("/{id:[0-9a-fA-F-]+}/performance")
  public ResponseEntity<GenericApiResponse<ProductPerformanceResponse>> performance(@PathVariable String id, @RequestParam(required = false) Integer days, Authentication a) {
    current.get(a);
    return Responses.ok(insights.performance(id, days), ProductPerformanceResponse.class);
  }

  /** Creates products from a CSV file (all or nothing). */
  @PostMapping("/import")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> importCsv(@RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(importer.importCsv(current.require(a, ADMIN), file));
  }

  @PostMapping("/{id}/image")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadImage(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(Map.of("imageUrl", service.uploadImage(current.require(a, ADMIN), id, file)));
  }
}
