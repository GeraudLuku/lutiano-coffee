package com.lutiano.api.controller.auth;

import java.util.Map;
import com.lutiano.api.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.lutiano.api.service.auth.AuthService;
import com.lutiano.api.models.request.auth.ForgotPasswordRequest;
import com.lutiano.api.models.request.auth.LoginRequest;
import com.lutiano.api.models.request.auth.MfaChallengeRequest;
import com.lutiano.api.models.request.auth.MfaCodeRequest;
import com.lutiano.api.models.request.common.PasswordChangeRequest;
import com.lutiano.api.models.request.auth.ResetPasswordRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.models.response.auth.MeResponse;
import com.lutiano.api.utils.Responses;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService service;
  private final CurrentUser current;

  public AuthController(AuthService service, CurrentUser current) { this.service = service; this.current = current; }


  @PostMapping("/login")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> login(@RequestBody LoginRequest body, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
    return Responses.ok(service.login(body.email(), body.password(), describeClient(userAgent)));
  }

  @PostMapping("/mfa/verify")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> verifyMfa(@RequestBody MfaCodeRequest body, @RequestHeader(value = "User-Agent", required = false) String userAgent) {
    return Responses.ok(service.verifyMfa(body.challenge(), body.code(), describeClient(userAgent)));
  }

  /** Always answers the same, whether or not the account exists. */
  @PostMapping("/password/forgot")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> forgotPassword(@RequestBody ForgotPasswordRequest body) {
    service.forgotPassword(body.email());
    return Responses.ok(Map.of("status", "sent", "resendAfterSeconds", 60));
  }

  @PostMapping("/password/reset")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resetPassword(@RequestBody ResetPasswordRequest body) {
    service.resetPassword(body.email(), body.code(), body.newPassword());
    return Responses.ok(Map.of("status", "reset"));
  }

  @PostMapping("/mfa/resend")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> resendMfa(@RequestBody MfaChallengeRequest body) { return Responses.ok(service.resendMfa(body.challenge())); }

  /** "Seller app", "Chrome on macOS"… — enough to recognise a device in the activity log, without storing the raw header. */
  static String describeClient(String ua) { return describeClient(ua, "Seller app"); }

  /** {@code appName} is what a Flutter (Dart) client is called: "Seller app" or "Customer app". */
  public static String describeClient(String ua, String appName) {
    if (ua == null || ua.isBlank()) return "Unknown device";
    String u = ua.toLowerCase();
    if (u.contains("dart")) return appName;
    String browser = u.contains("edg/") ? "Edge" : u.contains("chrome/") ? "Chrome" : u.contains("firefox/") ? "Firefox" : u.contains("safari/") ? "Safari" : u.contains("curl") ? "Command line" : "Browser";
    String os = u.contains("android") ? "Android" : u.contains("iphone") || u.contains("ipad") ? "iOS" : u.contains("mac os") ? "macOS" : u.contains("windows") ? "Windows" : u.contains("linux") ? "Linux" : null;
    return os == null ? browser : browser + " on " + os;
  }

  @GetMapping("/me")
  public ResponseEntity<GenericApiResponse<MeResponse>> me(Authentication a) {
    return Responses.ok(service.profile(current.get(a).id()), MeResponse.class);
  }

  @PostMapping("/password")
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> changePassword(@RequestBody PasswordChangeRequest body, Authentication a) {
    service.changePassword(current.get(a), body.currentPassword(), body.newPassword());
    return Responses.ok(Map.of("status", "changed"));
  }
}
