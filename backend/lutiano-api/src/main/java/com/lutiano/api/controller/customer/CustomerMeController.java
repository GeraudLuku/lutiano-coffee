package com.lutiano.api.controller.customer;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentCustomer;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.customer.CustomerProfileService;
import com.lutiano.api.service.customer.CustomerSocialService;
import com.lutiano.api.models.request.common.CodeRequest;
import com.lutiano.api.models.request.customer.CustomerProfileRequest;
import com.lutiano.api.models.request.customer.DeleteAccountRequest;
import com.lutiano.api.models.request.common.PasswordChangeRequest;
import com.lutiano.api.models.request.common.PhoneRequest;
import com.lutiano.api.models.request.common.SocialLoginRequest;
import com.lutiano.api.models.request.customer.TwoFactorRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.account.ActivityItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.customer.CustomerMeResponse;
import com.lutiano.api.utils.Responses;

/** The signed-in customer's own account. */
@RestController
@RequestMapping("/customer/me")
public class CustomerMeController {
  private final CustomerProfileService service;
  private final CustomerSocialService social;
  private final CurrentCustomer current;

  public CustomerMeController(CustomerProfileService service, CustomerSocialService social, CurrentCustomer current) { this.service = service; this.social = social; this.current = current; }


  @GetMapping
  public ResponseEntity<GenericApiResponse<CustomerMeResponse>> me(Authentication a) { return Responses.ok(service.profile(current.get(a).id()), CustomerMeResponse.class); }

  @PutMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@RequestBody CustomerProfileRequest body, Authentication a) { return Responses.ok(service.update(current.get(a), body)); }

  @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> photo(@RequestPart("file") MultipartFile file, Authentication a) throws Exception { return Responses.ok(service.uploadPhoto(current.get(a), file)); }

  @PostMapping("/phone")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> startPhoneChange(@RequestBody PhoneRequest body, Authentication a) { return Responses.ok(service.startPhoneChange(current.get(a), body.phone())); }

  @PostMapping("/phone/confirm")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> confirmPhoneChange(@RequestBody CodeRequest body, Authentication a) { return Responses.ok(service.confirmPhoneChange(current.get(a), body.code())); }

  @PostMapping("/password")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> changePassword(@RequestBody PasswordChangeRequest body, Authentication a) {
    service.changePassword(current.get(a), body.currentPassword(), body.newPassword());
    return Responses.ok(Map.of("status", "changed"));
  }

  @PutMapping("/two-factor")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> twoFactor(@RequestBody TwoFactorRequest body, Authentication a) { return Responses.ok(service.setTwoFactor(current.get(a), Boolean.TRUE.equals(body.enabled()))); }

  /** Links a Google or Apple account for faster sign-in. */
  @PostMapping("/social")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> link(@RequestBody SocialLoginRequest body, Authentication a) { return Responses.ok(social.link(current.get(a), body.provider(), body.idToken(), body.nonce())); }

  @DeleteMapping("/social/{provider}")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> unlink(@PathVariable String provider, Authentication a) { return Responses.ok(social.unlink(current.get(a), provider)); }

  @GetMapping("/activity")
  public ResponseEntity<GenericApiResponse<PageResponse<ActivityItemResponse>>> activity(PageRequest paging, Authentication a) { return Responses.page(service.activity(current.get(a), paging), ActivityItemResponse.class); }

  @PostMapping("/sessions/revoke")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> revoke(Authentication a) { return Responses.ok(service.signOutEverywhere(current.get(a))); }

  /** For an account without a password: sends the code that confirms the deletion. */
  @PostMapping("/delete/code")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> deletionCode(Authentication a) { return Responses.ok(service.startDeletion(current.get(a))); }

  @DeleteMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> delete(@RequestBody DeleteAccountRequest body, Authentication a) {
    service.delete(current.get(a), body.password(), body.code());
    return Responses.ok(Map.of("status", "deleted"));
  }
}
