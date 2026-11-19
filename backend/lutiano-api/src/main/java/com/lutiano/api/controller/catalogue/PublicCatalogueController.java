package com.lutiano.api.controller.catalogue;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.service.search.SearchRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.catalogue.PublicCatalogueService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.catalogue.CategoryItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.catalogue.OperationItemResponse;
import com.lutiano.api.models.response.catalogue.OperationResponse;
import com.lutiano.api.models.response.catalogue.CatalogueProductItemResponse;
import com.lutiano.api.models.response.catalogue.ProductItemResponse;
import com.lutiano.api.utils.Responses;

/** The menu and the sales locations, readable without signing in. Limited per address. */
@RestController
@RequestMapping("/public")
public class PublicCatalogueController {
  private static final int PER_MINUTE = 120;
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final PublicCatalogueService service;
  private final SearchRateLimiter limiter;

  public PublicCatalogueController(PublicCatalogueService service, SearchRateLimiter limiter) { this.service = service; this.limiter = limiter; }

  @GetMapping("/categories")
  public ResponseEntity<GenericApiResponse<PageResponse<CategoryItemResponse>>> categories(PageRequest paging, HttpServletRequest req) {
    limit(req);
    return Responses.page(service.categories(paging), CategoryItemResponse.class);
  }

  /** category (name), q (text search). Each product has its sizes (variants) and add-ons. */
  @GetMapping("/products")
  public ResponseEntity<GenericApiResponse<PageResponse<ProductItemResponse>>> products(@RequestParam(required = false) String category, @RequestParam(required = false) String q, PageRequest paging, HttpServletRequest req) {
    limit(req);
    return Responses.page(service.products(category, q, paging), ProductItemResponse.class);
  }

  @GetMapping("/products" + ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> product(@PathVariable String id, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.product(id));
  }

  @GetMapping("/operations")
  public ResponseEntity<GenericApiResponse<PageResponse<OperationItemResponse>>> operations(@RequestParam(required = false) String q, PageRequest paging, HttpServletRequest req) {
    limit(req);
    return Responses.page(service.operations(q, paging), OperationItemResponse.class);
  }

  @GetMapping("/operations" + ID)
  public ResponseEntity<GenericApiResponse<OperationResponse>> operation(@PathVariable String id, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.operation(id), OperationResponse.class);
  }

  /** The location behind a scanned QR code (the text "LUTIANO:OP:…"). */
  @GetMapping("/operations/by-qr")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> byQr(@RequestParam String code, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.operationByQr(code));
  }

  /** The menu at one location, each product saying whether it is available there now. */
  @GetMapping("/operations" + ID + "/products")
  public ResponseEntity<GenericApiResponse<PageResponse<CatalogueProductItemResponse>>> menuAt(@PathVariable String id, @RequestParam(required = false) String category, @RequestParam(required = false) String q,
                                          PageRequest paging, HttpServletRequest req) {
    limit(req);
    return Responses.page(service.menuAt(id, category, q, paging), CatalogueProductItemResponse.class);
  }

  private void limit(HttpServletRequest req) {
    String forwarded = req.getHeader("X-Forwarded-For");
    limiter.check("public", forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : req.getRemoteAddr(), PER_MINUTE);
  }
}
