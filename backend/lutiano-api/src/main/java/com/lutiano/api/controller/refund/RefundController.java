package com.lutiano.api.controller.refund;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.search.SearchCandidates;
import com.lutiano.api.service.refund.RefundService;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.refund.RefundPayRequest;
import com.lutiano.api.models.request.refund.RefundRequest;
import com.lutiano.api.models.request.refund.StockReturnRequest;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.refund.RefundItemResponse;
import com.lutiano.api.models.response.refund.RefundResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.refund.RefundSettingResponse;

/** Refunds are financially sensitive: administrators only. */
@RestController
@RequestMapping("/refunds")
public class RefundController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final RefundService service;
  private final CurrentUser current;
  private final SearchCandidates candidates;

  public RefundController(RefundService service, CurrentUser current, SearchCandidates candidates) { this.candidates = candidates; this.service = service; this.current = current; }


  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<RefundItemResponse>>> search(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) String q, PageRequest paging,
                                          @RequestParam(required = false) String status, @RequestParam(required = false) String operationId,
                                          @RequestParam(required = false) String sellerId, Authentication a) {
    var admin = current.require(a, ADMIN);
    return Responses.page(service.search(from, to, status, operationId, sellerId, candidates.find(admin, "REFUND", q), paging), RefundItemResponse.class);
  }

  @GetMapping("/settings")
  public ResponseEntity<GenericApiResponse<RefundSettingResponse>> settings(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(Map.of("secondApprover", service.requiresSecondApprover(), "reasons", RefundService.REASONS.stream().sorted().toList()), RefundSettingResponse.class);
  }

  /** What is still refundable on a sale, by saleId or saleReference. */
  @GetMapping("/sale")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> sale(@RequestParam(required = false) String saleId, @RequestParam(required = false) String saleReference, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.saleSummary(saleId, saleReference));
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> request(@RequestBody RefundRequest body, Authentication a) {
    return Responses.created(service.request(current.require(a, ADMIN), body));
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<RefundResponse>> detail(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.detail(id), RefundResponse.class);
  }

  @PostMapping(ID + "/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approve(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approve(current.require(a, ADMIN), id, body == null ? null : body.note()));
  }

  @PostMapping(ID + "/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.reject(current.require(a, ADMIN), id, body.note()));
  }

  @PostMapping(ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.cancel(current.require(a, ADMIN), id, body.note()));
  }

  @PostMapping(ID + "/pay")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> pay(@PathVariable String id, @RequestBody RefundPayRequest body, Authentication a) {
    return Responses.ok(service.pay(current.require(a, ADMIN), id, body));
  }

  @PostMapping(ID + "/stock-returns")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> stockReturn(@PathVariable String id, @RequestBody StockReturnRequest body, Authentication a) {
    return Responses.ok(service.requestStockReturn(current.require(a, ADMIN), id, body));
  }

  @PostMapping(ID + "/evidence")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> addEvidence(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.created(service.addEvidence(current.require(a, ADMIN), id, file));
  }

  @GetMapping(ID + "/evidence/{evidenceId:[0-9a-fA-F-]+}")
  public ResponseEntity<byte[]> evidence(@PathVariable String id, @PathVariable String evidenceId, Authentication a) throws Exception {
    current.require(a, ADMIN);
    RefundService.EvidenceFile f = service.evidence(id, evidenceId);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(f.fileName()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(f.bytes());
  }
}
