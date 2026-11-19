package com.lutiano.api.repository.notification;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Announcements sent to sellers, and who they reach. */
@Repository
public class AnnouncementRepository {
  private static final String SELECT = "select a.*, u.full_name created_by_name, o.name operation_name from announcement a join staff_user u on u.id=a.created_by left join operation o on o.id=a.operation_id";
  private final JdbcTemplate db;

  public AnnouncementRepository(JdbcTemplate db) { this.db = db; }

  public void insert(String id, String title, String body, String audience, String sellerRole, String operationId, boolean important, int recipients, String adminId) {
    db.update("insert into announcement(id,title,body,audience,seller_role,operation_id,important,recipient_count,created_by,created_at) values(?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, title, body, audience, sellerRole, operationId, important, recipients, adminId);
  }

  /** The active sellers an audience reaches: all of them, one seller role, or those assigned to one operation. */
  public List<String> recipients(String audience, String role, String operationId) {
    return switch (audience) {
      case "CUSTOMERS" -> db.queryForList("select id from customer where registered_at is not null and status='ACTIVE' and deleted_at is null and marketing_consent=true", String.class);
      case "ROLE" -> db.queryForList("select id from staff_user where role='SELLER' and active=true and seller_role=?", String.class, role);
      case "OPERATION" -> db.queryForList("select u.id from seller_operation x join staff_user u on u.id=x.seller_id where x.operation_id=? and u.active=true", String.class, operationId);
      default -> db.queryForList("select id from staff_user where role='SELLER' and active=true", String.class);
    };
  }

  public Map<String, Object> find(String id) { return db.queryForMap(SELECT + " where a.id=?", id); }

  /** Newest first. */
  public PageResponse<Map<String, Object>> page(PageRequest req) { return new SqlFilter(SELECT).page(db, "order by a.created_at desc, a.id", req); }
}
