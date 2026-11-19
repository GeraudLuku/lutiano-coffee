package com.lutiano.api.security;

import java.time.Instant;
import com.lutiano.api.utils.Validate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.lutiano.api.repository.security.ActorRepository;

/**
 * Resolves the caller from the JWT subject and enforces role and location access.
 * Roles are re-read from the database so role changes and deactivation apply immediately.
 */
@Component
public class CurrentUser {
  private final ActorRepository actors;

  public CurrentUser(ActorRepository actors) { this.actors = actors; }

  /**
   * The signed-in staff member. Rejects deactivated accounts and tokens issued before the account's
   * sessions were revoked (sign out of all devices, password reset, deactivation).
   */
  public Actor get(Authentication a) {
    ActorRepository.Session session = actors.findActive(a.getName())
        .orElseThrow(() -> Validate.unauthorized("Your session is no longer valid. Sign in again."));
    Instant issuedAt = a.getPrincipal() instanceof Jwt jwt ? jwt.getIssuedAt() : null;
    if (session.tokensValidAfter() != null && (issuedAt == null || issuedAt.isBefore(session.tokensValidAfter())))
      throw Validate.unauthorized("You were signed out. Sign in again.");
    return session.actor();
  }

  public Actor require(Authentication a, String... roles) {
    Actor actor = get(a);
    if (!actor.is(roles)) throw Validate.forbidden("You do not have permission to do this.");
    return actor;
  }

  /** An active seller account, for an administrator recording a sale on their behalf. */
  public Actor activeSeller(String id) {
    return actors.findActive(id).map(ActorRepository.Session::actor).filter(Actor::isSeller)
        .orElseThrow(() -> Validate.notFound("Active seller not found."));
  }

  public boolean isAssigned(Actor seller, String operationId) { return actors.isAssigned(seller.id(), operationId); }

  /** Staff may see any operation; sellers only those assigned to them. */
  public void requireOperationAccess(Actor actor, String operationId) {
    if (actor.isSeller() && !actors.isAssigned(actor.id(), operationId))
      throw Validate.forbidden("This sales operation is not assigned to you.");
  }
}
