package com.lutiano.api.controller.loyalty;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import static com.lutiano.api.security.Roles.ADMIN;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.loyalty.LoyaltyConfigService;
import com.lutiano.api.service.loyalty.PointsService;
import com.lutiano.api.models.request.loyalty.EarningRuleRequest;
import com.lutiano.api.models.request.loyalty.LoyaltySettingsRequest;
import com.lutiano.api.models.request.loyalty.PointsAdjustmentRequest;
import com.lutiano.api.models.request.common.ReasonRequest;
import com.lutiano.api.models.request.loyalty.RedeemRequest;
import com.lutiano.api.models.request.loyalty.RewardRequest;
import com.lutiano.api.models.request.loyalty.TierRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.loyalty.LoyaltyAdjustmentItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyLedgerItemResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyOverviewV2Response;
import com.lutiano.api.models.response.loyalty.RedemptionItemResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.loyalty.LoyaltyRewardResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyRuleV2Response;
import com.lutiano.api.models.response.loyalty.SettingResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyTierResponse;

/** Loyalty programme management: administrators only. */
@RestController
@RequestMapping("/loyalty")
public class LoyaltyController {
  private static final String ID = "/{id:[0-9a-fA-F-]+}";
  private final LoyaltyConfigService configService;
  private final PointsService pointsService;
  private final CurrentUser current;

  public LoyaltyController(LoyaltyConfigService configService, PointsService pointsService, CurrentUser current) {
    this.configService = configService; this.pointsService = pointsService; this.current = current;
  }


  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<LoyaltyOverviewV2Response>> overview(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to, Authentication a) {
    current.require(a, ADMIN);
    var out = pointsService.overview(from, to);
    return Responses.ok(out, LoyaltyOverviewV2Response.class);
  }

  @GetMapping("/ledger")
  public ResponseEntity<GenericApiResponse<PageResponse<LoyaltyLedgerItemResponse>>> ledger(@RequestParam(required = false) String type, @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(pointsService.ledger(type, from, to, paging), LoyaltyLedgerItemResponse.class);
  }

  // ----- settings, rules, tiers, rewards -----

  @GetMapping("/settings")
  public ResponseEntity<GenericApiResponse<SettingResponse>> settings(Authentication a) { current.require(a, ADMIN); return Responses.ok(configService.settings(), SettingResponse.class); }

  @PutMapping("/settings")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateSettings(@RequestBody LoyaltySettingsRequest body, Authentication a) {
    return Responses.ok(configService.updateSettings(current.require(a, ADMIN), body));
  }

  @GetMapping("/rules")
  public ResponseEntity<GenericApiResponse<List<LoyaltyRuleV2Response>>> rules(Authentication a) { current.require(a, ADMIN); return Responses.list(configService.rules(), LoyaltyRuleV2Response.class); }

  @PostMapping("/rules")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> createRule(@RequestBody EarningRuleRequest body, Authentication a) {
    return Responses.created(configService.saveRule(current.require(a, ADMIN), null, body));
  }

  @PutMapping("/rules" + ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateRule(@PathVariable String id, @RequestBody EarningRuleRequest body, Authentication a) {
    return Responses.ok(configService.saveRule(current.require(a, ADMIN), id, body));
  }

  @DeleteMapping("/rules" + ID)
  public ResponseEntity<Void> deleteRule(@PathVariable String id, Authentication a) { configService.deleteRule(current.require(a, ADMIN), id);
    return Responses.noContent();
  }

  @GetMapping("/tiers")
  public ResponseEntity<GenericApiResponse<List<LoyaltyTierResponse>>> tiers(Authentication a) { current.require(a, ADMIN); return Responses.list(configService.tiers(), LoyaltyTierResponse.class); }

  @PutMapping("/tiers/{code:[A-Z_]+}")
  public ResponseEntity<GenericApiResponse<List<Map<String, Object>>>> updateTier(@PathVariable String code, @RequestBody TierRequest body, Authentication a) {
    return Responses.ok(configService.updateTier(current.require(a, ADMIN), code, body));
  }

  @GetMapping("/rewards")
  public ResponseEntity<GenericApiResponse<List<LoyaltyRewardResponse>>> rewards(Authentication a) { current.require(a, ADMIN); return Responses.list(configService.rewards(), LoyaltyRewardResponse.class); }

  @PostMapping("/rewards")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> createReward(@RequestBody RewardRequest body, Authentication a) {
    return Responses.created(configService.saveReward(current.require(a, ADMIN), null, body));
  }

  @PutMapping("/rewards" + ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> updateReward(@PathVariable String id, @RequestBody RewardRequest body, Authentication a) {
    return Responses.ok(configService.saveReward(current.require(a, ADMIN), id, body));
  }

  // ----- redemptions, adjustments, expiry -----

  @GetMapping("/redemptions")
  public ResponseEntity<GenericApiResponse<PageResponse<RedemptionItemResponse>>> redemptions(@RequestParam(required = false) String status, @RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(pointsService.redemptions(status, from, to, paging), RedemptionItemResponse.class);
  }

  @PostMapping("/redemptions")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> redeem(@RequestBody RedeemRequest body, Authentication a) { return Responses.created(pointsService.redeem(current.require(a, ADMIN), body)); }

  @PostMapping("/redemptions" + ID + "/reverse")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reverse(@PathVariable String id, @RequestBody ReasonRequest body, Authentication a) {
    return Responses.ok(pointsService.reverseRedemption(current.require(a, ADMIN), id, body.reason()));
  }

  @GetMapping("/adjustments")
  public ResponseEntity<GenericApiResponse<PageResponse<LoyaltyAdjustmentItemResponse>>> adjustments(@RequestParam(required = false) String reason, @RequestParam(required = false) LocalDate from,
                                               @RequestParam(required = false) LocalDate to, PageRequest paging, Authentication a) {
    current.require(a, ADMIN);
    return Responses.page(pointsService.adjustments(reason, from, to, paging), LoyaltyAdjustmentItemResponse.class);
  }

  @PostMapping("/adjustments")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> adjust(@RequestBody PointsAdjustmentRequest body, Authentication a) { return Responses.created(pointsService.adjust(current.require(a, ADMIN), body)); }

  @PostMapping("/expire")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> expire(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(Map.of("expiredPoints", pointsService.expireAll()));
  }
}
