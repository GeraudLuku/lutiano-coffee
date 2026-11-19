package com.lutiano.api.service.inventory;

import com.lutiano.api.utils.Validate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.repository.inventory.MovementRepository;
import com.lutiano.api.definitions.enums.MovementType;

/**
 * The only way to change a stock balance. Every change updates the balance atomically and appends a
 * ledger row in the caller's transaction, then refreshes the stock alert for that product and place. A null operation id means central stock.
 */
@Service
public class StockLedger {
    /** What caused a movement, e.g. ("sale", saleId, "SL-20261116-00001"). */
  public record Ref(String type, String id, String label) {
    public static Ref of(String type, String id) { return new Ref(type, id, null); }
  }

  private final InventoryRepository balances;
  private final MovementRepository movements;
  private final StockAlerts alerts;

  public StockLedger(InventoryRepository balances, MovementRepository movements, StockAlerts alerts) { this.balances = balances; this.movements = movements; this.alerts = alerts; }

  @Transactional(propagation = Propagation.MANDATORY)
  public void add(String operationId, String productId, int quantity, MovementType type, Ref ref, String actorId, String note) {
    if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
    if (operationId == null) balances.addCentral(productId, quantity);
    else balances.addToOperation(operationId, productId, quantity);
    record(operationId, productId, quantity, type, ref, actorId, note);
  }

  /** Removes stock if enough is available; returns false (and records nothing) otherwise. */
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean take(String operationId, String productId, int quantity, MovementType type, Ref ref, String actorId, String note) {
    if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
    boolean ok = operationId == null ? balances.takeCentral(productId, quantity) : balances.takeFromOperation(operationId, productId, quantity);
    if (ok) record(operationId, productId, -quantity, type, ref, actorId, note);
    return ok;
  }

  /** Applies a signed change, failing with 409 if it would make the balance negative. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(String operationId, String productId, int delta, MovementType type, Ref ref, String actorId, String note, String productName) {
    if (delta > 0) add(operationId, productId, delta, type, ref, actorId, note);
    else if (delta < 0 && !take(operationId, productId, -delta, type, ref, actorId, note))
      throw Validate.conflict("Not enough " + productName + " in stock to remove " + (-delta) + ". Recount and try again.");
  }

  private void record(String operationId, String productId, int delta, MovementType type, Ref ref, String actorId, String note) {
    int after = balances.quantity(operationId, productId);
    movements.insert(operationId, productId, delta, after, type.name(),
        ref == null ? null : ref.type(), ref == null ? null : ref.id(), ref == null ? null : ref.label(), actorId, note);
    alerts.check(operationId, productId);
  }
}
