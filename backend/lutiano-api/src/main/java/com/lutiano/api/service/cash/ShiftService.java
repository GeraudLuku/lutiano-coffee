package com.lutiano.api.service.cash;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.sql.Timestamp;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.security.Actor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.cash.CashRepository;
import com.lutiano.api.repository.cash.ShiftRepository;

/**
 * Seller cash shifts: expected cash = opening float + cash sales − cash refunds paid − approved outflows − remittances
 * (see CashLedger). Closing with a variance needs a reason and goes to manager review.
 */
@Service
public class ShiftService {
  private static final Logger log = LoggerFactory.getLogger(ShiftService.class);
  private final ShiftRepository shifts;
  private final AuditService audit;
  private final CashLedger ledger;
  private final CashRepository cash;
  private final NotificationService notifications;

  public ShiftService(ShiftRepository shifts, AuditService audit, CashLedger ledger, CashRepository cash, NotificationService notifications) {
    this.shifts = shifts; this.audit = audit; this.ledger = ledger; this.cash = cash; this.notifications = notifications;
  }

  /**
   * A shift left open from an earlier day gets one reminder a day, during the day (not in the middle of the night):
   * the drawer should be counted and the shift closed.
   */
  @Scheduled(fixedDelay = 30 * 60 * 1000L, initialDelay = 60 * 1000L)
  public void remindStaleShifts() {
    var now = ZonedDateTime.now(Periods.DOUALA);
    if (now.getHour() < 7 || now.getHour() >= 20) return;
    var todayStart = Periods.startOf(Periods.today());
    for (Map<String, Object> s : shifts.staleOpenShifts(todayStart, todayStart)) {
      Timestamp opened = (Timestamp) s.get("opened_at");
      String since = opened.toInstant().atZone(Periods.DOUALA).format(DateTimeFormatter.ofPattern("dd/MM"));
      try { notifications.send(NotificationCatalog.SHIFT_STILL_OPEN, (String) s.get("seller_id"), Map.of("since", since), "cash_shift", (String) s.get("id")); }
      catch (RuntimeException e) { log.error("Could not remind about shift {}: {}", s.get("id"), e.toString()); }
    }
  }

  @Transactional
  public Map<String, Object> open(Actor seller, Long openingFloat) {
    long f = openingFloat == null ? 0 : openingFloat;
    if (f < 0) throw Validate.bad("Opening float cannot be negative.");
    shifts.lockSeller(seller.id());
    if (shifts.hasOpenShift(seller.id())) throw Validate.conflict("You already have an open cash shift.");
    String id = UUID.randomUUID().toString();
    shifts.open(id, seller.id(), f);
    ledger.post(id, CashLedger.OPENING_FLOAT, f, "SHIFT", id, seller.id(), null);
    audit.log(seller, "SHIFT_OPENED", "cash_shift", id, "float " + f + " XAF");
    return shifts.findById(id);
  }

  @Transactional
  public Map<String, Object> close(Actor seller, Long countedCash, String varianceReason) {
    Map<String, Object> shift = shifts.lockOpenShift(seller.id()).orElseThrow(() -> Validate.conflict("You do not have an open cash shift."));
    if (countedCash == null || countedCash < 0) throw Validate.bad("Enter the counted cash (0 or more).");
    long expected = Rows.asLong(shift.get("expected_cash")), variance = countedCash - expected;
    String reason = Validate.optional(varianceReason, "Variance reason", 500);
    if (variance != 0 && reason == null) throw Validate.bad("A reason is required when counted cash differs from expected cash.");
    String id = (String) shift.get("id");
    shifts.close(id, countedCash, reason, variance == 0 ? "NOT_REQUIRED" : "PENDING_REVIEW");
    cash.insertCount(id, "INITIAL", expected, countedCash, reason, seller.id());
    cash.cancelOpenOutflows(id);
    audit.log(seller, "SHIFT_CLOSED", "cash_shift", id, "expected " + expected + ", counted " + countedCash);
    if (variance != 0) notifications.notifyApprovers(NotificationCatalog.SHIFT_VARIANCE, seller.id(), Map.of("seller", seller.fullName(), "amount", NotificationService.xaf(Math.abs(variance))), "cash_shift", id);
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", id); out.put("status", "CLOSED"); out.put("expectedCash", expected); out.put("countedCash", countedCash); out.put("variance", variance);
    return out;
  }

  /** Called inside the sale transaction: the seller's open shift (locked) or a conflict. */
  public String requireOpenShiftId(Actor seller) {
    return (String) shifts.lockOpenShift(seller.id())
        .orElseThrow(() -> Validate.conflict("Open a cash shift before recording a sale."))
        .get("id");
  }

  public void addCashSale(String shiftId, String saleId, String sellerId, long amount) {
    ledger.post(shiftId, CashLedger.CASH_SALE, amount, "SALE", saleId, sellerId, null);
  }

  /**
   * Pays a cash refund out of an open shift (inside the refund transaction). The shift must hold at least the amount;
   * otherwise there is no refund fund and the refund stays approved but unpaid.
   */
  public Map<String, Object> payRefund(String shiftId, String refundId, String actorId, long amount) {
    Map<String, Object> shift = shifts.lockOpenById(shiftId).orElseThrow(() -> Validate.conflict("That cash shift is not open."));
    long available = Rows.asLong(shift.get("expected_cash"));
    if (available < amount)
      throw Validate.conflict("This shift holds only " + available + " XAF, not enough to pay " + amount + " XAF. The refund stays approved but unpaid.");
    ledger.post(shiftId, CashLedger.REFUND_PAID, -amount, "REFUND", refundId, actorId, null);
    return shift;
  }

  public List<Map<String, Object>> openShifts(Actor actor) { return actor.isSeller() ? shifts.openForSeller(actor.id()) : shifts.allOpen(); }
}
