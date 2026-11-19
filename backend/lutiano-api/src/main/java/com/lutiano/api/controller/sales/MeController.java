package com.lutiano.api.controller.sales;

import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.sales.SalesSummaryResponse;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import com.lutiano.api.service.sales.SaleService;
import com.lutiano.api.utils.Responses;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in seller's own figures for the Seller app's home screen. */
@RestController
@RequestMapping("/me")
public class MeController {
  private final SaleService sales;
  private final CurrentUser current;

  public MeController(SaleService sales, CurrentUser current) { this.sales = sales; this.current = current; }

  @GetMapping("/summary")
  public ResponseEntity<GenericApiResponse<SalesSummaryResponse>> summary(Authentication a) {
    return Responses.ok(sales.sellerSummary(current.require(a, Roles.SELLER)), SalesSummaryResponse.class);
  }
}
