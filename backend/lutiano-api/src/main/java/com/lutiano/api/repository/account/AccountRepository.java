package com.lutiano.api.repository.account;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A person's own profile fields, a pending phone change, and their sign-in history. */
@Repository
public class AccountRepository {
  private final JdbcTemplate db;

  public AccountRepository(JdbcTemplate db) { this.db = db; }

  public void updateProfile(String id, String fullName, String language) {
    db.update("update staff_user set full_name=coalesce(?,full_name), language=coalesce(?,language) where id=?", fullName, language, id);
  }

  public void setPhone(String id, String phone) { db.update("update staff_user set phone=? where id=?", phone, id); }

  /** Whether another active account already uses this phone. */
  public boolean phoneTakenByOther(String phone, String id) {
    Integer n = db.queryForObject("select count(*) from staff_user where phone=? and id<>? and active=true", Integer.class, phone, id);
    return n != null && n > 0;
  }

  public void savePendingPhone(String id, String phone) {
    db.update("insert into phone_change(user_id,phone) values(?,?) on duplicate key update phone=values(phone), created_at=CURRENT_TIMESTAMP", id, phone);
  }

  public Optional<String> pendingPhone(String id) {
    return db.queryForList("select phone from phone_change where user_id=? and created_at > date_sub(UTC_TIMESTAMP(), interval 30 minute)", String.class, id).stream().findFirst();
  }

  public void clearPendingPhone(String id) { db.update("delete from phone_change where user_id=?", id); }

  /** Signs out every token issued before [at]. */
  public void revokeTokensBefore(String id, Timestamp at) { db.update("update staff_user set tokens_valid_after=? where id=?", at, id); }

  /** The person's sign-ins and account-security changes, newest first. */
  public PageResponse<Map<String, Object>> activity(String id, List<String> actions, PageRequest req) {
    return new SqlFilter("select action, details client, created_at from audit_event").and("actor_id=?", id).andIn("action", actions)
        .page(db, "order by created_at desc, id desc", req);
  }
}
