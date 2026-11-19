package com.lutiano.api.repository.account;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Requests sellers send to the LUTIANO team, and the team's replies. */
@Repository
public class SupportRepository {
  private static final String SELECT = "select r.id, r.reference, r.user_id, r.requester_type, coalesce(u.full_name, c.full_name) user_name, r.type, r.subject, r.description, r.status, r.attachment_name, r.attachment_type," +
      " (r.attachment_key is not null) has_attachment, r.reply, rb.full_name replied_by_name, r.replied_at, r.created_at" +
      " from support_request r left join staff_user u on u.id=r.user_id and r.requester_type='STAFF' left join customer c on c.id=r.user_id and r.requester_type='CUSTOMER'" +
      " left join staff_user rb on rb.id=r.replied_by";
  private final JdbcTemplate db;

  public SupportRepository(JdbcTemplate db) { this.db = db; }

  public String insert(String reference, String userId, String requesterType, String type, String subject, String description, String key, String name, String contentType) {
    String id = UUID.randomUUID().toString();
    db.update("insert into support_request(id,reference,user_id,requester_type,type,subject,description,attachment_key,attachment_name,attachment_type,created_at) values(?,?,?,?,?,?,?,?,?,?,UTC_TIMESTAMP())",
        id, reference, userId, requesterType, type, subject, description, key, name, contentType);
    return id;
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where r.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> attachment(String id) {
    return db.queryForList("select user_id, attachment_key, attachment_name, attachment_type from support_request where id=? and attachment_key is not null", id).stream().findFirst();
  }

  /** One person's requests, newest first. */
  public PageResponse<Map<String, Object>> ofUser(String userId, String status, PageRequest req) {
    return new SqlFilter(SELECT).and("r.user_id=?", userId).and("r.status=?", status).page(db, "order by r.created_at desc, r.id desc", req);
  }

  /** Every request (the admin inbox): open ones first, then newest. */
  public PageResponse<Map<String, Object>> all(String status, String requesterType, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("r.status=?", status).and("r.requester_type=?", requesterType);
    Map<String, Object> counts = db.queryForMap("select coalesce(sum(status='OPEN'),0) open_count, coalesce(sum(status='IN_PROGRESS'),0) in_progress, coalesce(sum(status='RESOLVED'),0) resolved from support_request");
    return f.page(db, "order by (r.status='RESOLVED'), r.created_at desc, r.id", req).withSummary(counts);
  }

  public void reply(String id, String status, String reply, String adminId) {
    db.update("update support_request set status=?, reply=coalesce(?,reply), replied_by=case when ? is null then replied_by else ? end, replied_at=case when ? is null then replied_at else UTC_TIMESTAMP() end where id=?",
        status, reply, reply, adminId, reply, id);
  }
}
