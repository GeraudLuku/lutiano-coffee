package com.lutiano.api.controller.sales;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.SELLER;

import java.time.LocalDate;
import java.util.Map;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.security.Roles;
import com.lutiano.api.service.sales.ReceiptPdfRenderer;
import com.lutiano.api.service.sales.SaleService;
import com.lutiano.api.models.request.sales.SaleOnBehalfRequest;
import com.lutiano.api.models.request.sales.SaleRequest;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.sales.SalesSaleResponse;
import com.lutiano.api.models.response.sales.SalesSaleItemResponse;

@RestController
@RequestMapping("/sales")
public class SalesController {
  private final SaleService service;
  private final ReceiptPdfRenderer receipts;
  private final CurrentUser current;

  public SalesController(SaleService service, ReceiptPdfRenderer receipts, CurrentUser current) {
    this.service = service; this.receipts = receipts; this.current = current;
  }

  /** 201 for a new sale, 200 when an idempotent retry returns the original. */
  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> complete(@RequestBody SaleRequest body, @RequestHeader("Idempotency-Key") String key, Authentication a) {
    SaleService.SaleResult r = service.complete(current.require(a, SELLER), key, body);
    return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(GenericApiResponse.ok(r.sale()));
  }


  @PostMapping("/on-behalf")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> completeOnBehalf(@RequestBody SaleOnBehalfRequest body, @RequestHeader("Idempotency-Key") String key, Authentication a) {
    var admin = current.require(a, Roles.ADMIN);
    SaleService.SaleResult r = service.completeOnBehalf(admin, current.activeSeller(body.sellerId()), key,
        new SaleRequest(body.operationId(), body.lines(), body.cashReceivedXaf(), body.paymentMethod(), body.customerToken()));
    return ResponseEntity.status(r.created() ? HttpStatus.CREATED : HttpStatus.OK).body(GenericApiResponse.ok(r.sale()));
  }

  /** Dates are Douala business days, inclusive. Sellers only ever see their own sales. */
  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<SalesSaleItemResponse>>> search(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) String operationId, @RequestParam(required = false) String sellerId,
                                          @RequestParam(required = false) String channel, @RequestParam(required = false) String paymentMethod,
                                          @RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    return Responses.page(service.search(current.get(a), from, to, operationId, sellerId, channel, paymentMethod, q, paging), SalesSaleItemResponse.class);
  }

  @GetMapping("/{id}")
  public ResponseEntity<GenericApiResponse<SalesSaleResponse>> get(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.get(a), id), SalesSaleResponse.class); }

  /** Kept for existing clients; the sale detail already includes its lines. */
  @GetMapping("/{id}/lines")
  public ResponseEntity<GenericApiResponse<Object>> lines(@PathVariable String id, Authentication a) { return Responses.ok(service.get(current.get(a), id).get("lines")); }

  @GetMapping("/{id}/receipt.pdf")
  public ResponseEntity<byte[]> receipt(@PathVariable String id, Authentication a) {
    Actor actor = current.get(a);
    Map<String, Object> sale = service.get(actor, id);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=lutiano-receipt-" + sale.get("reference") + ".pdf")
        .body(receipts.render(sale));
  }
}
