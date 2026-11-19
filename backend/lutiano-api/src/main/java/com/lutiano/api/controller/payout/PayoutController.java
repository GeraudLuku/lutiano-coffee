package com.lutiano.api.controller.payout;

import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.Map;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.search.SearchCandidates;
import com.lutiano.api.service.payout.DestinationService;
import com.lutiano.api.service.payout.PayoutService;
import com.lutiano.api.models.request.payout.ApprovalThresholdRequest;
import com.lutiano.api.models.request.common.CodeRequest;
import com.lutiano.api.models.request.payout.DestinationRequest;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.payout.PayoutPayRequest;
import com.lutiano.api.models.request.common.ReasonRequest;
import com.lutiano.api.models.request.payout.RecordPayoutRequest;
import com.lutiano.api.models.response.payout.BalanceItemResponse;
import com.lutiano.api.models.response.payout.BalanceResponse;
import com.lutiano.api.models.response.payout.DestinationItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.payout.PayoutOverviewResponse;
import com.lutiano.api.models.response.payout.PayoutV2Response;
import com.lutiano.api.models.response.payout.PayoutItemV2Response;
import com.lutiano.api.models.response.payout.PayoutPeriodItemResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.payout.PayoutSettingResponse;

/** Seller payouts and payout numbers. Money is involved: administrators only. */
@RestController
@RequestMapping("/payouts")
public class PayoutController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final PayoutService service;
  private final DestinationService destinations;
  private final CurrentUser current;
  private final SearchCandidates candidates;

  public PayoutController(PayoutService service, DestinationService destinations, CurrentUser current, SearchCandidates candidates) {
    this.candidates = candidates;
    this.service = service; this.destinations = destinations; this.current = current;
  }


  // ----- overview, balances, settings -----

  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<PayoutOverviewResponse>> overview(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.overview(), PayoutOverviewResponse.class);
  }

  @GetMapping("/settings")
  public ResponseEntity<GenericApiResponse<PayoutSettingResponse>> settings(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(Map.of("approvalThresholdXaf", service.approvalThreshold()), PayoutSettingResponse.class);
  }

  @PutMapping("/settings")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> setSettings(@RequestBody ApprovalThresholdRequest body, Authentication a) {
    return Responses.ok(Map.of("approvalThresholdXaf", service.setApprovalThreshold(current.require(a, ADMIN), body.approvalThresholdXaf())));
  }

  @GetMapping("/balances")
  public ResponseEntity<GenericApiResponse<PageResponse<BalanceItemResponse>>> balances(@RequestParam(required = false) String q, @RequestParam(defaultValue = "false") boolean owedOnly,
                                            PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.balances(q, owedOnly, paging), BalanceItemResponse.class);
  }

  @GetMapping("/balances/{sellerId:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<BalanceResponse>> sellerBalance(@PathVariable String sellerId, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.sellerBalance(sellerId), BalanceResponse.class);
  }

  /** A seller's finalised periods with what was paid and what is left; {@code openOnly} keeps those with something left to pay. */
  @GetMapping("/balances/{sellerId:[0-9a-fA-F-]+}/periods")
  public ResponseEntity<GenericApiResponse<PageResponse<PayoutPeriodItemResponse>>> periods(@PathVariable String sellerId, @RequestParam(defaultValue = "false") boolean openOnly,
                                           PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.periods(sellerId, openOnly, paging), PayoutPeriodItemResponse.class);
  }

  // ----- payout numbers -----

  @GetMapping("/destinations")
  public ResponseEntity<GenericApiResponse<PageResponse<DestinationItemResponse>>> destinations(@RequestParam(required = false) String status, @RequestParam(required = false) String provider,
                                                @RequestParam(required = false) String sellerId, @RequestParam(required = false) String q,
                                                PageRequest paging, Authentication a) {
    var admin = current.require(a, ADMIN);
    return Responses.page(destinations.search(status, provider, sellerId, candidates.find(admin, "PAYOUT_DESTINATION", q), paging), DestinationItemResponse.class);
  }

  @PostMapping("/destinations")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> addDestination(@RequestBody DestinationRequest body, Authentication a) {
    return Responses.created(destinations.add(current.require(a, ADMIN), body));
  }

  @PostMapping("/destinations" + ID + "/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verifyDestination(@PathVariable String id, @RequestBody CodeRequest body, Authentication a) {
    return Responses.ok(destinations.verify(current.require(a, ADMIN), id, body.code()));
  }

  @PostMapping("/destinations" + ID + "/resend-code")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resendCode(@PathVariable String id, Authentication a) {
    destinations.resendCode(current.require(a, ADMIN), id);
    return Responses.ok(Map.of("status", "sent"));
  }

  @PostMapping("/destinations" + ID + "/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> rejectDestination(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(destinations.reject(current.require(a, ADMIN), id, body.reason()));
  }

  // ----- payouts -----

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<PayoutItemV2Response>>> search(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) String status, @RequestParam(required = false) String method,
                                          @RequestParam(required = false) String sellerId, @RequestParam(required = false) String q,
                                          PageRequest paging, Authentication a) {
    var admin = current.require(a, ADMIN);
    return Responses.page(service.search(from, to, status, method, sellerId, candidates.find(admin, "PAYOUT", q), paging), PayoutItemV2Response.class);
  }

  @PostMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> record(@RequestBody RecordPayoutRequest body, Authentication a) {
    return Responses.created(service.record(current.require(a, ADMIN), body));
  }

  @GetMapping(ID)
  public ResponseEntity<GenericApiResponse<PayoutV2Response>> detail(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.detail(id), PayoutV2Response.class);
  }

  @PostMapping(ID + "/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approve(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approve(current.require(a, ADMIN), id, body == null ? null : body.note()));
  }

  @PostMapping(ID + "/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reject(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(service.reject(current.require(a, ADMIN), id, body.reason()));
  }

  @PostMapping(ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancel(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.cancel(current.require(a, ADMIN), id, body == null ? null : body.note()));
  }

  @PostMapping(ID + "/pay")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> pay(@PathVariable String id, @RequestBody(required = false) PayoutPayRequest body, Authentication a) {
    return Responses.ok(service.pay(current.require(a, ADMIN), id, body));
  }

  @PostMapping(ID + "/reversal")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> requestReversal(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(service.requestReversal(current.require(a, ADMIN), id, body.reason()));
  }

  @PostMapping(ID + "/reversal/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approveReversal(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approveReversal(current.require(a, ADMIN), id, body == null ? null : body.note()));
  }

  @PostMapping(ID + "/reversal/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> rejectReversal(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(service.rejectReversal(current.require(a, ADMIN), id, body.reason()));
  }

  @PostMapping(ID + "/evidence")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> addEvidence(@PathVariable String id, @RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.created(service.addEvidence(current.require(a, ADMIN), id, file));
  }

  @GetMapping(ID + "/evidence/{evidenceId:[0-9a-fA-F-]+}")
  public ResponseEntity<byte[]> evidence(@PathVariable String id, @PathVariable String evidenceId, Authentication a) throws Exception {
    current.require(a, ADMIN);
    PayoutService.EvidenceFile f = service.evidence(id, evidenceId);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(f.fileName()).build().toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .body(f.bytes());
  }
}
