package com.lutiano.api.controller.product;

import static com.lutiano.api.security.Roles.ADMIN;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.product.AddonService;
import com.lutiano.api.service.product.CategoryService;
import com.lutiano.api.models.request.product.AddonRequest;
import com.lutiano.api.models.request.product.CategoryRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.product.AddonResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.product.ProductCategoryResponse;
import com.lutiano.api.utils.Responses;

/** Product categories and add-ons. Anyone signed in can read; administrators manage. */
@RestController
public class CatalogSettingsController {
  private final CategoryService categories;
  private final AddonService addons;
  private final CurrentUser current;

  public CatalogSettingsController(CategoryService categories, AddonService addons, CurrentUser current) {
    this.categories = categories; this.addons = addons; this.current = current;
  }

  @GetMapping("/product-categories")
  public ResponseEntity<GenericApiResponse<List<ProductCategoryResponse>>> categories(Authentication a) {
    current.get(a);
    return Responses.list(categories.list(), ProductCategoryResponse.class);
  }

  @PostMapping("/product-categories")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> createCategory(@RequestBody CategoryRequest body, Authentication a) {
    return Responses.created(categories.create(current.require(a, ADMIN), body));
  }

  @PutMapping("/product-categories/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateCategory(@PathVariable String id, @RequestBody CategoryRequest body, Authentication a) {
    return Responses.ok(categories.update(current.require(a, ADMIN), id, body));
  }

  @PostMapping("/product-categories/{id}/image")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadCategoryImage(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(Map.of("imageUrl", categories.uploadImage(current.require(a, ADMIN), id, file)));
  }

  @PostMapping("/addons/{id}/image")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> uploadAddonImage(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(Map.of("imageUrl", addons.uploadImage(current.require(a, ADMIN), id, file)));
  }

  /** Staff get every add-on; sellers get active add-ons with the product ids each applies to. */
  @GetMapping("/addons")
  public ResponseEntity<GenericApiResponse<List<AddonResponse>>> addons(Authentication a) {
    return Responses.list(current.get(a).isSeller() ? addons.forSellers() : addons.list(), AddonResponse.class);
  }

  @PostMapping("/addons")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> createAddon(@RequestBody AddonRequest body, Authentication a) {
    return Responses.created(addons.create(current.require(a, ADMIN), body));
  }

  @PutMapping("/addons/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateAddon(@PathVariable String id, @RequestBody AddonRequest body, Authentication a) {
    return Responses.ok(addons.update(current.require(a, ADMIN), id, body));
  }
}
