package com.lutiano.api.controller.earnings;

import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.earnings.MyTargetsService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.earnings.BonuseItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.earnings.PerformanceResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.earnings.EarningsSummaryResponse;
import com.lutiano.api.models.response.earnings.EarningsTargetItemResponse;

/** A seller's own targets, bonuses and performance. Sellers only, and only their own data. Month parameters are yyyy-MM. */
@RestController
@RequestMapping("/me/targets")
public class MyTargetsController {
  private final MyTargetsService service;
  private final CurrentUser current;

  public MyTargetsController(MyTargetsService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping
  public ResponseEntity<GenericApiResponse<PageResponse<EarningsTargetItemResponse>>> list(@RequestParam(required = false) String state, @RequestParam(required = false) String month,
                                        PageRequest paging, Authentication a) {
    return Responses.page(service.list(current.require(a, SELLER), state, month, paging), EarningsTargetItemResponse.class);
  }

  @GetMapping("/summary")
  public ResponseEntity<GenericApiResponse<EarningsSummaryResponse>> summary(@RequestParam(required = false) String month, Authentication a) {
    return Responses.ok(service.summary(current.require(a, SELLER), month), EarningsSummaryResponse.class);
  }

  @GetMapping("/bonuses")
  public ResponseEntity<GenericApiResponse<PageResponse<BonuseItemResponse>>> bonuses(@RequestParam(required = false) String status, PageRequest paging, Authentication a) {
    return Responses.page(service.bonuses(current.require(a, SELLER), status, paging), BonuseItemResponse.class);
  }

  /** The last N months (1 to 12) of sales and target results: a bounded series for charts, not a list. */
  @GetMapping("/performance")
  public ResponseEntity<GenericApiResponse<PerformanceResponse>> performance(@RequestParam(defaultValue = "6") int count, Authentication a) {
    return Responses.ok(service.performance(current.require(a, SELLER), count), PerformanceResponse.class);
  }

  @GetMapping("/{id:[0-9a-fA-F-]+}")
  public ResponseEntity<GenericApiResponse<EarningsTargetItemResponse>> get(@PathVariable String id, Authentication a) {
    return Responses.ok(service.get(current.require(a, SELLER), id), EarningsTargetItemResponse.class);
  }
}
