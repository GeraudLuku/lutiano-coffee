package com.lutiano.api.controller.payment;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.search.SearchCandidates;
import com.lutiano.api.service.payment.PaymentService;
import com.lutiano.api.models.request.payment.PaymentMethodRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.payment.PaymentItemResponse;
import com.lutiano.api.models.response.payment.PaymentMethodResponse;
import com.lutiano.api.models.response.payment.PaymentResponse;
import com.lutiano.api.models.response.payment.ReconciliationResponse;
import com.lutiano.api.utils.Responses;

/** Payments and reconciliation are finance information: administrators only. Every signed-in user can read the method list. */
@RestController
public class PaymentController {
  private final PaymentService service;
  private final CurrentUser current;
  private final SearchCandidates candidates;

  public PaymentController(PaymentService service, CurrentUser current, SearchCandidates candidates) { this.candidates = candidates; this.service = service; this.current = current; }

  @GetMapping("/payment-methods")
  public ResponseEntity<GenericApiResponse<List<PaymentMethodResponse>>> methods(Authentication a) {
    current.get(a);
    return Responses.list(service.methods(), PaymentMethodResponse.class);
  }

  @PutMapping("/payment-methods/{code}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateMethod(@PathVariable String code, @RequestBody PaymentMethodRequest body, Authentication a) {
    return Responses.ok(service.updateMethod(current.require(a, ADMIN), code, body));
  }

  @GetMapping("/payments")
  public ResponseEntity<GenericApiResponse<PageResponse<PaymentItemResponse>>> search(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) String method, @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String operationId, @RequestParam(required = false) String sellerId,
                                          @RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    var admin = current.require(a, ADMIN);
    return Responses.page(service.search(from, to, method, status, operationId, sellerId, candidates.find(admin, "PAYMENT", q), paging), PaymentItemResponse.class);
  }

  @GetMapping("/payments/reconciliation")
  public ResponseEntity<GenericApiResponse<ReconciliationResponse>> reconciliation(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.reconciliation(from, to), ReconciliationResponse.class);
  }

  @GetMapping("/payments/{id:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<PaymentResponse>> detail(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.detail(id), PaymentResponse.class);
  }
}
