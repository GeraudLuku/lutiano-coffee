package com.lutiano.api.service.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.security.Actor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.inventory.StocktakeRepository;
import com.lutiano.api.models.request.inventory.StocktakeCountRequest;
import com.lutiano.api.definitions.enums.MovementType;

/**
 * Cycle counts. Starting a stocktake snapshots expected quantities; staff enter counts and submit;
 * a second manager approves (differences are posted to the ledger) or rejects (back to counting).
 * Differences are applied relative to the snapshot, so sales made during the count are preserved.
 */
@Service
public class StocktakeService {
  public static final String COUNTING = "COUNTING", SUBMITTED = "SUBMITTED", APPROVED = "APPROVED", CANCELLED = "CANCELLED";


  private final StocktakeRepository stocktakes;
  private final OperationRepository operations;
  private final StockLedger ledger;
  private final ApprovalPolicy policy;
  private final AuditService audit;

  public StocktakeService(StocktakeRepository stocktakes, OperationRepository operations, StockLedger ledger, ApprovalPolicy policy, AuditService audit) {
    this.stocktakes = stocktakes; this.operations = operations; this.ledger = ledger; this.policy = policy; this.audit = audit;
  }

  /** @param operationId null counts central stock. One open count per location at a time. */
  @Transactional
  public Map<String, Object> start(Actor actor, String operationId, String note) {
    if (operationId != null) {
      Validate.id(operationId, "Sales location");
      operations.findById(operationId).orElseThrow(() -> Validate.notFound("Sales location not found."));
    }
    if (stocktakes.hasOpen(operationId)) throw Validate.conflict("A stock count is already open for this location. Finish or cancel it first.");
    String id = UUID.randomUUID().toString();
    stocktakes.insert(id, operationId, actor.id(), Validate.optional(note, "Note", 500));
    stocktakes.snapshotLines(id, operationId);
    audit.log(actor, "STOCKTAKE_STARTED", "stocktake", id, operationId == null ? "Central stock" : null);
    return detail(id);
  }

  public Map<String, Object> detail(String id) {
    Map<String, Object> s = stocktakes.find(Validate.id(id, "Stocktake"));
    s.put("lines", stocktakes.lines(id));
    return s;
  }

  public List<Map<String, Object>> submitted() { return stocktakes.search(SUBMITTED); }

  public PageResponse<Map<String, Object>> search(String status, String location, PageRequest req) {
    return stocktakes.page(status == null || status.isBlank() ? null : status, location == null || location.isBlank() ? null : location, req);
  }

  /** Saves counts as they are entered; null clears a count. */
  @Transactional
  public Map<String, Object> saveCounts(Actor actor, String id, List<StocktakeCountRequest> counts) {
    lockIn(id, COUNTING, "edited");
    if (counts == null) throw Validate.bad("No counts provided.");
    for (StocktakeCountRequest c : counts) {
      if (c.countedQuantity() != null && c.countedQuantity() < 0) throw Validate.bad("Counted quantities cannot be negative.");
      if (stocktakes.setCount(id, Validate.id(c.productId(), "Product"), c.countedQuantity()) != 1) throw Validate.bad("A product is not part of this count.");
    }
    return detail(id);
  }

  @Transactional
  public Map<String, Object> submit(Actor actor, String id) {
    lockIn(id, COUNTING, "submitted");
    int missing = stocktakes.uncountedLines(id);
    if (missing > 0) throw Validate.bad("Enter a count for every product (" + missing + " remaining). Enter 0 for products not found.");
    stocktakes.submit(id, actor.id());
    audit.log(actor, "STOCKTAKE_SUBMITTED", "stocktake", id);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> s = lockIn(id, SUBMITTED, "approved");
    policy.requireDifferentApprover(actor, s.get("submitted_by"), "a stock count");
    String operationId = (String) s.get("operation_id");
    int posted = 0;
    for (Map<String, Object> line : stocktakes.lines(id)) {
      int diff = Rows.asInt(line.get("difference"));
      if (diff == 0) continue;
      ledger.apply(operationId, (String) line.get("product_id"), diff, MovementType.STOCKTAKE,
          StockLedger.Ref.of("stocktake", id), actor.id(), null, (String) line.get("product_name"));
      posted++;
    }
    stocktakes.decide(id, APPROVED, actor.id(), Validate.optional(note, "Note", 500));
    audit.log(actor, "STOCKTAKE_APPROVED", "stocktake", id, posted + " differences posted");
    return detail(id);
  }

  /** Rejecting sends the count back for recounting. */
  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    lockIn(id, SUBMITTED, "rejected");
    stocktakes.reopen(id, actor.id(), Validate.text(reason, "Reason", 500));
    audit.log(actor, "STOCKTAKE_REJECTED", "stocktake", id, reason);
    return detail(id);
  }

  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String reason) {
    Map<String, Object> s = stocktakes.lock(Validate.id(id, "Stocktake")).orElseThrow(() -> Validate.notFound("Stock count not found."));
    if (!COUNTING.equals(s.get("status")) && !SUBMITTED.equals(s.get("status"))) throw Validate.conflict("This stock count is already closed.");
    stocktakes.decide(id, CANCELLED, actor.id(), Validate.text(reason, "Reason", 500));
    audit.log(actor, "STOCKTAKE_CANCELLED", "stocktake", id, reason);
    return detail(id);
  }

  private Map<String, Object> lockIn(String id, String expected, String verb) {
    return Validate.status(stocktakes.lock(Validate.id(id, "Stocktake")).orElseThrow(() -> Validate.notFound("Stock count not found.")),
        "stock count", verb, expected);
  }
}
