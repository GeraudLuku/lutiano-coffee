package com.lutiano.api.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;

/** Google and Apple ID tokens are accepted only when signed by the provider, issued for our app, unexpired and (Apple) bound to our nonce. */
class SocialVerifierTest {
  static KeyPair keys;
  static SocialVerifier verifier;

  @BeforeAll static void setUp() throws Exception {
    KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048);
    keys = g.generateKeyPair();
    var decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keys.getPublic()).build();
    verifier = new SocialVerifier(Map.of("GOOGLE", decoder, "APPLE", decoder), Map.of("GOOGLE", Set.of("our-google-client"), "APPLE", Set.of("com.lutiano.customer")));
  }

  private static String token(KeyPair signWith, String issuer, String audience, Instant expires, Map<String, Object> extra) throws Exception {
    JWTClaimsSet.Builder b = new JWTClaimsSet.Builder().issuer(issuer).audience(List.of(audience)).subject("subject-1").issueTime(new Date())
        .expirationTime(Date.from(expires)).claim("email", "a@example.com").claim("email_verified", true).claim("name", "Ada");
    extra.forEach(b::claim);
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), b.build());
    jwt.sign(new RSASSASigner(signWith.getPrivate()));
    return jwt.serialize();
  }

  private static int status(Runnable r) { return assertThrows(ResponseStatusException.class, r::run).getStatusCode().value(); }

  @Test void aGoogleTokenForOurAppIsAccepted() throws Exception {
    var id = verifier.verify("google", token(keys, "https://accounts.google.com", "our-google-client", Instant.now().plusSeconds(600), Map.of()), null);
    assertEquals("subject-1", id.subject());
    assertEquals("a@example.com", id.email());
    assertEquals("GOOGLE", id.provider());
  }

  @Test void aTokenForAnotherAppIsRefused() throws Exception {
    String other = token(keys, "https://accounts.google.com", "someone-elses-client", Instant.now().plusSeconds(600), Map.of());
    assertEquals(HttpStatus.UNAUTHORIZED.value(), status(() -> verifier.verify("GOOGLE", other, null)));
  }

  @Test void anExpiredOrForgedOrWrongIssuerTokenIsRefused() throws Exception {
    String expired = token(keys, "https://accounts.google.com", "our-google-client", Instant.now().minusSeconds(3600), Map.of());
    assertEquals(401, status(() -> verifier.verify("GOOGLE", expired, null)));
    KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
    g.initialize(2048);
    String forged = token(g.generateKeyPair(), "https://accounts.google.com", "our-google-client", Instant.now().plusSeconds(600), Map.of());
    assertEquals(401, status(() -> verifier.verify("GOOGLE", forged, null)));
    String wrongIssuer = token(keys, "https://evil.example.com", "our-google-client", Instant.now().plusSeconds(600), Map.of());
    assertEquals(401, status(() -> verifier.verify("GOOGLE", wrongIssuer, null)));
  }

  @Test void anAddressTheProviderDidNotVerifyIsNotTrusted() throws Exception {
    var id = verifier.verify("GOOGLE", token(keys, "https://accounts.google.com", "our-google-client", Instant.now().plusSeconds(600), Map.of("email_verified", false)), null);
    assertNull(id.email());
  }

  @Test void appleMustMatchTheNonceTheAppGenerated() throws Exception {
    String hashed = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("raw-nonce".getBytes(StandardCharsets.UTF_8)));
    String ok = token(keys, "https://appleid.apple.com", "com.lutiano.customer", Instant.now().plusSeconds(600), Map.of("nonce", hashed));
    assertEquals("subject-1", verifier.verify("APPLE", ok, "raw-nonce").subject());
    assertEquals(401, status(() -> verifier.verify("APPLE", ok, "another-nonce")));
  }

  @Test void aProviderWithNoConfiguredAudienceIsOff() {
    var off = new SocialVerifier(Map.of(), Map.of("GOOGLE", Set.of(), "APPLE", Set.of()));
    assertFalse(off.enabled("GOOGLE"));
    assertEquals(409, status(() -> off.verify("GOOGLE", "x", null)));
    assertTrue(verifier.enabled("APPLE"));
  }
}
