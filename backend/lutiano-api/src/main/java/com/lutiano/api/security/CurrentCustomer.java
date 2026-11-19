package com.lutiano.api.security;

import java.sql.Timestamp;
import java.time.Instant;
import com.lutiano.api.utils.Validate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Resolves the caller of a /customer endpoint from the JWT subject, re-reading the account each time so deletion, suspension and
 * "sign out everywhere" apply at once. The caller is returned as an {@link Actor} with role CUSTOMER, so services written for people
 * (notifications, devices) work for customers too.
 */
@Component
public class CurrentCustomer {
  private final JdbcTemplate db;

  public CurrentCustomer(JdbcTemplate db) { this.db = db; }

  public Actor get(Authentication a) {
    var rows = db.query("select id,coalesce(email,phone),full_name,tokens_valid_after from customer where id=? and status='ACTIVE' and deleted_at is null",
        (rs, n) -> new Object[] {new Actor(rs.getString(1), rs.getString(2), rs.getString(3), Roles.CUSTOMER), rs.getTimestamp(4)}, a.getName());
    if (rows.isEmpty()) throw Validate.unauthorized("Your session is no longer valid. Sign in again.");
    Timestamp after = (Timestamp) rows.get(0)[1];
    Instant issuedAt = a.getPrincipal() instanceof Jwt jwt ? jwt.getIssuedAt() : null;
    if (after != null && (issuedAt == null || issuedAt.isBefore(after.toInstant()))) throw Validate.unauthorized("You were signed out. Sign in again.");
    return (Actor) rows.get(0)[0];
  }
}
