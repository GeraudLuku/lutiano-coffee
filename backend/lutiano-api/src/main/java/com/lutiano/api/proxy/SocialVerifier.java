package com.lutiano.api.proxy;

import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import com.lutiano.api.utils.Validate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import com.lutiano.api.config.AppProperties;

/**
 * Checks the ID token the Google or Apple sign-in gave the app: its signature against the provider's published keys, its issuer, that it
 * was issued for one of our apps (audience), that it has not expired and, for Apple, the nonce the app generated. A provider with no
 * configured audience is off.
 */
@Component
public class SocialVerifier {
  public static final String GOOGLE = "GOOGLE", APPLE = "APPLE";

  /** Who the provider says the person is. */
  public record Identity(String provider, String subject, String email, String name) {}

  private final Map<String, JwtDecoder> decoders;
  private final Map<String, Set<String>> audiences;

  @Autowired
  public SocialVerifier(AppProperties.Social social) {
    this(Map.of(GOOGLE, NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs").build(),
                APPLE, NimbusJwtDecoder.withJwkSetUri("https://appleid.apple.com/auth/keys").build()),
         Map.of(GOOGLE, split(social.google().clientIds()), APPLE, split(social.apple().audiences())));
  }

  /** For tests: the decoders (with their own keys) and the allowed audiences per provider. */
  SocialVerifier(Map<String, JwtDecoder> decoders, Map<String, Set<String>> audiences) { this.decoders = decoders; this.audiences = audiences; }

  public boolean enabled(String provider) { return !audiences.getOrDefault(provider, Set.of()).isEmpty(); }

  public Identity verify(String rawProvider, String idToken, String nonce) {
    String provider = Validate.oneOf(rawProvider == null ? "" : rawProvider.toUpperCase(), "Provider", Set.of(GOOGLE, APPLE));
    if (!enabled(provider)) throw Validate.conflict(("GOOGLE".equals(provider) ? "Google" : "Apple") + " sign-in is not available.");
    if (idToken == null || idToken.isBlank()) throw Validate.bad("The sign-in did not return a token.");
    Jwt jwt;
    try { jwt = decoders.get(provider).decode(idToken); }
    catch (JwtException e) { throw Validate.unauthorized("This sign-in could not be verified. Try again."); }
    String issuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
    boolean issuerOk = GOOGLE.equals(provider) ? issuer.equals("https://accounts.google.com") || issuer.equals("accounts.google.com") : issuer.equals("https://appleid.apple.com");
    List<String> aud = jwt.getAudience() == null ? List.of() : jwt.getAudience();
    if (!issuerOk || aud.stream().noneMatch(audiences.get(provider)::contains)) throw Validate.unauthorized("This sign-in could not be verified. Try again.");
    if (APPLE.equals(provider) && nonce != null && !nonce.isBlank()) {
      String claim = jwt.getClaimAsString("nonce");
      if (claim == null || !(claim.equals(nonce) || claim.equalsIgnoreCase(sha256(nonce)))) throw Validate.unauthorized("This sign-in could not be verified. Try again.");
    }
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) throw Validate.unauthorized("This sign-in could not be verified. Try again.");
    // an address the provider has not verified is never trusted
    Object flag = jwt.getClaim("email_verified");
    boolean verified = Boolean.TRUE.equals(flag) || "true".equals(String.valueOf(flag));
    return new Identity(provider, subject, verified ? jwt.getClaimAsString("email") : null, jwt.getClaimAsString("name"));
  }

  private static Set<String> split(String s) { return Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty()).collect(Collectors.toSet()); }

  private static String sha256(String s) {
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
    catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
