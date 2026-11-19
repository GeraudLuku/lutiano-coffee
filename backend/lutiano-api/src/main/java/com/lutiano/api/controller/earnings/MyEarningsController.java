package com.lutiano.api.controller.earnings;

import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.CurrentUser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.earnings.MyEarningsService;
import com.lutiano.api.models.response.earnings.BreakdownResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.earnings.MonthResponse;
import com.lutiano.api.models.response.earnings.EarningsOperationResponse;
import com.lutiano.api.models.response.earnings.EarningsOverviewV2Response;
import com.lutiano.api.models.response.earnings.PayoutItemResponse;
import com.lutiano.api.models.response.earnings.PayoutResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.earnings.RuleItemResponse;
import com.lutiano.api.models.response.earnings.SaleItemResponse;
import com.lutiano.api.models.response.earnings.SeryResponse;

/**
 * A seller's own earnings: overview and breakdown for a month, trends, the sales that earned commission, payouts, the
 * commission rules for their role and a PDF statement. Sellers only, and only their own data.
 * Month parameters are yyyy-MM (this month when omitted). The series endpoints return a bounded run of points for charts, not a list.
 */
@RestController
@RequestMapping("/me/earnings")
public class MyEarningsController {
  private final MyEarningsService service;
  private final CurrentUser current;

  public MyEarningsController(MyEarningsService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<EarningsOverviewV2Response>> overview(@RequestParam(required = false) String month, Authentication a) {
    return Responses.ok(service.overview(current.require(a, SELLER), month), EarningsOverviewV2Response.class);
  }

  @GetMapping("/breakdown")
  public ResponseEntity<GenericApiResponse<BreakdownResponse>> breakdown(@RequestParam(required = false) String month, Authentication a) {
    return Responses.ok(service.breakdown(current.require(a, SELLER), month), BreakdownResponse.class);
  }

  @GetMapping("/months")
  public ResponseEntity<GenericApiResponse<List<MonthResponse>>> months(@RequestParam(defaultValue = "6") int count, Authentication a) {
    return Responses.list(service.months(current.require(a, SELLER), count), MonthResponse.class);
  }

  @GetMapping("/series")
  public ResponseEntity<GenericApiResponse<List<SeryResponse>>> series(@RequestParam(defaultValue = "MONTH") String granularity, @RequestParam(defaultValue = "6") int count, Authentication a) {
    return Responses.list(service.series(current.require(a, SELLER), granularity, count), SeryResponse.class);
  }

  @GetMapping("/operations")
  public ResponseEntity<GenericApiResponse<List<EarningsOperationResponse>>> operations(@RequestParam(required = false) String month, Authentication a) {
    return Responses.list(service.operations(current.require(a, SELLER), month), EarningsOperationResponse.class);
  }

  @GetMapping("/sales")
  public ResponseEntity<GenericApiResponse<PageResponse<SaleItemResponse>>> sales(@RequestParam(required = false) String month, PageRequest paging, Authentication a) {
    return Responses.page(service.sales(current.require(a, SELLER), month, paging), SaleItemResponse.class);
  }

  @GetMapping("/sales/{id}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> sale(@PathVariable String id, Authentication a) {
    return Responses.ok(service.sale(current.require(a, SELLER), id));
  }

  @GetMapping("/payouts")
  public ResponseEntity<GenericApiResponse<PageResponse<PayoutItemResponse>>> payouts(@RequestParam(required = false) String status, PageRequest paging, Authentication a) {
    return Responses.page(service.payouts(current.require(a, SELLER), status, paging), PayoutItemResponse.class);
  }

  @GetMapping("/payouts/{id}")
  public ResponseEntity<GenericApiResponse<PayoutResponse>> payout(@PathVariable String id, Authentication a) {
    return Responses.ok(service.payout(current.require(a, SELLER), id), PayoutResponse.class);
  }

  @GetMapping("/rules")
  public ResponseEntity<GenericApiResponse<PageResponse<RuleItemResponse>>> rules(PageRequest paging, Authentication a) {
    return Responses.page(service.rules(current.require(a, SELLER), paging), RuleItemResponse.class);
  }

  /** sections: comma-separated SUMMARY, BREAKDOWN, BONUSES, ADJUSTMENTS, SALES. lang: fr or en. */
  @GetMapping(value = "/statement.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> statement(@RequestParam String from, @RequestParam String to, @RequestParam String sections,
                                          @RequestParam(defaultValue = "en") String lang, Authentication a) {
    Set<String> parts = new HashSet<>(Arrays.asList(sections.toUpperCase().split(",")));
    byte[] pdf = service.statementPdf(current.require(a, SELLER), from, to, parts, Validate.oneOf(lang.toLowerCase(), "Language", Set.of("fr", "en")));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("lutiano-earnings-" + from + "-" + to + ".pdf").build().toString())
        .contentType(MediaType.APPLICATION_PDF).body(pdf);
  }
}
