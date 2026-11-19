package com.lutiano.api.service.audit;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import com.lutiano.api.utils.Periods;
import java.time.LocalDate;
import com.lutiano.api.repository.audit.AuditRepository;

/** Append-only audit trail of business and security events. */
@Service
public class AuditService {
  private final AuditRepository repo;

  public AuditService(AuditRepository repo) { this.repo = repo; }

  public void log(Actor actor, String action, String entityType, String entityId, String details) {
    repo.insert(actor == null ? null : actor.id(), action, entityType, entityId, details);
  }

  public void log(Actor actor, String action, String entityType, String entityId) { log(actor, action, entityType, entityId, null); }

  public PageResponse<Map<String, Object>> search(String action, String entityType, String entityId, String actorId, LocalDate from, LocalDate to, String q, PageRequest req) {
    return repo.search(action, entityType, entityId, actorId, Periods.startOrNull(from), Periods.endOrNull(to), q, req);
  }

  public List<String> actions() { return repo.actions(); }

}
