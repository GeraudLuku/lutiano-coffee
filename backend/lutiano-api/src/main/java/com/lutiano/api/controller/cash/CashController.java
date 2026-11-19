package com.lutiano.api.controller.cash;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;
import static com.lutiano.api.security.Roles.SELLER;
import static com.lutiano.api.security.Roles.STAFF;

import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.cash.CashService;
import com.lutiano.api.models.request.cash.AcknowledgementRequest;
import com.lutiano.api.models.request.common.NoteRequest;
import com.lutiano.api.models.request.cash.OutflowRequest;
import com.lutiano.api.models.request.cash.RecountRequest;
import com.lutiano.api.models.request.cash.RemittanceRequest;
import com.lutiano.api.models.request.cash.ShiftReviewRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.cash.LedgerItemResponse;
import com.lutiano.api.models.response.cash.OutflowItemResponse;
import com.lutiano.api.models.response.cash.OverviewResponse;
import com.lutiano.api.models.response.cash.RecipientResponse;
import com.lutiano.api.models.response.cash.RemittanceItemResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.cash.ShiftItemResponse;
import com.lutiano.api.models.response.cash.ShiftResponse;
import com.lutiano.api.models.response.cash.SummaryResponse;

/**
 * Cash custody. Sellers request outflows, record remittances and recount their own shifts; staff see every shift and
 * acknowledge handovers made to them; administrators approve outflows, confirm bank deposits and review variances.
 */
@RestController
@RequestMapping("/cash")
public class CashController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final CashService service;
  private final CurrentUser current;

  public CashController(CashService service, CurrentUser current) { this.service = service; this.current = current; }


  @GetMapping("/summary")
  public ResponseEntity<GenericApiResponse<SummaryResponse>> summary(Authentication a) {
    current.require(a, STAFF);
    return Responses.ok(service.summary(), SummaryResponse.class);
  }

  /** The signed-in seller's cash items that need attention: undecided outflows, unconfirmed or disputed remittances, recounts asked for. */
  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<OverviewResponse>> overview(Authentication a) {
    return Responses.ok(service.overview(current.require(a, SELLER)), OverviewResponse.class);
  }

  /** Sellers get their own recent shifts. */
  @GetMapping("/shifts")
  public ResponseEntity<GenericApiResponse<PageResponse<ShiftItemResponse>>> shifts(@RequestParam(required = false) String status, @RequestParam(required = false) String review,
                                          @RequestParam(required = false) String q, PageRequest paging, Authentication a) {
    return Responses.page(service.shifts(current.get(a), status, review, q, paging), ShiftItemResponse.class);
  }

  @GetMapping("/shifts" + ID + "/ledger")
  public ResponseEntity<GenericApiResponse<PageResponse<LedgerItemResponse>>> shiftLedger(@PathVariable String id, PageRequest paging, Authentication a) {
    return Responses.page(service.ledger(current.get(a), id, paging), LedgerItemResponse.class);
  }

  @GetMapping("/shifts" + ID)
  public ResponseEntity<GenericApiResponse<ShiftResponse>> shift(@PathVariable String id, Authentication a) { return Responses.ok(service.shift(current.get(a), id), ShiftResponse.class); }

  @PostMapping("/shifts" + ID + "/review")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> review(@PathVariable String id, @RequestBody ShiftReviewRequest body, Authentication a) {
    return Responses.ok(service.review(current.require(a, ADMIN), id, body));
  }

  @PostMapping("/shifts" + ID + "/recount")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> recount(@PathVariable String id, @RequestBody RecountRequest body, Authentication a) {
    return Responses.ok(service.recount(current.require(a, SELLER), id, body));
  }

  // ----- outflows -----

  @GetMapping("/outflows")
  public ResponseEntity<GenericApiResponse<PageResponse<OutflowItemResponse>>> outflows(@RequestParam(required = false) String status, PageRequest paging, Authentication a) {
    return Responses.page(service.outflows(current.get(a), status, paging), OutflowItemResponse.class);
  }

  @PostMapping("/outflows")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> requestOutflow(@RequestBody OutflowRequest body, Authentication a) {
    return Responses.created(service.requestOutflow(current.require(a, SELLER), body));
  }

  @PostMapping("/outflows" + ID + "/approve")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> approveOutflow(@PathVariable String id, @RequestBody(required = false) NoteRequest body, Authentication a) {
    return Responses.ok(service.approveOutflow(current.require(a, ADMIN), id, body == null ? null : body.note()));
  }

  @PostMapping("/outflows" + ID + "/reject")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> rejectOutflow(@PathVariable String id, @RequestBody NoteRequest body, Authentication a) {
    return Responses.ok(service.rejectOutflow(current.require(a, ADMIN), id, body.note()));
  }

  @PostMapping("/outflows" + ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancelOutflow(@PathVariable String id, Authentication a) {
    return Responses.ok(service.cancelOutflow(current.require(a, SELLER, ADMIN), id));
  }

  // ----- remittances -----

  /** mine=true: handovers made to the signed-in staff member. */
  @GetMapping("/remittances")
  public ResponseEntity<GenericApiResponse<PageResponse<RemittanceItemResponse>>> remittances(@RequestParam(required = false) String status, @RequestParam(defaultValue = "false") boolean mine,
                                               PageRequest paging, Authentication a) {
    return Responses.page(service.remittances(current.get(a), status, mine, paging), RemittanceItemResponse.class);
  }

  @GetMapping("/recipients")
  public ResponseEntity<GenericApiResponse<List<RecipientResponse>>> recipients(Authentication a) {
    current.get(a);
    return Responses.list(service.recipients(), RecipientResponse.class);
  }

  @PostMapping("/remittances")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> remit(@RequestBody RemittanceRequest body, Authentication a) {
    return Responses.created(service.recordRemittance(current.require(a, SELLER), body));
  }

  @PostMapping("/remittances" + ID + "/acknowledge")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> acknowledge(@PathVariable String id, @RequestBody AcknowledgementRequest body, Authentication a) {
    return Responses.ok(service.acknowledge(current.require(a, STAFF), id, body));
  }
}
