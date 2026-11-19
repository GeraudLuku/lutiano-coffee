package com.lutiano.api.repository.search;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.lutiano.api.definitions.enums.SearchType;

/**
 * Write side of the outbox. Services call {@link #changed} inside the same transaction as their MariaDB write, so a
 * rolled-back change leaves nothing behind and a committed one is never lost, whether or not Elasticsearch is up.
 */
@Component
public class SearchOutbox {
  private final JdbcTemplate db;

  public SearchOutbox(JdbcTemplate db) { this.db = db; }

  /** Records that {@code type}/{@code id} was created, changed or deleted. Repeats for the same record coalesce. */
  public void changed(SearchType type, String id) {
    if (id == null) return;
    // a newer pending row replaces an older one, so a burst of edits is indexed once, at the latest revision
    db.update("delete from search_outbox where type = ? and source_id = ? and status = 'PENDING'", type.name(), id);
    db.update("insert into search_outbox (type, source_id) values (?, ?)", type.name(), id);
  }

  public void changedAll(SearchType type, List<String> ids) { ids.forEach(i -> changed(type, i)); }

  /** Counts for the admin status screen. */
  public Map<String, Object> status() {
    Map<String, Object> m = db.queryForMap("""
        select coalesce(sum(status='PENDING'),0) pending, coalesce(sum(status='FAILED'),0) failed,
               min(case when status='PENDING' then created_at end) oldestPending, max(case when status='DONE' then id end) lastDoneId
          from search_outbox""");
    return m;
  }

  /** Puts failed changes back in the queue (after the cause was fixed). */
  public int retryFailed() {
    return db.update("update search_outbox set status='PENDING', attempts=0, next_attempt_at=now(), error=null where status='FAILED'");
  }

  /** Housekeeping: finished rows after 7 days, failed rows after 30. */
  public void purge() {
    db.update("delete from search_outbox where status='DONE' and created_at < now() - interval 7 day");
    db.update("delete from search_outbox where status='FAILED' and created_at < now() - interval 30 day");
  }
}
