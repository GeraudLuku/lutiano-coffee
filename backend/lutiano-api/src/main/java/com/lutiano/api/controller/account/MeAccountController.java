package com.lutiano.api.controller.account;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.service.account.AccountService;
import com.lutiano.api.models.request.common.CodeRequest;
import com.lutiano.api.models.request.common.PhoneRequest;
import com.lutiano.api.models.request.account.ProfileRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.account.ActivityItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

/** The signed-in person's own account: profile, phone, photo, sign-in history and sessions. */
@RestController
@RequestMapping("/me")
public class MeAccountController {
  private final AccountService service;
  private final CurrentUser current;

  public MeAccountController(AccountService service, CurrentUser current) { this.service = service; this.current = current; }


  /** Changes the name and/or language. Phone has its own confirmation, below. */
  @PutMapping("/profile")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@RequestBody ProfileRequest body, Authentication a) {
    return Responses.ok(service.update(current.get(a), body.fullName(), body.language()));
  }

  /** Sends a code to the new number. */
  @PostMapping("/phone")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> startPhone(@RequestBody PhoneRequest body, Authentication a) {
    return Responses.ok(service.startPhoneChange(current.get(a), body.phone()));
  }

  /** Confirms the code and switches to the new number. */
  @PostMapping("/phone/confirm")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> confirmPhone(@RequestBody CodeRequest body, Authentication a) {
    return Responses.ok(service.confirmPhoneChange(current.get(a), body.code()));
  }

  @PostMapping("/photo")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> photo(@RequestParam("file") MultipartFile file, Authentication a) throws Exception {
    return Responses.ok(service.uploadPhoto(current.get(a), file));
  }

  /** Recent sign-ins and account-security changes, newest first. */
  @GetMapping("/activity")
  public ResponseEntity<GenericApiResponse<PageResponse<ActivityItemResponse>>> activity(PageRequest paging, Authentication a) { return Responses.page(service.activity(current.get(a), paging), ActivityItemResponse.class); }

  /** Signs out every other device and returns a fresh token for this one. */
  @PostMapping("/sessions/revoke")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> revoke(Authentication a) {
    var me = current.get(a);
    return Responses.ok(service.signOutEverywhere(me, me.role()));
  }
}
