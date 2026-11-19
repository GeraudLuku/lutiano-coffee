package com.lutiano.api.controller.audit;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import com.lutiano.api.security.CurrentUser;
import com.lutiano.api.security.Roles;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import com.lutiano.api.service.audit.AuditService;
import org.springframework.http.ResponseEntity;
import com.lutiano.api.models.response.audit.AuditItemResponse;
import com.lutiano.api.models.response.GenericApiResponse;
import com.lutiano.api.utils.Responses;

@RestController
public class AuditController {
  private final AuditService audit;
  private final CurrentUser current;

  public AuditController(AuditService audit, CurrentUser current) { this.audit = audit; this.current = current; }

  @GetMapping("/audit")
  public ResponseEntity<GenericApiResponse<PageResponse<AuditItemResponse>>> list(@RequestParam(required = false) String action,
                                        @RequestParam(required = false) String entityType, @RequestParam(required = false) String entityId,
                                        @RequestParam(required = false) String actorId, @RequestParam(required = false) LocalDate from,
                                        @RequestParam(required = false) LocalDate to, @RequestParam(required = false) String q,
                                        PageRequest paging, Authentication a) {
    current.require(a, Roles.STAFF);
    return Responses.page(audit.search(action, entityType, entityId, actorId, from, to, q, paging), AuditItemResponse.class);
  }

  /** Every action name in the log, for the filter. */
  @GetMapping("/audit/actions")
  public ResponseEntity<GenericApiResponse<List<String>>> actions(Authentication a) {
    current.require(a, Roles.STAFF);
    return Responses.ok(audit.actions());
  }
}
