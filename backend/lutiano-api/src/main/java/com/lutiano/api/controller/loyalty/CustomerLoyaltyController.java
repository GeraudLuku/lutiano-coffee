package com.lutiano.api.controller.loyalty;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentCustomer;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.loyalty.CustomerLoyaltyService;
import com.lutiano.api.service.loyalty.WalletService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.loyalty.EstimateResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyOverviewResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.loyalty.RewardItemResponse;
import com.lutiano.api.models.response.loyalty.RewardResponse;
import com.lutiano.api.models.response.loyalty.LoyaltyRuleResponse;
import com.lutiano.api.models.response.loyalty.TierResponse;

/** A customer's own points, rewards and wallet. */
@RestController
@RequestMapping("/customer/loyalty")
public class CustomerLoyaltyController {
  private static final String ID = "/{id:[0-9a-fA-F-]{36}}";
  private final CustomerLoyaltyService loyalty;
  private final WalletService wallet;
  private final CurrentCustomer current;

  public CustomerLoyaltyController(CustomerLoyaltyService loyalty, WalletService wallet, CurrentCustomer current) { this.loyalty = loyalty; this.wallet = wallet; this.current = current; }

  @GetMapping("/overview")
  public ResponseEntity<GenericApiResponse<LoyaltyOverviewResponse>> overview(Authentication a) { return Responses.ok(loyalty.overview(current.get(a)), LoyaltyOverviewResponse.class); }

  @GetMapping("/tiers")
  public ResponseEntity<GenericApiResponse<List<TierResponse>>> tiers(Authentication a) { return Responses.list(loyalty.tiers(current.get(a)), TierResponse.class); }

  /** type: EARNED, USED or EXPIRED. */
  @GetMapping("/history")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> history(@RequestParam(required = false) String type, PageRequest paging, Authentication a) { return Responses.ok(loyalty.history(current.get(a), type, paging)); }

  @GetMapping("/expiries")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> expiries(PageRequest paging, Authentication a) { return Responses.ok(loyalty.expiries(current.get(a), paging)); }

  @GetMapping("/rules")
  public ResponseEntity<GenericApiResponse<LoyaltyRuleResponse>> rules() { return Responses.ok(loyalty.rules(), LoyaltyRuleResponse.class); }

  @GetMapping("/estimate")
  public ResponseEntity<GenericApiResponse<EstimateResponse>> estimate(@RequestParam Long amountXaf, Authentication a) { return Responses.ok(loyalty.estimate(current.get(a), amountXaf), EstimateResponse.class); }

  @GetMapping("/rewards")
  public ResponseEntity<GenericApiResponse<PageResponse<RewardItemResponse>>> rewards(PageRequest paging, Authentication a) { return Responses.page(loyalty.rewards(current.get(a), paging), RewardItemResponse.class); }

  @GetMapping("/rewards" + ID)
  public ResponseEntity<GenericApiResponse<RewardResponse>> reward(@PathVariable String id, Authentication a) { return Responses.ok(loyalty.reward(current.get(a), id), RewardResponse.class); }

  /** Spends the points and puts the reward in the wallet. */
  @PostMapping("/rewards" + ID + "/redeem")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> redeem(@PathVariable String id, Authentication a) { return Responses.ok(wallet.redeem(current.get(a), id)); }

  /** The wallet: state AVAILABLE, USED or EXPIRED. */
  @GetMapping("/redemptions")
  public ResponseEntity<GenericApiResponse<PageResponse<Map<String, Object>>>> redemptions(@RequestParam(required = false) String state, PageRequest paging, Authentication a) { return Responses.ok(wallet.mine(current.get(a), state, paging)); }

  /** One wallet item; while it can be used it carries `qr` to show at the sales point. */
  @GetMapping("/redemptions" + ID)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> redemption(@PathVariable String id, Authentication a) { return Responses.ok(wallet.item(current.get(a), id)); }
}
