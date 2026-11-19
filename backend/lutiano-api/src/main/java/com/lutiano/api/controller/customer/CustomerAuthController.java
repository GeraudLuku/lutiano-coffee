package com.lutiano.api.controller.customer;

import com.lutiano.api.controller.auth.AuthController;
import com.lutiano.api.service.search.SearchRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.customer.ClientAddress;
import com.lutiano.api.service.customer.CustomerAuthService;
import com.lutiano.api.service.customer.CustomerSocialService;
import com.lutiano.api.models.request.customer.CustomerLoginRequest;
import com.lutiano.api.models.request.customer.CustomerResetPasswordRequest;
import com.lutiano.api.models.request.customer.PhoneCodeRequest;
import com.lutiano.api.models.request.common.PhoneRequest;
import com.lutiano.api.models.request.customer.SecondStepRequest;
import com.lutiano.api.models.request.customer.SignUpRequest;
import com.lutiano.api.models.request.common.SocialLoginRequest;
import com.lutiano.api.models.request.customer.SocialPhoneRequest;
import com.lutiano.api.models.request.customer.TicketCodeRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.customer.ProviderResponse;
import com.lutiano.api.utils.Responses;

/** Open endpoints of the Customer app: sign-up, sign-in and forgotten password. Limited per address so they cannot be used to guess or to flood SMS. */
@RestController
@RequestMapping("/customer/auth")
public class CustomerAuthController {
  private static final int PER_MINUTE = 20;
  private final CustomerAuthService service;
  private final CustomerSocialService social;
  private final SearchRateLimiter limiter;

  public CustomerAuthController(CustomerAuthService service, CustomerSocialService social, SearchRateLimiter limiter) { this.service = service; this.social = social; this.limiter = limiter; }


  @PostMapping("/signup")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> signUp(@RequestBody SignUpRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.accepted(service.signUp(body));
  }

  @PostMapping("/signup/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verifySignUp(@RequestBody PhoneCodeRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.verifySignUp(body.phone(), body.code(), client(req)));
  }

  /** Sends a sign-in code; answers the same for every number. */
  @PostMapping("/otp/request")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> requestCode(@RequestBody PhoneRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.requestCode(body.phone()));
  }

  @PostMapping("/otp/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verifyCode(@RequestBody PhoneCodeRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.verifyCode(body.phone(), body.code(), client(req)));
  }

  @PostMapping("/login")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> login(@RequestBody CustomerLoginRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.login(body.phone(), body.password(), client(req)));
  }

  /** The code after the password, when the second step is on. */
  @PostMapping("/login/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verifySecondStep(@RequestBody SecondStepRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.verifySecondStep(body.challenge(), body.code(), client(req)));
  }

  @PostMapping("/password/forgot")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> forgot(@RequestBody PhoneRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(service.forgotPassword(body.phone()));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> reset(@RequestBody CustomerResetPasswordRequest body, HttpServletRequest req) {
    limit(req);
    service.resetPassword(body.phone(), body.code(), body.newPassword());
    return Responses.ok(Map.of("status", "reset"));
  }

  /** Which of Google and Apple sign-in are available, so the app shows only those. */
  @GetMapping("/providers")
  public ResponseEntity<GenericApiResponse<ProviderResponse>> providers() { return Responses.ok(social.providers(), ProviderResponse.class); }

  /** A Google or Apple ID token: a known account signs in; a new one gets a ticket and must add its phone. */
  @PostMapping("/social")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> social(@RequestBody SocialLoginRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(social.signIn(body.provider(), body.idToken(), body.nonce(), client(req)));
  }

  @PostMapping("/social/phone")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> socialPhone(@RequestBody SocialPhoneRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(social.phoneStep(body));
  }

  @PostMapping("/social/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> socialVerify(@RequestBody TicketCodeRequest body, HttpServletRequest req) {
    limit(req);
    return Responses.ok(social.verify(body.ticket(), body.code(), client(req)));
  }

  private void limit(HttpServletRequest req) { limiter.check("customer-auth", ClientAddress.of(req), PER_MINUTE); }

  private static String client(HttpServletRequest req) { return AuthController.describeClient(req.getHeader("User-Agent"), "Customer app"); }
}
