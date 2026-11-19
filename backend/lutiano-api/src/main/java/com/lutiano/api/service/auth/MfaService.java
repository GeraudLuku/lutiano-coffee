package com.lutiano.api.service.auth;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.repository.staff.StaffRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.lutiano.api.config.AppProperties;

/**
 * Two-step sign-in for administrators. After the password is right, a short-lived challenge is created in Redis and a
 * code is sent by SMS to the administrator's phone; the session starts only when the code is entered. An administrator
 * who has never done this is enrolled by the same step: entering the code confirms their phone.
 */
@Service
public class MfaService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Duration CHALLENGE_TTL = Duration.ofMinutes(10);
  private static final String PURPOSE = "signin";

  private final StringRedisTemplate redis;
  private final OtpService otp;
  private final StaffRepository staff;
  private final boolean enabled;

  public MfaService(StringRedisTemplate redis, OtpService otp, StaffRepository staff, AppProperties.Mfa mfa) {
    this.redis = redis; this.otp = otp; this.staff = staff; this.enabled = mfa.enabled();
  }

  public boolean required(Map<String, Object> user) { return enabled && "ADMIN".equals(user.get("role")); }

  /** Starts the second step: sends a code and returns what the sign-in screen needs. */
  public Map<String, Object> challenge(Map<String, Object> user) {
    String id = (String) user.get("id");
    if (Phones.normalize((String) user.get("phone")) == null)
      throw Validate.forbidden("Your account has no valid mobile number, which two-step sign-in needs. Ask another administrator to add it to your profile.");
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    redis.opsForValue().set("mfa:" + token, id, CHALLENGE_TTL);
    sendCode(user, true);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("mfaRequired", true);
    out.put("challenge", token);
    out.put("phone", Phones.mask(Phones.normalize((String) user.get("phone"))));
    out.put("enrolment", user.get("mfa_enrolled_at") == null);
    out.put("resendAfterSeconds", otp.cooldownSeconds());
    return out;
  }

  /** Sends a new code for an open challenge. */
  public Map<String, Object> resend(String challenge) {
    sendCode(staff.findById(staffId(challenge)).orElseThrow(() -> Validate.unauthorized("Your sign-in expired. Sign in again.")), false);
    return Map.of("resendAfterSeconds", otp.cooldownSeconds());
  }

  /** Checks the code; returns the id of the administrator who may now start a session. */
  public String verify(String challenge, String code) {
    String id = staffId(challenge);
    otp.verify(PURPOSE, id, code);
    Map<String, Object> user = staff.findById(id).orElseThrow(() -> Validate.unauthorized("Your sign-in expired. Sign in again."));
    if (user.get("mfa_enrolled_at") == null) staff.markMfaEnrolled(id);
    redis.delete("mfa:" + challenge);
    return id;
  }

  private void sendCode(Map<String, Object> user, boolean reuseRecent) {
    otp.start(PURPOSE, (String) user.get("id"), NotificationCatalog.OTP_SIGN_IN, (String) user.get("id"), (String) user.get("full_name"),
        (String) user.get("phone"), (String) user.get("language"), reuseRecent);
  }

  private String staffId(String challenge) {
    String id = challenge == null || challenge.isBlank() ? null : redis.opsForValue().get("mfa:" + challenge);
    if (id == null) throw Validate.unauthorized("Your sign-in expired. Sign in again.");
    return id;
  }
}
