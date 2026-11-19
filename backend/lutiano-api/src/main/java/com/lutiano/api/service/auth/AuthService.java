package com.lutiano.api.service.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.staff.StaffRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.security.Roles;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final AuthenticationManager authManager;
  private final TokenService tokens;
  private final StaffRepository staff;
  private final PasswordEncoder encoder;
  private final AuditService audit;
  private final MfaService mfa;
  private final OtpService otp;

  public AuthService(AuthenticationManager authManager, TokenService tokens, StaffRepository staff, PasswordEncoder encoder, AuditService audit, MfaService mfa, OtpService otp) {
    this.otp = otp;
    this.authManager = authManager; this.tokens = tokens; this.staff = staff; this.encoder = encoder; this.audit = audit; this.mfa = mfa;
  }

  /**
   * Checks the password. Administrators then get a second step (a code by SMS) instead of a session; everyone else gets the session.
   * @param client a short description of the device, e.g. "Seller app" or "Chrome on macOS", for the activity log.
   */
  public Map<String, Object> login(String rawEmail, String password, String client) {
    if (rawEmail == null || rawEmail.isBlank() || password == null || password.isEmpty()) throw Validate.bad("Enter your email and password.");
    String email = rawEmail.trim().toLowerCase();
    try {
      authManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
    } catch (AuthenticationException e) {
      // Same message for unknown, wrong-password and deactivated accounts.
      throw Validate.unauthorized("Incorrect email or password.");
    }
    Map<String, Object> user = staff.findByEmail(email).orElseThrow(() -> Validate.unauthorized("Incorrect email or password."));
    if (mfa.required(user)) return mfa.challenge(user);
    return session(user, email, client);
  }

  /** Completes the second step with the code from the SMS. */
  public Map<String, Object> verifyMfa(String challenge, String code, String client) {
    String id = mfa.verify(challenge, code);
    Map<String, Object> user = staff.findById(id).orElseThrow(() -> Validate.unauthorized("Your sign-in expired. Sign in again."));
    return session(user, (String) user.get("email"), client);
  }

  public Map<String, Object> resendMfa(String challenge) { return mfa.resend(challenge); }

  private Map<String, Object> session(Map<String, Object> user, String email, String client) {
    String id = (String) user.get("id");
    TokenService.IssuedToken token = tokens.issue(id, (String) user.get("role"));
    staff.touchLastLogin(id);
    audit.log(new Actor(id, email, (String) user.get("full_name"), (String) user.get("role")), "SIGNED_IN", "staff_user", id, client);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("token", token.value());
    out.put("expiresAt", token.expiresAt().toString());
    out.put("user", profile(id));
    return out;
  }

  public Map<String, Object> profile(String id) {
    Map<String, Object> user = staff.findById(id).orElseThrow(() -> Validate.notFound("Account not found."));
    user.put("operations", staff.operationsOf(id));
    return user;
  }

  private static final String RESET = "password-reset";

  /**
   * Sellers who forgot their password get a code by SMS on the phone on file. The answer is the same whether or not the account exists,
   * so the form cannot be used to find out who has an account. Administrators reset through another administrator.
   */
  public void forgotPassword(String rawEmail) {
    if (rawEmail == null || rawEmail.isBlank()) throw Validate.bad("Enter your email.");
    staff.findByEmail(rawEmail.trim().toLowerCase())
        .filter(u -> Roles.SELLER.equals(u.get("role")) && Rows.truthy(u.get("active")) && u.get("phone") != null)
        .ifPresent(u -> {
          try {
            otp.start(RESET, (String) u.get("id"), NotificationCatalog.OTP_PASSWORD_RESET, (String) u.get("id"), (String) u.get("full_name"),
                (String) u.get("phone"), (String) u.get("language"), false);
          } catch (ResponseStatusException e) {
            // too many requests or an SMS failure: not shown, so the answer stays the same for every account
          }
        });
  }

  /** Sets a new password after the SMS code, and signs the account out everywhere. */
  public void resetPassword(String rawEmail, String code, String newPassword) {
    Map<String, Object> user = rawEmail == null ? null : staff.findByEmail(rawEmail.trim().toLowerCase()).filter(u -> Roles.SELLER.equals(u.get("role")) && Rows.truthy(u.get("active"))).orElse(null);
    if (user == null) throw Validate.bad("This code has expired, or none was requested. Ask for a new one.");   // same words as a missing code, so an unknown account looks the same
    String id = (String) user.get("id");
    Validate.password(newPassword);
    otp.verify(RESET, id, code);
    staff.setPasswordHash(id, encoder.encode(newPassword));
    staff.revokeSessions(id);
    audit.log(new Actor(id, (String) user.get("email"), (String) user.get("full_name"), (String) user.get("role")), "PASSWORD_RESET", "staff_user", id);
  }

  public void changePassword(Actor actor, String currentPassword, String newPassword) {
    if (currentPassword == null || !encoder.matches(currentPassword, staff.passwordHash(actor.id())))
      throw Validate.bad("Current password is incorrect.");
    Validate.password(newPassword);
    if (newPassword.equals(currentPassword)) throw Validate.bad("Choose a password different from the current one.");
    staff.setPasswordHash(actor.id(), encoder.encode(newPassword));
    audit.log(actor, "PASSWORD_CHANGED", "staff_user", actor.id());
  }
}
