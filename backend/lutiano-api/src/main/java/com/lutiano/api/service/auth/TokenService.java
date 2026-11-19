package com.lutiano.api.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import com.lutiano.api.config.AppProperties;

/** Issues short-lived HS256 access tokens; the subject is the staff or customer id. */
@Service
public class TokenService {
  public record IssuedToken(String value, Instant expiresAt) {}

  private final JwtEncoder encoder;
  private final Duration ttl;

  public TokenService(JwtEncoder encoder, AppProperties.Jwt jwt) {
    this.encoder = encoder; this.ttl = Duration.ofHours(jwt.ttlHours());
  }

  public static final String STAFF_AUDIENCE = "lutiano-staff", CUSTOMER_AUDIENCE = "lutiano-customer";

  public IssuedToken issue(String staffId, String role) { return issue(staffId, role, STAFF_AUDIENCE); }

  /** A token for a customer of the Customer app: role CUSTOMER and its own audience, so it is refused by every staff endpoint. */
  public IssuedToken issueCustomer(String customerId) { return issue(customerId, "CUSTOMER", CUSTOMER_AUDIENCE); }

  private IssuedToken issue(String subject, String role, String audience) {
    Instant now = Instant.now(), expires = now.plus(ttl);
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("lutiano-api").audience(List.of(audience)).issuedAt(now).expiresAt(expires)
        .subject(subject).claim("roles", List.of(role)).build();
    String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    return new IssuedToken(token, expires);
  }
}
