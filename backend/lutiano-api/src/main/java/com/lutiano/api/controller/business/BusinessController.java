package com.lutiano.api.controller.business;

import static com.lutiano.api.security.Roles.ADMIN;
import static com.lutiano.api.security.Roles.STAFF;

import com.lutiano.api.security.CurrentUser;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.lutiano.api.service.business.BusinessService;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.models.request.business.BusinessRequest;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.business.BusinessResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;
import com.lutiano.api.models.response.business.SecurityResponse;

/** The business details in Settings: readable by staff, editable by administrators. */
@RestController
@RequestMapping("/settings/business")
public class BusinessController {
  private final BusinessService service;
  private final CurrentUser current;
  private final boolean mfaEnabled;
  private final int codeLength, ttlSeconds, maxAttempts, sessionHours;

  public BusinessController(BusinessService service, CurrentUser current, AppProperties props) {
    this.service = service; this.current = current; this.mfaEnabled = props.mfa().enabled(); this.codeLength = props.otp().length();
    this.ttlSeconds = props.otp().ttlSeconds(); this.maxAttempts = props.otp().maxAttempts(); this.sessionHours = props.jwt().ttlHours();
  }

  @GetMapping
  public ResponseEntity<GenericApiResponse<BusinessResponse>> get(Authentication a) {
    current.require(a, STAFF);
    return Responses.ok(service.get(), BusinessResponse.class);
  }

  /** The sign-in security rules actually in force, read from the configuration, for the Security tab. */
  @GetMapping("/security")
  public ResponseEntity<GenericApiResponse<SecurityResponse>> security(Authentication a) {
    current.require(a, ADMIN);
    return Responses.ok(Map.of("mfaEnabled", mfaEnabled, "codeLength", codeLength, "codeMinutes", ttlSeconds / 60, "maxAttempts", maxAttempts, "sessionHours", sessionHours), SecurityResponse.class);
  }

  @PutMapping
  public ResponseEntity<GenericApiResponse<Map<String, Object>>> update(@RequestBody BusinessRequest in, Authentication a) {
    return Responses.ok(service.update(current.require(a, ADMIN), in));
  }
}
