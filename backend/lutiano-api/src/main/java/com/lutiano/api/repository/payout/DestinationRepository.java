package com.lutiano.api.repository.payout;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.lutiano.api.repository.search.SearchOutbox;
import com.lutiano.api.utils.Candidates;
import com.lutiano.api.definitions.enums.SearchType;

/** Mobile money numbers sellers are paid to. */
@Repository
public class DestinationRepository {
  private static final String SELECT =
      "select d.*, u.full_name seller_name, cb.full_name created_by_name, vb.full_name verified_by_name" +
      " from payout_destination d join staff_user u on u.id=d.seller_id join staff_user cb on cb.id=d.created_by left join staff_user vb on vb.id=d.verified_by";

  private final JdbcTemplate db;
  private final SearchOutbox search;

  public DestinationRepository(JdbcTemplate db, SearchOutbox search) { this.db = db; this.search = search; }

  public void insert(String id, String sellerId, String provider, String phone, String accountName, String note, String actorId) {
    db.update("insert into payout_destination(id,seller_id,provider,phone,account_name,status,note,created_by,created_at) values(?,?,?,?,?,'PENDING',?,?,UTC_TIMESTAMP())",
        id, sellerId, provider, phone, accountName, note, actorId);
    search.changed(SearchType.PAYOUT_DESTINATION, id);
  }

  public Optional<Map<String, Object>> find(String id) { return db.queryForList(SELECT + " where d.id=?", id).stream().findFirst(); }

  public Optional<Map<String, Object>> lock(String id) { return db.queryForList("select * from payout_destination where id=? for update", id).stream().findFirst(); }

  /** The seller's number for a provider that can be used for payouts, if verified. */
  public Optional<Map<String, Object>> verified(String sellerId, String provider) {
    return db.queryForList(SELECT + " where d.seller_id=? and d.provider=? and d.status='VERIFIED' order by d.verified_at desc limit 1", sellerId, provider).stream().findFirst();
  }

  public boolean activeNumberExists(String sellerId, String provider, String phone) {
    Integer n = db.queryForObject("select count(*) from payout_destination where seller_id=? and provider=? and phone=? and status in ('PENDING','VERIFIED')", Integer.class, sellerId, provider, phone);
    return n != null && n > 0;
  }

  /** A seller's numbers that are waiting for verification or in use (at most a few). */
  public List<Map<String, Object>> current(String sellerId) {
    return db.queryForList(SELECT + " where d.seller_id=? and d.status in ('PENDING','VERIFIED') order by d.provider, d.created_at desc", sellerId);
  }

  /** Marks the seller's other numbers for the provider as replaced: older pending ones when a new number is added, the old verified one when a new one is verified. */
  public void replaceOthers(String sellerId, String provider, String keepId, String status) {
    db.update("update payout_destination set status='REPLACED' where seller_id=? and provider=? and status=? and id<>?", sellerId, provider, status, keepId);
    search.changedAll(SearchType.PAYOUT_DESTINATION, db.queryForList("select id from payout_destination where seller_id=? and provider=? and status='REPLACED'", String.class, sellerId, provider));
  }

  public void setStatus(String id, String status, String actorId, String note) {
    db.update("update payout_destination set status=?,verified_by=?,verified_at=UTC_TIMESTAMP(),note=coalesce(?,note) where id=?", status, actorId, note, id);
    search.changed(SearchType.PAYOUT_DESTINATION, id);
  }

  public PageResponse<Map<String, Object>> search(String status, String provider, String sellerId, Candidates text, PageRequest req) {
    SqlFilter summary = filter(provider, sellerId, text);
    return filter(provider, sellerId, text).and("d.status=?", status).page(db, "order by d.created_at desc, d.id", req)
        .withSummary(summary.aggregate(db, "count(*) numbers, coalesce(sum(status='PENDING'),0) pending, coalesce(sum(status='VERIFIED'),0) verified"));
  }

  private static SqlFilter filter(String provider, String sellerId, Candidates text) {
    return new SqlFilter(SELECT).and("d.provider=?", provider).and("d.seller_id=?", sellerId).searchIds(text, "d.id", "d.phone");
  }

  public long pendingCount() { return db.queryForObject("select count(*) from payout_destination where status='PENDING'", Long.class); }
}
