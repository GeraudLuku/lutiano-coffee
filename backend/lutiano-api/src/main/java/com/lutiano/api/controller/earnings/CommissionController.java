package com.lutiano.api.controller.earnings;

import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.earnings.CommissionService;
import com.lutiano.api.service.earnings.TargetService;
import com.lutiano.api.models.request.earnings.CommissionRuleRequest;
import com.lutiano.api.models.request.common.ReasonRequest;
import com.lutiano.api.models.request.earnings.TargetRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.earnings.LineItemResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.earnings.RuleResponse;
import com.lutiano.api.models.response.earnings.TargetItemResponse;

/** Commission rules, posted commission lines and seller targets. Administrators only. */
@RestController
public class CommissionController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final CommissionService commissions;
  private final TargetService targets;
  private final CurrentUser current;

  public CommissionController(CommissionService commissions, TargetService targets, CurrentUser current) {
    this.commissions = commissions; this.targets = targets; this.current = current;
  }


  /** Current rules (one per product and seller role); a short configuration list. */
  @GetMapping("/commissions/rules")
  public ResponseEntity<GenericApiResponse<List<RuleResponse>>> rules(@RequestParam(required = false) String sellerRole, Authentication a) {
    current.require(a, ADMIN);
    return Responses.list(commissions.rules(sellerRole), RuleResponse.class);
  }

  @PostMapping("/commissions/rules")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> saveRule(@RequestBody CommissionRuleRequest body, Authentication a) {
    return Responses.created(commissions.saveRule(current.require(a, ADMIN), body));
  }

  @DeleteMapping("/commissions/rules" + ID)
  public ResponseEntity<Void> endRule(@PathVariable String id, Authentication a) { commissions.endRule(current.require(a, ADMIN), id);
    return Responses.noContent();
  }

  @GetMapping("/commissions/rules/history")
  public ResponseEntity<GenericApiResponse<PageResponse<RuleResponse>>> ruleHistory(@RequestParam String productId, @RequestParam String sellerRole, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(commissions.history(productId, sellerRole, paging), RuleResponse.class);
  }

  /** Commission per sale line, newest first; the summary totals every match. */
  @GetMapping("/commissions/lines")
  public ResponseEntity<GenericApiResponse<PageResponse<LineItemResponse>>> lines(@RequestParam(required = false) String sellerId, @RequestParam(required = false) LocalDate from,
                                         @RequestParam(required = false) LocalDate to, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(commissions.lines(sellerId, from, to, paging), LineItemResponse.class);
  }

  // ----- targets -----

  /** state: CURRENT, UPCOMING or ENDED. */
  @GetMapping("/targets")
  public ResponseEntity<GenericApiResponse<PageResponse<TargetItemResponse>>> targets(@RequestParam(required = false) String sellerId, @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String state, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(targets.list(sellerId, status, state, paging), TargetItemResponse.class);
  }

  @GetMapping("/targets" + ID)
  public ResponseEntity<GenericApiResponse<TargetItemResponse>> target(@PathVariable String id, Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(targets.get(id), TargetItemResponse.class);
  }

  @PostMapping("/targets")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> createTarget(@RequestBody TargetRequest body, Authentication a) { return Responses.created(targets.create(current.require(a, ADMIN), body)); }

  @PostMapping("/targets" + ID + "/cancel")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> cancelTarget(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(targets.cancel(current.require(a, ADMIN), id, body.reason()));
  }
}
