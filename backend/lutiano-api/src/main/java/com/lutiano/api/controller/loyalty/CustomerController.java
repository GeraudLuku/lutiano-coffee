package com.lutiano.api.controller.loyalty;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.search.SearchCandidates;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.LinkedHashMap;
import java.util.List;
import static com.lutiano.api.security.Roles.SELLER;
import com.lutiano.api.service.loyalty.CustomerService;
import com.lutiano.api.models.request.loyalty.CustomerRequest;
import com.lutiano.api.models.response.loyalty.CustomerItemResponse;
import com.lutiano.api.models.response.loyalty.CustomerResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** Loyalty members (minimal customer records). Administrators only. */
@RestController
@RequestMapping("/customers")
public class CustomerController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final CustomerService service;
  private final CurrentUser current;

  private final SearchCandidates candidates;

  public CustomerController(CustomerService service, CurrentUser current, SearchCandidates candidates) { this.service = service; this.current = current; this.candidates = candidates; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<CustomerItemResponse>>> search(@RequestParam(required = false) String q, @RequestParam(required = false) String tier,
                                          @RequestParam(required = false) String status, PageRequest paging, Authentication a) {
    var admin = current.require(a, ADMIN);
    return Responses.page(service.search(candidates.find(admin, "CUSTOMER", q), tier, status, paging), CustomerItemResponse.class);
  }

  @GetMapping(ID + "/ledger")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> ledger(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.ledger(id, paging));
  }

  @GetMapping(ID + "/batches")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> batches(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.batches(id, paging));
  }

  @GetMapping(ID + "/redemptions")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> redemptions(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.redemptions(id, paging));
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> create(@RequestBody CustomerRequest body, Authentication a) { return Responses.created(service.create(current.require(a, ADMIN), body)); }

  /** A seller scans a customer's loyalty QR at checkout: only what is needed to confirm who it is (never the phone number). */
  @GetMapping("/scan")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> scan(@RequestParam String token, Authentication a) {
    current.require(a, ADMIN, SELLER);
    Map<String, Object> c = service.byToken(token).orElseThrow();
    Map<String, Object> out = new LinkedHashMap<>();
    for (String k : List.of("full_name", "code", "tier_name", "tier_color", "points_balance")) out.put(k, c.get(k));
    return Responses.ok(out);
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<CustomerResponse>> detail(@PathVariable String id, Authentication a) { current.require(a, ADMIN); return Responses.ok(service.detail(id), CustomerResponse.class); }

  @PutMapping(ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@PathVariable String id, @RequestBody CustomerRequest body, Authentication a) {
    return Responses.ok(service.update(current.require(a, ADMIN), id, body));
  }

  @GetMapping(value = ID + "/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qr(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return ResponseEntity.ok().header("Cache-Control", "no-store").body(service.qrPng(id));
  }

  @PostMapping(ID + "/reissue-qr")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reissue(@PathVariable String id, Authentication a) { return Responses.ok(service.rotateToken(current.require(a, ADMIN), id)); }
}
