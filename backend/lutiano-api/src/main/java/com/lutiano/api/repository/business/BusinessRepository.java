package com.lutiano.api.repository.business;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The single row of business details shown in Settings. */
@Repository
public class BusinessRepository {
  private final JdbcTemplate db;

  public BusinessRepository(JdbcTemplate db) { this.db = db; }

  public Map<String, Object> find() {
    return db.queryForMap("select name,description,email,phone,address,updated_at from business_info where id=1");
  }

  public void update(String name, String description, String email, String phone, String address, String actorId) {
    db.update("update business_info set name=?,description=?,email=?,phone=?,address=?,updated_by=?,updated_at=UTC_TIMESTAMP() where id=1",
        name, description, email, phone, address, actorId);
  }
}
