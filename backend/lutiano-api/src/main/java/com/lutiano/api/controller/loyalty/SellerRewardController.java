package com.lutiano.api.controller.loyalty;

import static com.lutiano.api.security.Roles.SELLER;

import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.loyalty.WalletService;
import com.lutiano.api.models.request.loyalty.UseRewardRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** Giving a customer's reward at a sales point: scan its QR, check, confirm. */
@RestController
@RequestMapping("/seller/rewards")
public class SellerRewardController {
  private final WalletService wallet;
  private final CurrentUser current;

  public SellerRewardController(WalletService wallet, CurrentUser current) { this.wallet = wallet; this.current = current; }


  /** What the scanned code is (reward, customer's name, whether it can still be used), without using it. */
  @GetMapping("/lookup")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> lookup(@RequestParam String code, Authentication a) { return Responses.ok(wallet.lookup(current.require(a, SELLER), code)); }

  /** Gives the reward at this seller's sales point; works once. */
  @PostMapping("/use")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> use(@RequestBody UseRewardRequest body, Authentication a) { return Responses.ok(wallet.use(current.require(a, SELLER), body.code(), body.operationId())); }
}
