package com.lutiano.api.service.cash;

import java.util.Map;
import java.util.UUID;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.cash.CashRepository;

/**
 * The only code that changes a shift's expected cash. Each movement is an append-only ledger entry with the running
 * balance after it, linked to the shift, the actor and the source record (sale, refund, outflow, remittance).
 * Must run inside the caller's transaction.
 */
@Component
public class CashLedger {
  public static final String OPENING_FLOAT = "OPENING_FLOAT", CASH_SALE = "CASH_SALE", REFUND_PAID = "REFUND_PAID",
      OUTFLOW = "OUTFLOW", REMITTANCE = "REMITTANCE";

  private final CashRepository cash;

  public CashLedger(CashRepository cash) { this.cash = cash; }

  /**
   * Posts a signed movement to an OPEN shift and returns the new expected cash.
   * Cash going out can never take the drawer below zero.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public long post(String shiftId, String type, long amount, String sourceType, String sourceId, String actorId, String note) {
    Map<String, Object> shift = cash.lockShift(shiftId).orElseThrow(() -> Validate.notFound("Cash shift not found."));
    if (!"OPEN".equals(shift.get("status"))) throw Validate.conflict("That cash shift is closed.");
    long balance = Rows.asLong(shift.get("expected_cash")) + amount;
    if (amount < 0 && balance < 0)
      throw Validate.conflict("The drawer holds only " + Rows.asLong(shift.get("expected_cash")) + " XAF, not enough for " + (-amount) + " XAF.");
    cash.setExpectedCash(shiftId, balance, REFUND_PAID.equals(type) ? -amount : 0);
    cash.insertLedgerEntry(UUID.randomUUID().toString(), shiftId, type, amount, balance, sourceType, sourceId, actorId, note);
    return balance;
  }
}
