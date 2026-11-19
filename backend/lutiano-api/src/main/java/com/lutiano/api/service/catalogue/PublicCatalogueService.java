package com.lutiano.api.service.catalogue;

import com.lutiano.api.utils.Candidates;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.service.operation.QrCodeService;
import com.lutiano.api.service.search.SearchPrincipal;
import com.lutiano.api.service.search.SearchService;
import java.sql.Time;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.catalogue.PublicCatalogueRepository;

/** The public menu and sales locations, shaped for the Customer app (sizes and add-ons attached, "open now" worked out). */
@Service
public class PublicCatalogueService {
  private final PublicCatalogueRepository repo;
  private final OperationRepository operations;
  private final SearchService search;

  public PublicCatalogueService(PublicCatalogueRepository repo, OperationRepository operations, SearchService search) { this.repo = repo; this.operations = operations; this.search = search; }

  public PageResponse<Map<String, Object>> categories(PageRequest req) { return repo.categories(req); }

  /** The menu, optionally one category and/or a text the search service understands (typos, accents, French and English). */
  public PageResponse<Map<String, Object>> products(String category, String q, PageRequest req) {
    Candidates text = search.candidates(SearchPrincipal.publicCatalogue(), "PRODUCT", q);
    PageResponse<Map<String, Object>> page = repo.products(category == null || category.isBlank() ? null : category, text, req);
    attach(page.items());
    return page;
  }

  public Map<String, Object> product(String id) {
    Map<String, Object> p = repo.product(Validate.id(id, "Product")).orElseThrow(() -> Validate.notFound("Product not found."));
    attach(List.of(p));
    return p;
  }

  /** A group of sizes lists its sizes; every product lists the add-ons it takes. */
  private void attach(List<Map<String, Object>> products) {
    Map<String, List<Map<String, Object>>> sizes = repo.variants(products.stream().filter(p -> "PARENT".equals(p.get("kind"))).map(p -> (String) p.get("id")).toList());
    Map<String, List<Map<String, Object>>> addons = repo.addons(products.stream().map(p -> (String) p.get("id")).toList());
    for (Map<String, Object> p : products) {
      p.put("variants", sizes.getOrDefault((String) p.get("id"), List.of()));
      p.put("addons", addons.getOrDefault((String) p.get("id"), List.of()));
    }
  }

  // ----- sales locations -----

  public PageResponse<Map<String, Object>> operations(String q, PageRequest req) {
    PageResponse<Map<String, Object>> page = repo.operations(q, req);
    page.items().forEach(o -> o.put("open_now", openNow(operations.hours((String) o.get("id")))));
    return page;
  }

  public Map<String, Object> operation(String id) { return detail(repo.operation(Validate.id(id, "Sales location")).orElseThrow(() -> Validate.notFound("Sales location not found."))); }

  /** The location behind a scanned QR code ("LUTIANO:OP:…"); a code that is not ours is a 400, an unknown or closed one a 404. */
  public Map<String, Object> operationByQr(String code) {
    String c = code == null ? "" : code.trim();
    if (!c.startsWith(QrCodeService.PREFIX)) throw Validate.bad("This is not a LUTIANO sales point code.");
    return detail(repo.operationByQr(c.substring(QrCodeService.PREFIX.length())).orElseThrow(() -> Validate.notFound("This sales point is not available.")));
  }

  private Map<String, Object> detail(Map<String, Object> o) {
    List<Map<String, Object>> hours = operations.hours((String) o.get("id"));
    Map<String, Object> out = new LinkedHashMap<>(o);
    out.put("hours", hours);
    out.put("open_now", openNow(hours));
    out.put("payment_methods", repo.paymentMethods((String) o.get("id")));
    return out;
  }

  /** The menu as sold at one location: each product says whether it can be had there now. */
  public PageResponse<Map<String, Object>> menuAt(String operationId, String category, String q, PageRequest req) {
    repo.operation(Validate.id(operationId, "Sales location")).orElseThrow(() -> Validate.notFound("Sales location not found."));
    PageResponse<Map<String, Object>> page = products(category, q, req);
    Map<String, Boolean> available = repo.availability(operationId, page.items().stream().map(p -> (String) p.get("id")).toList());
    page.items().forEach(p -> p.put("available", available.getOrDefault((String) p.get("id"), false)));
    return page;
  }

  /** Open now in Douala time. A location with no hours set is treated as always open. */
  public static boolean openNow(List<Map<String, Object>> hours) { return openAt(hours, ZonedDateTime.now(Periods.DOUALA)); }

  public static boolean openAt(List<Map<String, Object>> hours, ZonedDateTime now) {
    if (hours.isEmpty()) return true;
    int weekday = now.getDayOfWeek().getValue();
    LocalTime t = now.toLocalTime();
    return hours.stream().anyMatch(h -> Rows.asInt(h.get("weekday")) == weekday && !t.isBefore(((Time) h.get("opens")).toLocalTime()) && t.isBefore(((Time) h.get("closes")).toLocalTime()));
  }
}
