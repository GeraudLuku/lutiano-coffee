package com.lutiano.api.controller.dashboard;

import java.time.LocalDate;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lutiano.api.service.dashboard.DashboardService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.dashboard.DashboardResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

@RestController
public class DashboardController {
  private final DashboardService service;
  private final CurrentUser current;

  public DashboardController(DashboardService service, CurrentUser current) { this.service = service; this.current = current; }

  /** Either ?days=N (last N days) or ?from=YYYY-MM-DD&to=YYYY-MM-DD (Douala days, inclusive). */
  @GetMapping("/dashboard")
  public ResponseEntity<GenericApiResponse<DashboardResponse>> dashboard(@RequestParam(required = false) Integer days,
                                       @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                       Authentication a) {
    return Responses.ok(service.summary(days, from, to, current.require(a, Roles.STAFF).is(Roles.ADMIN)), DashboardResponse.class);
  }
}
