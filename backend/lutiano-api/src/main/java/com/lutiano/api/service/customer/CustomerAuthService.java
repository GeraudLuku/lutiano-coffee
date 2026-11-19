package com.lutiano.api.service.customer;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.service.auth.TokenService;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.Roles;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.lutiano.api.repository.customer.CustomerAccountRepository;
import com.lutiano.api.models.request.customer.SignUpRequest;

/**
 * Sign-up and sign-in for customers: a phone number confirmed by an SMS code, with an optional password (and optional second step
 * after the password). A customer record an administrator created earlier, with its points, is claimed by the person who confirms
 * its phone number. Whatever is asked about an unknown number is answered like a known one, so the forms cannot be used to find out
 * who has an account (sign-up is the exception: it must say the number is taken).
 */
@Service
public class CustomerAuthService {
  public static final String SIGNUP = "customer-signup", SIGNIN = "customer-signin", RESET = "customer-reset", SECOND_STEP = "customer-2fa";
  private static final Duration PENDING_TTL = Duration.ofMinutes(15), CHALLENGE_TTL = Duration.ofMinutes(10);
  private static final SecureRandom RANDOM = new SecureRandom();


  private final CustomerAccountRepository customers;
  private final OtpService otp;
  private final TokenService tokens;
  private final PasswordEncoder encoder;
  private final StringRedisTemplate redis;
  private final AuditService audit;
  private final References references;
  private final CustomerProfileService profiles;
  private final CustomerAuthCodes codes;

  public CustomerAuthService(CustomerAccountRepository customers, OtpService otp, TokenService tokens, PasswordEncoder encoder, StringRedisTemplate redis,
                             AuditService audit, References references, CustomerProfileService profiles, CustomerAuthCodes codes) {
    this.customers = customers; this.otp = otp; this.tokens = tokens; this.encoder = encoder; this.redis = redis; this.audit = audit;
    this.references = references; this.profiles = profiles; this.codes = codes;
  }

  // ----- sign-up -----

  /** Checks the form and sends a code to the phone; nothing is created until the code is entered. */
  public Map<String, Object> signUp(SignUpRequest in) {
    String phone = Phones.require(in.phone(), "Phone");
    String name = Validate.text(in.fullName(), "Full name", 150);
    if (!Boolean.TRUE.equals(in.acceptTerms())) throw Validate.bad("Accept the Terms of Service and the Privacy Policy to create an account.");
    String language = Validate.language(in.language(), "fr");
    String email = blank(in.email()) ? "" : Validate.email(in.email());
    String hash = "";
    if (!blank(in.password())) { Validate.password(in.password()); hash = encoder.encode(in.password()); }
    Map<String, Object> existing = customers.byPhone(phone).orElse(null);
    if (existing != null && existing.get("registered_at") != null) throw Validate.conflict("An account already exists for this number. Sign in instead.");
    if (existing != null && !"ACTIVE".equals(existing.get("status"))) throw Validate.conflict("This account is suspended. Contact the LUTIANO team.");
    if (!email.isEmpty() && customers.emailTaken(email, existing == null ? null : (String) existing.get("id"))) throw Validate.conflict("This email is already used by another account.");
    String key = "csignup:" + phone;
    redis.opsForHash().putAll(key, Map.of("name", name, "email", email, "hash", hash, "language", language));
    redis.expire(key, PENDING_TTL);
    sendCode(SIGNUP, phone, null, name, phone, language);
    return sent();
  }

  /** The code from the SMS creates (or claims) the account and signs in. */
  @Transactional
  public Map<String, Object> verifySignUp(String rawPhone, String code, String client) {
    String phone = Phones.require(rawPhone, "Phone");
    Map<Object, Object> pending = redis.opsForHash().entries("csignup:" + phone);
    if (pending.isEmpty()) throw Validate.bad("This code has expired, or none was requested. Ask for a new one.");
    otp.verify(SIGNUP, phone, code);
    String name = (String) pending.get("name"), email = emptyToNull((String) pending.get("email")), hash = emptyToNull((String) pending.get("hash")), language = (String) pending.get("language");
    Map<String, Object> existing = customers.byPhone(phone).orElse(null);
    String id;
    if (existing != null) {
      if (existing.get("registered_at") != null) throw Validate.conflict("An account already exists for this number. Sign in instead.");
      id = (String) existing.get("id");
      customers.claim(id, name, email, hash, language);
    } else {
      id = UUID.randomUUID().toString();
      customers.insertRegistered(id, references.plain("CU", "customer_code_seq"), name, phone, email, hash, language);
    }
    redis.delete("csignup:" + phone);
    audit.log(actor(id, phone, name), "CUSTOMER_REGISTERED", "customer", id, existing != null ? "claimed an existing record" : null);
    return session(id, client);
  }

  // ----- sign-in -----

  /** Sends a sign-in code to a registered phone; says the same for an unknown one. */
  public Map<String, Object> requestCode(String rawPhone) {
    String phone = Phones.require(rawPhone, "Phone");
    customers.byPhone(phone).filter(CustomerAuthService::registeredAndActive)
        .ifPresent(c -> quietly(() -> sendCode(SIGNIN, phone, (String) c.get("id"), (String) c.get("full_name"), phone, (String) c.get("language"))));
    return sent();
  }

