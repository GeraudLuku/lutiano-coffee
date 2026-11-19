package com.lutiano.api.controller.reports;

import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lutiano.api.service.reports.ReportService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.reports.ReportsOverviewResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.reports.SaleResponse;

/** Reports and analytics. Business figures: administrators only. */
@RestController
@RequestMapping("/reports")
public class ReportController {
  private final ReportService service;
  private final CurrentUser current;

  public ReportController(ReportService service, CurrentUser current) { this.service = service; this.current = current; }

  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<ReportsOverviewResponse>> overview(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.overview(from, to), ReportsOverviewResponse.class);
  }

  @GetMapping("/sales")
  public ResponseEntity<GenericApiResponse<SaleResponse>> sales(@RequestParam(required = false) String groupBy, @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) String operationId, @RequestParam(required = false) String channel,
                                   @RequestParam(required = false) String sellerId, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.sales(groupBy, from, to, operationId, channel, sellerId), SaleResponse.class);
  }

  @GetMapping("/builder")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> builder(@RequestParam List<String> metrics, @RequestParam(required = false) String groupBy,
                                     @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(service.builder(metrics, groupBy, from, to));
  }
}
