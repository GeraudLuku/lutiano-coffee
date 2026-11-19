package com.lutiano.api.controller.earnings;

import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.Map;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.earnings.EarningsService;
import com.lutiano.api.models.request.earnings.EarningsAdjustmentRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.earnings.AdjustmentItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.earnings.EarningsOverviewResponse;
import com.lutiano.api.models.response.earnings.PeriodItemResponse;
import com.lutiano.api.models.response.earnings.PeriodResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.earnings.SellerItemResponse;
import com.lutiano.api.models.response.earnings.SellerResponse;
import com.lutiano.api.models.response.earnings.StatementItemResponse;

/** Earning periods, seller statements and adjustments. Administrators only. */
@RestController
@RequestMapping("/earnings")
public class EarningsController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final EarningsService service;
  private final CurrentUser current;

  public EarningsController(EarningsService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<EarningsOverviewResponse>> overview(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.overview(), EarningsOverviewResponse.class);
  }

  @GetMapping("/periods")
  public ResponseEntity<GenericApiResponse<PageResponse<PeriodItemResponse>>> periods(PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.periods(paging), PeriodItemResponse.class);
  }

  @GetMapping("/periods" + ID)
  public ResponseEntity<GenericApiResponse<PeriodResponse>> period(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.period(id), PeriodResponse.class);
  }

  /** Seller statements of a period (frozen when finalised, live while open), highest total first. */
  @GetMapping("/periods" + ID + "/sellers")
  public ResponseEntity<GenericApiResponse<PageResponse<SellerItemResponse>>> periodSellers(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.periodSellers(id, paging), SellerItemResponse.class);
  }

  @PostMapping("/periods" + ID + "/finalize")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> finalize(@PathVariable String id, Authentication a) { return Responses.ok(service.finalize(current.require(a, ADMIN), id)); }

  @GetMapping("/sellers" + ID)
  public ResponseEntity<GenericApiResponse<SellerResponse>> seller(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.seller(id), SellerResponse.class);
  }

  @GetMapping("/sellers" + ID + "/statements")
  public ResponseEntity<GenericApiResponse<PageResponse<StatementItemResponse>>> statements(@PathVariable String id, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.sellerStatements(id, paging), StatementItemResponse.class);
  }

  @GetMapping("/adjustments")
  public ResponseEntity<GenericApiResponse<PageResponse<AdjustmentItemResponse>>> adjustments(@RequestParam(required = false) String sellerId, @RequestParam(required = false) String type,
                                               @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                               PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(service.adjustments(sellerId, type, from, to, paging), AdjustmentItemResponse.class);
  }

  @PostMapping("/adjustments")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> adjust(@RequestBody EarningsAdjustmentRequest body, Authentication a) { return Responses.created(service.addManual(current.require(a, ADMIN), body)); }
}
