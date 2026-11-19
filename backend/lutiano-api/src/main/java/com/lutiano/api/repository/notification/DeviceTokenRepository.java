package com.lutiano.api.repository.notification;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The phones that receive push notifications: one row per Firebase token, owned by whoever signed in last on that phone. */
@Repository
public class DeviceTokenRepository {
  private final JdbcTemplate db;

  public DeviceTokenRepository(JdbcTemplate db) { this.db = db; }

  public void register(String userId, String token, String platform) {
    db.update("insert into device_token(token,user_id,platform) values(?,?,?) on duplicate key update user_id=values(user_id), platform=values(platform), last_seen_at=CURRENT_TIMESTAMP",
        token, userId, platform);
  }

  /** Only removes the phone if it belongs to this person, so signing out of one account cannot silence another's phone. */
  public void unregister(String userId, String token) { db.update("delete from device_token where token=? and user_id=?", token, userId); }

  public List<String> tokens(String userId) { return db.queryForList("select token from device_token where user_id=?", String.class, userId); }

  /** Forgets a token Firebase no longer accepts. */
  public void forget(String token) { db.update("delete from device_token where token=?", token); }
}
