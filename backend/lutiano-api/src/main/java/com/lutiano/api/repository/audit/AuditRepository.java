package com.lutiano.api.repository.audit;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.utils.Rows;
import java.sql.Timestamp;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Repository
public class AuditRepository {
  private static final String SELECT = "select e.*,u.full_name actor_name,u.role actor_role from audit_event e left join staff_user u on u.id=e.actor_id";
  private final JdbcTemplate db;

  public AuditRepository(JdbcTemplate db) { this.db = db; }

  public void insert(String actorId, String action, String entityType, String entityId, String details) {
    db.update("insert into audit_event(id,actor_id,action,entity_type,entity_id,details,ip,created_at) values(?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        UUID.randomUUID().toString(), actorId, action, entityType, entityId, details, clientIp());
  }

  /** The caller's address from the current request (first X-Forwarded-For hop when behind a proxy); null outside a request. */
  private static String clientIp() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a)) return null;
    String forwarded = a.getRequest().getHeader("X-Forwarded-For");
    String ip = forwarded != null && !forwarded.isBlank() ? forwarded.split(",")[0].trim() : a.getRequest().getRemoteAddr();
    return Rows.clip(ip, 45);
  }

  /** One page of the log; the summary counts matching events and the distinct people behind them. */
  /** Actions that move money, change access or override a control: counted as "critical" in the summary. */
  private static final String CRITICAL = "('REFUND_APPROVED','REFUND_PAID','PAYOUT_APPROVED','PAYOUT_PAID','PAYOUT_REVERSED','PAYOUT_THRESHOLD_CHANGED','CASH_OUTFLOW_APPROVED','SESSIONS_REVOKED','MFA_RESET','PASSWORD_RESET','STAFF_DEACTIVATED','PAYOUT_DESTINATION_VERIFIED')";

  public PageResponse<Map<String, Object>> search(String action, String entityType, String entityId, String actorId, Timestamp from, Timestamp to, String q, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("e.action=?", action).and("e.entity_type=?", entityType).and("e.entity_id=?", entityId).and("e.actor_id=?", actorId)
        .and("e.created_at>=?", from).and("e.created_at<?", to).search(q, "e.action", "e.details", "u.full_name");
    return f.page(db, "order by e.created_at desc, e.id desc", req).withSummary(f.aggregate(db, "count(*) events, count(distinct actor_id) users, coalesce(sum(action in " + CRITICAL + "),0) critical"));
  }

  public List<String> actions() { return db.queryForList("select distinct action from audit_event order by action", String.class); }



}
