package com.lutiano.api.repository.security;

import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import com.lutiano.api.security.Actor;

@Repository
public class ActorRepository {
  /** An active account and the moment before which its sign-in tokens are no longer accepted (null = none revoked). */
  public record Session(Actor actor, Instant tokensValidAfter) {}

  private final JdbcTemplate db;

  public ActorRepository(JdbcTemplate db) { this.db = db; }

  public Optional<Session> findActive(String id) {
    return db.query("select id,email,full_name,role,tokens_valid_after from staff_user where id=? and active=true",
        (rs, n) -> {
          Timestamp after = rs.getTimestamp(5);
          return new Session(new Actor(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)), after == null ? null : after.toInstant());
        }, id).stream().findFirst();
  }

  public boolean isAssigned(String sellerId, String operationId) {
    Integer n = db.queryForObject("select count(*) from seller_operation where seller_id=? and operation_id=?", Integer.class, sellerId, operationId);
    return n != null && n > 0;
  }

  /** The seller's working role (FIXED_POS_BARISTA, MOBILE_BARISTA or MARKET_AGENT); null for staff or when none is set. */
  public String sellerRole(String id) {
    return db.queryForList("select seller_role from staff_user where id=?", String.class, id).stream().findFirst().orElse(null);
  }
}
