package com.lutiano.api.service.account;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.service.auth.AuthService;
import com.lutiano.api.service.auth.TokenService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.proxy.ImageStorage;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.staff.StaffRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.repository.account.AccountRepository;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.StaffActivity;

/**
 * What a signed-in person can change about themselves: name, language, photo and phone (the new phone must be confirmed with a code
 * sent to it), plus their sign-in history and signing out of every other device.
 */
@Service
public class AccountService {
  private static final String PHONE_PURPOSE = "phone-change";
  private static final List<String> ACTIVITY = Enums.list(StaffActivity.class);

  private final AccountRepository account;
  private final StaffRepository staff;
  private final AuthService auth;
  private final TokenService tokens;
  private final OtpService otp;
  private final ImageStorage images;
  private final AuditService audit;

  public AccountService(AccountRepository account, StaffRepository staff, AuthService auth, TokenService tokens, OtpService otp, ImageStorage images, AuditService audit) {
    this.account = account; this.staff = staff; this.auth = auth; this.tokens = tokens; this.otp = otp; this.images = images; this.audit = audit;
  }

  /** Changes the name and/or the language (fr or en); what is left out stays as it is. */
  @Transactional
  public Map<String, Object> update(Actor me, String fullName, String language) {
    String name = fullName == null ? null : Validate.text(fullName, "Name", 150);
    String lang = Validate.language(language, null);
    if (name == null && lang == null) throw Validate.bad("Say what to change.");
    account.updateProfile(me.id(), name, lang);
    audit.log(me, "PROFILE_UPDATED", "staff_user", me.id(), (name != null ? "name" : "") + (name != null && lang != null ? ", " : "") + (lang != null ? "language " + lang : ""));
    return auth.profile(me.id());
  }

  /** Sends a code to the new number; the number only changes once the code is confirmed. */
  public Map<String, Object> startPhoneChange(Actor me, String rawPhone) {
    String phone = Phones.normalize(rawPhone);
    if (phone == null) throw Validate.bad("Enter a valid mobile number, for example +237 6 90 00 00 00.");
    Map<String, Object> user = staff.findById(me.id()).orElseThrow(() -> Validate.notFound("Account not found."));
    if (phone.equals(Phones.normalize((String) user.get("phone")))) throw Validate.bad("That is already your number.");
    if (account.phoneTakenByOther(phone, me.id())) throw Validate.conflict("Another account already uses this number.");
    account.savePendingPhone(me.id(), phone);
    otp.start(PHONE_PURPOSE, me.id(), NotificationCatalog.OTP_PHONE_CHANGE, me.id(), me.fullName(), phone, (String) user.get("language"), false);
    return Map.of("status", "sent", "resendAfterSeconds", otp.cooldownSeconds());
  }

  @Transactional
  public Map<String, Object> confirmPhoneChange(Actor me, String code) {
    String phone = account.pendingPhone(me.id()).orElseThrow(() -> Validate.bad("No phone change is waiting. Start again."));
    otp.verify(PHONE_PURPOSE, me.id(), code);
    if (account.phoneTakenByOther(phone, me.id())) throw Validate.conflict("Another account already uses this number.");
    account.setPhone(me.id(), phone);
    account.clearPendingPhone(me.id());
    audit.log(me, "PHONE_CHANGED", "staff_user", me.id());
    return auth.profile(me.id());
  }

  public Map<String, Object> uploadPhoto(Actor me, MultipartFile file) throws Exception {
    String url = images.save(ImageStorage.STAFF, me.id(), file);
    staff.setImageUrl(me.id(), url);
    audit.log(me, "PROFILE_UPDATED", "staff_user", me.id(), "photo");
    return Map.of("imageUrl", url);
  }

  public PageResponse<Map<String, Object>> activity(Actor me, PageRequest req) { return account.activity(me.id(), ACTIVITY, req); }

  /** Signs out every other device: older tokens stop working, and this device gets a fresh one. */
  @Transactional
  public Map<String, Object> signOutEverywhere(Actor me, String role) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);   // token times are whole seconds: the new token is not older than this
    account.revokeTokensBefore(me.id(), Timestamp.from(now));
    TokenService.IssuedToken token = tokens.issue(me.id(), role);
    audit.log(me, "SESSIONS_REVOKED", "staff_user", me.id(), "signed out of every other device");
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("token", token.value());
    out.put("expiresAt", token.expiresAt().toString());
    return out;
  }
}
