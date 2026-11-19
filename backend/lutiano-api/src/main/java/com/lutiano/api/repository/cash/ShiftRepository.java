package com.lutiano.api.repository.cash;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ShiftRepository {
  private static final String SELECT = "select c.*,u.full_name seller_name,(c.counted_cash-c.expected_cash) variance,(select count(*) from sale s where s.shift_id=c.id) sale_count from cash_shift c join staff_user u on u.id=c.seller_id";
  private final JdbcTemplate db;

  public ShiftRepository(JdbcTemplate db) { this.db = db; }

  /** Locks the seller row so concurrent "open shift" requests are serialised. */
  public void lockSeller(String sellerId) { db.queryForObject("select id from staff_user where id=? for update", String.class, sellerId); }

  /** The seller's open shift, row-locked for the rest of the transaction. */
  public Optional<Map<String, Object>> lockOpenShift(String sellerId) {
    return db.queryForList("select * from cash_shift where seller_id=? and status='OPEN' for update", sellerId).stream().findFirst();
  }

  public boolean hasOpenShift(String sellerId) {
    Integer n = db.queryForObject("select count(*) from cash_shift where seller_id=? and status='OPEN'", Integer.class, sellerId);
    return n != null && n > 0;
  }

  public void open(String id, String sellerId, long openingFloat) {
    db.update("insert into cash_shift(id,seller_id,opening_float,expected_cash,status,opened_at) values(?,?,?,0,'OPEN',UTC_TIMESTAMP())", id, sellerId, openingFloat);
  }

  public void close(String id, long countedCash, String varianceReason, String reviewStatus) {
    db.update("update cash_shift set counted_cash=?,variance_reason=?,status='CLOSED',closed_at=UTC_TIMESTAMP(),review_status=? where id=?",
        countedCash, varianceReason, reviewStatus, id);
  }


  /** An open shift by id, row-locked for the rest of the transaction. */
  public Optional<Map<String, Object>> lockOpenById(String id) {
    return db.queryForList("select c.*,u.full_name seller_name from cash_shift c join staff_user u on u.id=c.seller_id where c.id=? and c.status='OPEN' for update", id).stream().findFirst();
  }



  public Map<String, Object> findById(String id) { return db.queryForMap(SELECT + " where c.id=?", id); }
  public List<Map<String, Object>> openForSeller(String sellerId) { return db.queryForList(SELECT + " where c.status='OPEN' and c.seller_id=?", sellerId); }
  public List<Map<String, Object>> allOpen() { return db.queryForList(SELECT + " where c.status='OPEN' order by c.opened_at desc"); }

  /** Open shifts opened before [before] whose seller has not been reminded since [remindedSince]. */
  public List<Map<String, Object>> staleOpenShifts(Timestamp before, Timestamp remindedSince) {
    return db.queryForList("select c.id, c.seller_id, c.opened_at from cash_shift c where c.status='OPEN' and c.opened_at<? and not exists" +
        " (select 1 from notification_delivery d where d.event_key='SHIFT_STILL_OPEN' and d.related_id=c.id and d.created_at>=?)", before, remindedSince);
  }
}
