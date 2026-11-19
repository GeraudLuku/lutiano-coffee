package com.lutiano.api.service.customer;

import com.lutiano.api.security.Roles;
import com.lutiano.api.service.otp.OtpService;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Phones;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.customer.CustomerAccountRepository;
import com.lutiano.api.proxy.SocialVerifier;
import com.lutiano.api.models.request.customer.SocialPhoneRequest;

/**
 * Sign in with Google or Apple. A known account signs in at once (with the second step if it is on). A new one is asked for a
 * phone number first: orders, loyalty and in-store identification are tied to the phone, so it is confirmed by SMS before the account
 * is created, or linked to the customer record that already has that number.
 */
@Service
public class CustomerSocialService {
  private static final String PURPOSE = "customer-social";
  private static final Duration TICKET_TTL = Duration.ofMinutes(15);


  private final SocialVerifier verifier;
  private final CustomerAccountRepository customers;
  private final CustomerAuthService auth;
  private final CustomerAuthCodes codes;
  private final StringRedisTemplate redis;
  private final AuditService audit;
  private final References references;
  private final OtpService otp;

  public CustomerSocialService(SocialVerifier verifier, CustomerAccountRepository customers, CustomerAuthService auth, CustomerAuthCodes codes,
                               StringRedisTemplate redis, AuditService audit, References references, OtpService otp) {
    this.verifier = verifier; this.customers = customers; this.auth = auth; this.codes = codes; this.redis = redis; this.audit = audit;
    this.references = references; this.otp = otp;
  }

  public Map<String, Object> providers() { return Map.of("google", verifier.enabled(SocialVerifier.GOOGLE), "apple", verifier.enabled(SocialVerifier.APPLE)); }

  /** A known identity signs in; a new one gets a ticket for the phone step. */
  public Map<String, Object> signIn(String provider, String idToken, String nonce, String client) {
    SocialVerifier.Identity id = verifier.verify(provider, idToken, nonce);
    String customerId = customers.customerOfIdentity(id.provider(), id.subject()).orElse(null);
    Map<String, Object> c = customerId == null ? null : customers.byId(customerId).orElse(null);
    if (c != null) {
      if (!"ACTIVE".equals(c.get("status"))) throw Validate.conflict("This account is suspended. Contact the LUTIANO team.");
      return auth.afterPassword(c, client);
    }
    String ticket = CustomerAuthService.randomToken();
    Map<String, String> data = new LinkedHashMap<>();
    data.put("provider", id.provider());
    data.put("subject", id.subject());
    data.put("email", id.email() == null ? "" : id.email());
    data.put("name", id.name() == null ? "" : id.name());
    redis.opsForHash().putAll("csocial:" + ticket, data);
    redis.expire("csocial:" + ticket, TICKET_TTL);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("needsPhone", true);
    out.put("ticket", ticket);
    out.put("name", id.name());
    out.put("email", id.email());
    return out;
  }

  /** The phone for a new social account: a code is sent to it. */
  public Map<String, Object> phoneStep(SocialPhoneRequest in) {
    Map<Object, Object> t = ticket(in.ticket());
    String phone = Phones.require(in.phone(), "Phone");
    if (!Boolean.TRUE.equals(in.acceptTerms())) throw Validate.bad("Accept the Terms of Service and the Privacy Policy to create an account.");
    String name = in.fullName() != null && !in.fullName().isBlank() ? Validate.text(in.fullName(), "Full name", 150) : (String) t.get("name");
    if (name == null || name.isBlank()) throw Validate.bad("Enter your full name.");
    Map<String, Object> existing = customers.byPhone(phone).orElse(null);
    if (existing != null && !"ACTIVE".equals(existing.get("status"))) throw Validate.conflict("This account is suspended. Contact the LUTIANO team.");
    String language = Validate.language(in.language(), "fr");
    String key = "csocial:" + in.ticket();
    redis.opsForHash().putAll(key, Map.of("phone", phone, "fullName", name, "language", language));
    redis.expire(key, TICKET_TTL);
    codes.send(PURPOSE, in.ticket(), null, name, phone, language);
    return Map.of("status", "sent", "resendAfterSeconds", otp.cooldownSeconds());
  }

  /** The code proves the phone: the account is created (or the existing record claimed or linked) and the person signs in. */
  @Transactional
  public Map<String, Object> verify(String ticket, String code, String client) {
    Map<Object, Object> t = ticket(ticket);
    String phone = (String) t.get("phone");
    if (phone == null) throw Validate.bad("Enter your phone number first.");
    otp.verify(PURPOSE, ticket, code);
    String provider = (String) t.get("provider"), subject = (String) t.get("subject"), name = (String) t.get("fullName"), language = (String) t.get("language");
    String email = CustomerAuthService.emptyToNull((String) t.get("email"));
    if (email != null && customers.emailTaken(email, null)) email = null;   // the address belongs to another account: not used here
    Map<String, Object> existing = customers.byPhone(phone).orElse(null);
    String id;
    if (existing != null && existing.get("registered_at") != null) {
      id = (String) existing.get("id");   // the same person already has an account with this number: the provider is linked to it
    } else if (existing != null) {
      id = (String) existing.get("id");
      customers.claim(id, name, email, null, language);
    } else {
      id = UUID.randomUUID().toString();
      customers.insertRegistered(id, references.plain("CU", "customer_code_seq"), name, phone, email, null, language);
    }
    if (customers.customerOfIdentity(provider, subject).isEmpty()) customers.addIdentity(provider, subject, id, email);
    redis.delete("csocial:" + ticket);
    audit.log(new Actor(id, phone, name, Roles.CUSTOMER), "CUSTOMER_REGISTERED", "customer", id, provider.toLowerCase() + " sign-in");
    return auth.afterPassword(customers.byId(id).orElseThrow(), client);
  }

  /** Links a Google or Apple account to the signed-in customer. */
  public Map<String, Object> link(Actor me, String provider, String idToken, String nonce) {
    SocialVerifier.Identity id = verifier.verify(provider, idToken, nonce);
    String owner = customers.customerOfIdentity(id.provider(), id.subject()).orElse(null);
    if (owner != null && !owner.equals(me.id())) throw Validate.conflict("This account is already linked to another customer.");
    if (owner == null) customers.addIdentity(id.provider(), id.subject(), me.id(), id.email());
    audit.log(me, "SOCIAL_LINKED", "customer", me.id(), id.provider().toLowerCase());
    return Map.of("linked_accounts", customers.identities(me.id()));
  }

  /** Unlinks one. The phone code always works as a way in, so an account is never left without one. */
  public Map<String, Object> unlink(Actor me, String provider) {
    String p = Validate.oneOf(provider == null ? "" : provider.toUpperCase(), "Provider", Set.of(SocialVerifier.GOOGLE, SocialVerifier.APPLE));
    customers.removeIdentity(me.id(), p);
    audit.log(me, "SOCIAL_UNLINKED", "customer", me.id(), p.toLowerCase());
    return Map.of("linked_accounts", customers.identities(me.id()));
  }

  private Map<Object, Object> ticket(String ticket) {
    Map<Object, Object> t = ticket == null || ticket.isBlank() ? Map.of() : redis.opsForHash().entries("csocial:" + ticket);
    if (t.isEmpty()) throw Validate.unauthorized("This sign-in expired. Start again.");
    return t;
  }
}
