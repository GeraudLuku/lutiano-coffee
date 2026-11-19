package com.lutiano.api.service.loyalty;

import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.loyalty.LoyaltyRepository;
import com.lutiano.api.repository.loyalty.PointsRepository;

/**
 * The only code that changes point balances. Earned points go into a batch that expires on its own date; spending
 * takes the earliest-expiring batches first; a balance never goes negative. Every change writes an append-only
 * ledger entry with the balance after it. Callers lock the customer first (via {@link #lock}) and run in a transaction.
 */
@Component
public class PointsLedger {
  public static final String EARNED = "EARNED", REDEEMED = "REDEEMED", REDEMPTION_REVERSED = "REDEMPTION_REVERSED",
      REFUND_REVERSAL = "REFUND_REVERSAL", EXPIRED = "EXPIRED", ADJUSTED = "ADJUSTED";

  public record Entry(String type, String sourceType, String sourceId, String reference, String description, String actorId) {}
  public record Taken(String batchId, int points) {}

  private final PointsRepository points;
  private final LoyaltyRepository config;

  public PointsLedger(PointsRepository points, LoyaltyRepository config) { this.points = points; this.config = config; }

  /** Locks the customer row and expires any batches that are already past their date. */
  @Transactional(propagation = Propagation.MANDATORY)
  public Map<String, Object> lock(String customerId) {
    Map<String, Object> c = points.lockCustomer(customerId).orElseThrow(() -> Validate.notFound("Customer not found."));
    expireDue(customerId);
    return c;
  }

  /** Adds points in a new batch that expires after the configured number of months. Returns the batch id. */
  @Transactional(propagation = Propagation.MANDATORY)
  public String credit(String customerId, int amount, Entry e) {
    if (amount <= 0) throw new IllegalArgumentException("credit must be positive");
    int months = Rows.asInt(config.settings().get("expiry_months"));
    Timestamp expires = Timestamp.from(ZonedDateTime.now(Periods.DOUALA).plusMonths(months).toInstant());
    String batchId = UUID.randomUUID().toString();
    points.insertBatch(batchId, customerId, e.sourceType(), e.sourceId(), amount, expires);
    write(customerId, amount, e);
    return batchId;
  }

  /** Puts points back into a specific batch (a reversed redemption returns points to the batches it spent). */
  @Transactional(propagation = Propagation.MANDATORY)
  public void restore(String customerId, List<Taken> taken, Entry e) {
    int total = 0;
    for (Taken t : taken) { points.changeRemaining(t.batchId(), t.points()); total += t.points(); }
    if (total > 0) write(customerId, total, e);
  }

  /**
   * Spends points, earliest-expiring batches first. With {@code capAtBalance} the debit stops at the balance
   * (refund reversals: no negative balances); otherwise an insufficient balance is refused.
   * {@code firstBatch} is drained before the others when given (a refund takes back the sale's own points first).
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public List<Taken> debit(String customerId, int amount, Entry e, boolean capAtBalance, String firstBatch) {
    int balance = points.balance(customerId);
    if (amount > balance && !capAtBalance) throw Validate.conflict("Not enough points: the balance is " + balance + " pts.");
    int toTake = Math.min(amount, balance);
    List<Taken> taken = new ArrayList<>();
    if (toTake <= 0) return taken;
    List<Map<String, Object>> batches = new ArrayList<>(points.spendableBatches(customerId));
    if (firstBatch != null) batches.sort((a, b) -> Boolean.compare(!firstBatch.equals(a.get("id")), !firstBatch.equals(b.get("id"))));
    int left = toTake;
    for (Map<String, Object> b : batches) {
      if (left == 0) break;
      int take = Math.min(left, Rows.asInt(b.get("remaining")));
      points.changeRemaining((String) b.get("id"), -take);
      taken.add(new Taken((String) b.get("id"), take));
      left -= take;
    }
    write(customerId, -toTake, e);
    return taken;
  }

  /** Expires every batch past its date (one customer, or all when null). Returns the points expired. */
  @Transactional(propagation = Propagation.MANDATORY)
  public int expireDue(String customerId) {
    int total = 0;
    for (Map<String, Object> b : points.dueBatches(customerId)) {
      int left = Rows.asInt(b.get("remaining"));
      String owner = (String) b.get("customer_id");
      points.changeRemaining((String) b.get("id"), -left);
      String expiredOn = ((Timestamp) b.get("expires_at")).toInstant().atZone(Periods.DOUALA).toLocalDate().toString();
      points.insertLedger(UUID.randomUUID().toString(), owner, EXPIRED, -left, points.balance(owner), "BATCH", (String) b.get("id"), null,
          left + " pts earned " + ((Timestamp) b.get("earned_at")).toInstant().atZone(Periods.DOUALA).toLocalDate() + " expired on " + expiredOn, null);
      total += left;
    }
    return total;
  }

  private void write(String customerId, int delta, Entry e) {
    points.insertLedger(UUID.randomUUID().toString(), customerId, e.type(), delta, points.balance(customerId), e.sourceType(), e.sourceId(),
        e.reference(), e.description(), e.actorId());
  }

}
