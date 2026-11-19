package com.lutiano.api.service.customer;

import com.lutiano.api.repository.account.AccountRepository;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.service.auth.TokenService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.loyalty.PointsRepository;
import com.lutiano.api.proxy.ImageStorage;
import com.lutiano.api.service.operation.QrCodeService;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.security.Actor;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.lutiano.api.repository.customer.CustomerAccountRepository;
import com.lutiano.api.models.request.customer.CustomerProfileRequest;
import com.lutiano.api.definitions.enums.CustomerActivity;
import com.lutiano.api.utils.Enums;

/** A customer's own account: profile, photo, phone, password, second step, sessions and deletion. */
@Service
public class CustomerProfileService {
  private static final String PHONE_CHANGE = "customer-phone", DELETE = "customer-delete";
  private static final List<String> ACTIVITY = Enums.list(CustomerActivity.class);


  private final CustomerAccountRepository customers;
  private final PointsRepository points;
  private final AccountRepository account;
  private final OtpService otp;
  private final TokenService tokens;
  private final PasswordEncoder encoder;
  private final StringRedisTemplate redis;
  private final ImageStorage images;
  private final AuditService audit;
  private final CustomerAuthCodes codes;

  public CustomerProfileService(CustomerAccountRepository customers, PointsRepository points, AccountRepository account, OtpService otp, TokenService tokens,
                                PasswordEncoder encoder, StringRedisTemplate redis, ImageStorage images, AuditService audit, CustomerAuthCodes codes) {
    this.customers = customers; this.points = points; this.account = account; this.otp = otp; this.tokens = tokens; this.encoder = encoder;
    this.redis = redis; this.images = images; this.audit = audit; this.codes = codes;
  }

  /** What the app shows about the signed-in customer. The password hash never leaves the server. */
  public Map<String, Object> profile(String id) {
    Map<String, Object> c = customers.byId(id).orElseThrow(() -> Validate.notFound("Account not found."));
    Map<String, Object> out = new LinkedHashMap<>();
    for (String k : List.of("id", "code", "full_name", "phone", "email", "language", "birth_date", "image_url", "two_factor_enabled", "marketing_consent", "tier_code", "created_at"))
      out.put(k, c.get(k));
    out.put("has_password", c.get("password_hash") != null);
    out.put("loyalty_qr", QrCodeService.CUSTOMER_PREFIX + c.get("loyalty_token"));
    out.put("linked_accounts", customers.identities(id));
    return out;
  }

  @Transactional
  public Map<String, Object> update(Actor me, CustomerProfileRequest in) {
    String name = Validate.text(in.fullName(), "Full name", 150);
    String email = in.email() == null || in.email().isBlank() ? null : Validate.email(in.email());
    if (email != null && customers.emailTaken(email, me.id())) throw Validate.conflict("This email is already used by another account.");
    String language = Validate.language(in.language(), "fr");
    if (in.birthDate() != null && !in.birthDate().isBefore(LocalDate.now())) throw Validate.bad("Enter a valid date of birth.");
    customers.updateProfile(me.id(), name, email, language, in.birthDate() == null ? null : Date.valueOf(in.birthDate()), Boolean.TRUE.equals(in.marketingConsent()));
    audit.log(me, "PROFILE_UPDATED", "customer", me.id());
    return profile(me.id());
  }

  public Map<String, Object> uploadPhoto(Actor me, MultipartFile file) throws Exception {
    String url = images.save(ImageStorage.CUSTOMERS, me.id(), file);
    customers.setImage(me.id(), url);
    audit.log(me, "PROFILE_UPDATED", "customer", me.id(), "photo");
    return Map.of("imageUrl", url);
  }

  // ----- phone -----

  /** Sends a code to the new number; the number changes only when the code is entered. */
  public Map<String, Object> startPhoneChange(Actor me, String rawPhone) {
    String phone = Phones.require(rawPhone, "Phone");
    if (points.phoneTaken(phone, me.id())) throw Validate.conflict("This number belongs to another account.");
    redis.opsForValue().set("cphone:" + me.id(), phone, Duration.ofMinutes(30));
    codes.send(PHONE_CHANGE, me.id(), me.id(), me.fullName(), phone, language(me.id()));
    return Map.of("status", "sent", "resendAfterSeconds", otp.cooldownSeconds());
  }