  public Map<String, Object> verifyCode(String rawPhone, String code, String client) {
    String phone = Phones.require(rawPhone, "Phone");
    Map<String, Object> c = customers.byPhone(phone).filter(CustomerAuthService::registeredAndActive)
        .orElseThrow(() -> Validate.bad("This code has expired, or none was requested. Ask for a new one."));   // same words as a missing code
    otp.verify(SIGNIN, phone, code);
    return session((String) c.get("id"), client);
  }

  /** Phone and password. With the second step on, a code is sent and the session starts only when it is entered. */
  public Map<String, Object> login(String rawPhone, String password, String client) {
    String phone = Phones.normalize(rawPhone);
    Map<String, Object> c = phone == null || blank(password) ? null : customers.byPhone(phone).filter(CustomerAuthService::registeredAndActive).orElse(null);
    if (c == null || c.get("password_hash") == null || !encoder.matches(password, (String) c.get("password_hash")))
      throw Validate.unauthorized("Incorrect phone number or password.");
    return afterPassword(c, client);
  }

  /** The password (or the Google/Apple sign-in) was right: a session, or the second step when it is on. */
  Map<String, Object> afterPassword(Map<String, Object> c, String client) {
    String id = (String) c.get("id");
    if (!Rows.truthy(c.get("two_factor_enabled"))) return session(id, client);
    String challenge = randomToken();
    redis.opsForValue().set("cmfa:" + challenge, id, CHALLENGE_TTL);
    sendCode(SECOND_STEP, id, id, (String) c.get("full_name"), (String) c.get("phone"), (String) c.get("language"));
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("mfaRequired", true);
    out.put("challenge", challenge);
    out.put("phone", Phones.mask((String) c.get("phone")));
    out.put("resendAfterSeconds", otp.cooldownSeconds());
    return out;
  }

  public Map<String, Object> verifySecondStep(String challenge, String code, String client) {
    String id = challenge == null ? null : redis.opsForValue().get("cmfa:" + challenge);
    if (id == null) throw Validate.unauthorized("Your sign-in expired. Sign in again.");
    otp.verify(SECOND_STEP, id, code);
    redis.delete("cmfa:" + challenge);
    return session(id, client);
  }

  // ----- forgotten password -----

  public Map<String, Object> forgotPassword(String rawPhone) {
    String phone = Phones.require(rawPhone, "Phone");
    customers.byPhone(phone).filter(CustomerAuthService::registeredAndActive)
        .ifPresent(c -> quietly(() -> sendCode(RESET, phone, (String) c.get("id"), (String) c.get("full_name"), phone, (String) c.get("language"))));
    return sent();
  }

  /** A new password after the SMS code; every device is signed out. */
  @Transactional
  public void resetPassword(String rawPhone, String code, String newPassword) {
    String phone = Phones.require(rawPhone, "Phone");
    Map<String, Object> c = customers.byPhone(phone).filter(CustomerAuthService::registeredAndActive)
        .orElseThrow(() -> Validate.bad("This code has expired, or none was requested. Ask for a new one."));
    Validate.password(newPassword);
    otp.verify(RESET, phone, code);
    String id = (String) c.get("id");
    customers.setPassword(id, encoder.encode(newPassword));
    customers.revokeSessions(id, profiles.wholeSecondNow());
    audit.log(actor(id, phone, (String) c.get("full_name")), "PASSWORD_RESET", "customer", id);
  }

  // ----- shared -----

  /** Starts a session: a token for the customer and their profile. */
  public Map<String, Object> session(String customerId, String client) {
    TokenService.IssuedToken token = tokens.issueCustomer(customerId);
    customers.touchLogin(customerId);
    Map<String, Object> profile = profiles.profile(customerId);
    audit.log(actor(customerId, (String) profile.get("phone"), (String) profile.get("full_name")), "SIGNED_IN", "customer", customerId, client);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("token", token.value());
    out.put("expiresAt", token.expiresAt().toString());
    out.put("customer", profile);
    return out;
  }

  void sendCode(String purpose, String subject, String recipientId, String name, String phone, String language) {
    codes.send(purpose, subject, recipientId, name, phone, language);
  }

  private static boolean registeredAndActive(Map<String, Object> c) { return c.get("registered_at") != null && "ACTIVE".equals(c.get("status")); }

  private Map<String, Object> sent() { return Map.of("status", "sent", "resendAfterSeconds", otp.cooldownSeconds()); }

  static Actor actor(String id, String phone, String name) { return new Actor(id, phone, name, Roles.CUSTOMER); }

  /** A failure to send is not shown, so an unknown number and a known one look the same. */
  private static void quietly(Runnable r) {
    try { r.run(); } catch (ResponseStatusException e) { /* too many requests or an SMS failure */ }
  }

  private static boolean blank(String s) { return s == null || s.isBlank(); }
  static String emptyToNull(String s) { return s == null || s.isEmpty() ? null : s; }

  /** A random, URL-safe one-time handle (sign-in challenge, Google/Apple ticket). */
  static String randomToken() {
    byte[] b = new byte[24];
    RANDOM.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }
}
