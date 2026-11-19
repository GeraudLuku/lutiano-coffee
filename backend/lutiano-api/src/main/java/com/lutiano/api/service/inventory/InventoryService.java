package com.lutiano.api.service.inventory;

import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.product.ProductService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.repository.inventory.MovementRepository;
import com.lutiano.api.models.request.inventory.ReceiptRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.MovementType;
import com.lutiano.api.definitions.enums.ReceiptSource;

/** Balances, receipts into central stock, the movement ledger, reorder alerts and the approvals queue. */
@Service
public class InventoryService {
  public static final Set<String> RECEIPT_SOURCES = Enums.names(ReceiptSource.class);


  private final InventoryRepository inventory;
  private final MovementRepository movements;
  private final TransferService transfers;
  private final StockRequestService stockRequests;
  private final AdjustmentService adjustments;
  private final StocktakeService stocktakes;
  private final ProductService products;
  private final StockLedger ledger;
  private final ApprovalPolicy policy;
  private final CurrentUser current;
  private final AuditService audit;

  public InventoryService(InventoryRepository inventory, MovementRepository movements, TransferService transfers, StockRequestService stockRequests, AdjustmentService adjustments,
                          StocktakeService stocktakes, ProductService products, StockLedger ledger, ApprovalPolicy policy,
                          CurrentUser current, AuditService audit) {
    this.inventory = inventory; this.movements = movements; this.transfers = transfers; this.stockRequests = stockRequests; this.adjustments = adjustments;
    this.stocktakes = stocktakes; this.products = products; this.ledger = ledger; this.policy = policy;
    this.current = current; this.audit = audit;
  }

  /** Staff may list every location; sellers must name one of theirs. */
  public PageResponse<Map<String, Object>> balances(Actor actor, String operationId, String q, String category, String status, List<String> ids, String sort, PageRequest req) {
    if (status != null && !status.isBlank()) Validate.oneOf(status, "Status", Set.of("out", "low", "attention"));
    if (ids != null && ids.size() > PageRequest.MAX_SIZE) throw Validate.bad("Ask for at most " + PageRequest.MAX_SIZE + " products at a time.");
    if (operationId == null) {
      if (actor.isSeller()) throw Validate.bad("operationId is required.");
      return inventory.allOperationBalancesPage(req);
    }
    current.requireOperationAccess(actor, operationId);
    return inventory.operationBalancesPage(operationId, q, category, status, ids, "quantity".equals(sort), req);
  }

  public List<Map<String, Object>> central() { return inventory.centralBalances(); }
  public PageResponse<Map<String, Object>> receipts(PageRequest req) { return inventory.receipts(req); }
  public PageResponse<Map<String, Object>> discrepancies(PageRequest req) { return inventory.discrepancies(req); }

  /** Reorder alerts with a suggested quantity that tops stock up to three times the threshold. */
  public List<Map<String, Object>> reorder() {
    List<Map<String, Object>> rows = inventory.reorderAlerts();
    for (Map<String, Object> r : rows) {
      int threshold = Rows.asInt(r.get("low_stock_threshold")), qty = Rows.asInt(r.get("quantity")), inbound = Rows.asInt(r.get("inbound"));
      r.put("suggested_quantity", Math.max(0, Math.max(threshold * 3, threshold + 1) - qty - inbound));
    }
    return rows;
  }

  @Transactional
  public String recordReceipt(Actor actor, ReceiptRequest in) {
    Validate.id(in.productId(), "Product");
    int qty = Validate.positive(in.quantity(), "Quantity");
    String source = Validate.oneOf(in.source() == null ? "SUPPLIER" : in.source(), "Source", RECEIPT_SOURCES);
    String supplier = Validate.optional(in.supplier(), "Supplier", 150);
    if ("SUPPLIER".equals(source) && supplier == null) throw Validate.bad("Enter the supplier for a supplier receipt.");
    String name = (String) products.requireStockItem(in.productId()).get("name");
    String id = UUID.randomUUID().toString();
    String reference = Validate.optional(in.reference(), "Reference", 80);
    inventory.insertReceipt(id, in.productId(), qty, actor.id(), source, supplier, reference, Validate.optional(in.note(), "Note", 500));
    MovementType type = "OPENING_BALANCE".equals(source) ? MovementType.OPENING_BALANCE : MovementType.RECEIPT;
    ledger.add(null, in.productId(), qty, type, new StockLedger.Ref("receipt", id, reference), actor.id(), supplier);
    audit.log(actor, "OPENING_BALANCE".equals(source) ? "OPENING_BALANCE_RECORDED" : "STOCK_RECEIVED", "product", in.productId(), qty + " x " + name);
    return id;
  }

  /** Full ledger for staff. @param location "CENTRAL", an operation id, or null for all. */
  public PageResponse<Map<String, Object>> ledger(String location, String productId, String type, LocalDate from, LocalDate to, PageRequest req) {
    return movements.search(location, productId, type,
        Periods.startOrNull(from), Periods.endOrNull(to), req);
  }

  /** In/out history at one operation (Seller app and location pages). */
  public PageResponse<Map<String, Object>> operationMovements(Actor actor, String operationId, String productId, String direction, LocalDate from, LocalDate to, PageRequest req) {
    current.requireOperationAccess(actor, operationId);
    if (direction != null && !direction.isBlank()) Validate.oneOf(direction, "Direction", Set.of("IN", "OUT"));
    checkPeriod(from, to);
    return movements.forOperation(operationId, productId, direction, Periods.startOrNull(from), Periods.endOrNull(to), req);
  }

  /** Units in and out at one operation over a period (Douala days, inclusive), and the most consumed products. */
  public Map<String, Object> operationSummary(Actor actor, String operationId, String productId, LocalDate from, LocalDate to) {
    current.requireOperationAccess(actor, operationId);
    checkPeriod(from, to);
    return movements.summary(operationId, productId, Periods.startOrNull(from), Periods.endOrNull(to), 5);
  }

  private static void checkPeriod(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) throw Validate.bad("The end date must not be before the start date.");
  }

  /** Everything waiting for a manager, in one call. */
  public Map<String, Object> approvals() {
    Map<String, Object> out = new LinkedHashMap<>();
    out.put("secondApproverRequired", policy.requiresSecondApprover());
    out.put("transfersToApprove", transfers.inStatus(TransferService.PENDING));
    out.put("transfersToDispatch", transfers.inStatus(TransferService.APPROVED));
    out.put("requestsToApprove", stockRequests.inStatus(TransferService.PENDING));
    out.put("requestsToDispatch", stockRequests.inStatus(TransferService.APPROVED));
    out.put("adjustmentsToApprove", adjustments.pending());
    out.put("stocktakesToApprove", stocktakes.submitted());
    return out;
  }

}