  @Transactional
  public Map<String, Object> confirmPhoneChange(Actor me, String code) {
    String phone = redis.opsForValue().get("cphone:" + me.id());
    if (phone == null) throw Validate.bad("This code has expired, or none was requested. Ask for a new one.");
    otp.verify(PHONE_CHANGE, me.id(), code);
    if (points.phoneTaken(phone, me.id())) throw Validate.conflict("This number belongs to another account.");
    customers.setPhone(me.id(), phone);
    redis.delete("cphone:" + me.id());
    audit.log(me, "PHONE_CHANGED", "customer", me.id());
    return profile(me.id());
  }

  // ----- password, second step, sessions -----

  /** Changes the password; a customer who signed up without one just sets it. */
  public void changePassword(Actor me, String currentPassword, String newPassword) {
    String hash = (String) customers.byId(me.id()).orElseThrow().get("password_hash");
    if (hash != null && (currentPassword == null || !encoder.matches(currentPassword, hash))) throw Validate.bad("Current password is incorrect.");
    Validate.password(newPassword);
    if (hash != null && newPassword.equals(currentPassword)) throw Validate.bad("Choose a password different from the current one.");
    customers.setPassword(me.id(), encoder.encode(newPassword));
    audit.log(me, "PASSWORD_CHANGED", "customer", me.id());
  }

  /** The second step after the password needs a password to follow. */
  public Map<String, Object> setTwoFactor(Actor me, boolean on) {
    if (on && customers.byId(me.id()).orElseThrow().get("password_hash") == null) throw Validate.conflict("Set a password first: the second step is asked after it.");
    customers.setTwoFactor(me.id(), on);
    audit.log(me, "TWO_FACTOR_CHANGED", "customer", me.id(), on ? "on" : "off");
    return profile(me.id());
  }

  public PageResponse<Map<String, Object>> activity(Actor me, PageRequest req) { return account.activity(me.id(), ACTIVITY, req); }

  /** Signs out every other device: older tokens stop working and this device gets a fresh one. */
  public Map<String, Object> signOutEverywhere(Actor me) {
    customers.revokeSessions(me.id(), wholeSecondNow());
    TokenService.IssuedToken token = tokens.issueCustomer(me.id());
    audit.log(me, "SESSIONS_REVOKED", "customer", me.id(), "signed out of every other device");
    return Map.of("token", token.value(), "expiresAt", token.expiresAt().toString());
  }

  // ----- favourites -----

  public PageResponse<Map<String, Object>> favourites(Actor me, PageRequest req) { return customers.favourites(me.id(), req); }

  public Map<String, Object> addFavourite(Actor me, String productId) {
    if (!customers.favouritable(Validate.id(productId, "Product"))) throw Validate.notFound("Product not found.");
    customers.addFavourite(me.id(), productId);
    return Map.of("favourite", true);
  }

  public Map<String, Object> removeFavourite(Actor me, String productId) {
    customers.removeFavourite(me.id(), Validate.id(productId, "Product"));
    return Map.of("favourite", false);
  }

  // ----- deletion -----

  public Map<String, Object> startDeletion(Actor me) {
    String phone = (String) customers.byId(me.id()).orElseThrow().get("phone");
    codes.send(DELETE, me.id(), me.id(), me.fullName(), phone, language(me.id()));
    return Map.of("status", "sent", "resendAfterSeconds", otp.cooldownSeconds());
  }

  /** Closes the account after the password (or a code sent by SMS for an account without one). Sales and loyalty history stay for the accounts. */
  @Transactional
  public void delete(Actor me, String password, String code) {
    Map<String, Object> c = customers.byId(me.id()).orElseThrow();
    String hash = (String) c.get("password_hash");
    if (hash != null) { if (password == null || !encoder.matches(password, hash)) throw Validate.bad("Password is incorrect."); }
    else otp.verify(DELETE, me.id(), code);
    audit.log(me, "ACCOUNT_DELETED", "customer", me.id());
    customers.anonymise(me.id());
  }

  Timestamp wholeSecondNow() { return Timestamp.from(Instant.now().truncatedTo(ChronoUnit.SECONDS)); }

  private String language(String id) { return (String) customers.byId(id).orElseThrow().get("language"); }
}
