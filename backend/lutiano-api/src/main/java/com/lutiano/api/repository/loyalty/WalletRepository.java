package com.lutiano.api.repository.loyalty;

import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.SqlFilter;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Rewards a customer redeemed in the app and has not used yet (or used): the wallet. */
@Repository
public class WalletRepository {
  private static final String SELECT = "select x.id,x.reference,x.reward_id,x.reward_name,x.points,x.status,x.use_state,x.valid_until,x.used_at,x.created_at," +
      " w.reward_type,w.description,w.product_id,p.image_url, uo.name used_operation_name" +
      " from reward_redemption x join reward w on w.id=x.reward_id left join product p on p.id=w.product_id left join operation uo on uo.id=x.used_operation_id";
  private final JdbcTemplate db;

  public WalletRepository(JdbcTemplate db) { this.db = db; }

  /** Makes a redemption a wallet item: usable until {@code validUntil} by whoever shows its token. */
  public void makeWalletItem(String redemptionId, String token, Timestamp validUntil) {
    db.update("update reward_redemption set use_state='AVAILABLE', token=?, valid_until=? where id=?", token, validUntil, redemptionId);
  }

  /** One customer's redemptions, newest first; state AVAILABLE, USED or EXPIRED (a reversed one counts as expired). */
  public PageResponse<Map<String, Object>> ofCustomer(String customerId, String state, PageRequest req) {
    SqlFilter f = new SqlFilter(SELECT).and("x.customer_id=?", customerId);
    if ("AVAILABLE".equals(state)) f.and("x.status='COMPLETED' and x.use_state='AVAILABLE' and x.valid_until>UTC_TIMESTAMP()");
    else if ("USED".equals(state)) f.and("x.status='COMPLETED' and x.use_state='USED'");
    else if ("EXPIRED".equals(state)) f.and("(x.status='REVERSED' or x.use_state='EXPIRED' or (x.use_state='AVAILABLE' and x.valid_until<=UTC_TIMESTAMP()))");
    return f.page(db, "order by x.created_at desc, x.id", req);
  }

  public Optional<Map<String, Object>> ofCustomer(String customerId, String id) {
    return db.queryForList(SELECT + " where x.customer_id=? and x.id=?", customerId, id).stream().findFirst();
  }

  public Optional<String> tokenOf(String id) { return db.queryForList("select token from reward_redemption where id=?", String.class, id).stream().findFirst(); }

  /** A wallet item by its QR token, with its customer and what it is, locked for update. */
  public Optional<Map<String, Object>> lockByToken(String token) {
    return db.queryForList("select x.*, c.full_name customer_name, w.requires_stock, w.product_id, w.reward_type, w.discount_pct from reward_redemption x join customer c on c.id=x.customer_id" +
        " join reward w on w.id=x.reward_id where x.token=? for update", token).stream().findFirst();
  }

  public void markUsed(String id, String sellerId, String operationId) {
    db.update("update reward_redemption set use_state='USED', used_at=UTC_TIMESTAMP(), used_by=?, used_operation_id=? where id=?", sellerId, operationId, id);
  }

  /** Wallet items past their date that were never used. */
  public List<Map<String, Object>> due() {
    return db.queryForList("select id,customer_id,reward_name from reward_redemption where status='COMPLETED' and use_state='AVAILABLE' and valid_until<=UTC_TIMESTAMP()");
  }

  public boolean expire(String id) { return db.update("update reward_redemption set use_state='EXPIRED' where id=? and use_state='AVAILABLE'", id) == 1; }

  public int availableCount(String customerId) {
    Integer n = db.queryForObject("select count(*) from reward_redemption where customer_id=? and status='COMPLETED' and use_state='AVAILABLE' and valid_until>UTC_TIMESTAMP()", Integer.class, customerId);
    return n == null ? 0 : n;
  }
}
