package com.lutiano.api.service.inventory;

import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.service.product.ProductService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.inventory.AdjustmentRepository;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.MovementType;
import com.lutiano.api.definitions.enums.StockAdjustmentReason;

/**
 * Stock corrections outside sales and transfers. A request changes nothing until a manager approves it.
 * Sellers may report problems at their own operation but never approve.
 */
@Service
public class AdjustmentService {
  public static final String PENDING = "PENDING_APPROVAL", APPLIED = "APPLIED", REJECTED = "REJECTED", CANCELLED = "CANCELLED";
  public static final String RETURN_TO_CENTRAL = "RETURN_TO_CENTRAL";
  /** Reasons that can only remove stock. */
  private static final Set<String> DECREASE_ONLY = Set.of("DAMAGED", "EXPIRED", "MISSING", RETURN_TO_CENTRAL);
  /** Reasons that can only add stock. */
  private static final Set<String> INCREASE_ONLY = Set.of("FOUND", "CUSTOMER_RETURN");
  /** CUSTOMER_RETURN: goods handed back with a refund (requested from the refund, never automatic). */
  public static final String CUSTOMER_RETURN = "CUSTOMER_RETURN";
  public static final Set<String> REASONS = Enums.names(StockAdjustmentReason.class);

  private final AdjustmentRepository adjustments;
  private final InventoryRepository inventory;
  private final OperationRepository operations;
  private final ProductService products;
  private final StockLedger ledger;
  private final ApprovalPolicy policy;
  private final CurrentUser current;
  private final AuditService audit;
  private final NotificationService notifications;

  public AdjustmentService(AdjustmentRepository adjustments, InventoryRepository inventory, OperationRepository operations, ProductService products,
                           StockLedger ledger, ApprovalPolicy policy, CurrentUser current, AuditService audit, NotificationService notifications) {
    this.notifications = notifications;
    this.adjustments = adjustments; this.inventory = inventory; this.operations = operations; this.products = products;
    this.ledger = ledger; this.policy = policy; this.current = current; this.audit = audit;
  }

  /**
   * @param operationId null for central stock (staff only).
   * @param quantityDelta signed change; its sign must match the reason.
   */
  @Transactional
  public Map<String, Object> request(Actor actor, String operationId, String productId, Integer quantityDelta, String reason, String note) {
    Validate.id(productId, "Product");
    String why = Validate.oneOf(reason, "Reason", REASONS);
    String text = Validate.text(note, "Note", 500);
    if (quantityDelta == null || quantityDelta == 0) throw Validate.bad("Enter a quantity other than zero.");
    if (DECREASE_ONLY.contains(why) && quantityDelta > 0) throw Validate.bad("This reason can only remove stock.");
    if (INCREASE_ONLY.contains(why) && quantityDelta < 0) throw Validate.bad("This reason can only add stock.");
    if (operationId == null) {
      if (actor.isSeller()) throw Validate.forbidden("Sellers can only report adjustments for their own sales location.");
      if (RETURN_TO_CENTRAL.equals(why)) throw Validate.bad("Returns to central stock must come from a sales location.");
    } else {
      Validate.id(operationId, "Sales location");
      current.requireOperationAccess(actor, operationId);
      operations.findById(operationId).orElseThrow(() -> Validate.notFound("Sales location not found."));
    }
    Map<String, Object> product = products.requireStockItem(productId);
    if (quantityDelta < 0 && inventory.quantity(operationId, productId) < -quantityDelta)
      throw Validate.bad("Only " + inventory.quantity(operationId, productId) + " " + product.get("name") + " in stock here.");
    String id = UUID.randomUUID().toString();
    adjustments.insert(id, operationId, productId, quantityDelta, why, text, actor.id());
    audit.log(actor, "ADJUSTMENT_REQUESTED", "adjustment", id, signed(quantityDelta) + " " + product.get("name") + " (" + why + ")");
    notifications.notifyApprovers(NotificationCatalog.APPROVAL_STOCK, actor.id(), Map.of("what", signed(quantityDelta) + " × " + product.get("name"), "by", actor.fullName()), "adjustment", id);
    return adjustments.find(id);
  }

  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> j = lockPending(id, "approved");
    policy.requireDifferentApprover(actor, j.get("requested_by"), "an adjustment");
    String operationId = (String) j.get("operation_id"), productId = (String) j.get("product_id");
    int delta = Rows.asInt(j.get("quantity_delta"));
    String name = (String) products.get(productId).get("name");
    StockLedger.Ref ref = StockLedger.Ref.of("adjustment", id);
    if (RETURN_TO_CENTRAL.equals(j.get("reason"))) {
      if (!ledger.take(operationId, productId, -delta, MovementType.RETURN_OUT, ref, actor.id(), null))
        throw Validate.conflict("Not enough " + name + " left at the location to return " + (-delta) + ".");
      ledger.add(null, productId, -delta, MovementType.RETURN_IN, ref, actor.id(), null);
    } else {
      ledger.apply(operationId, productId, delta, MovementType.ADJUSTMENT, ref, actor.id(), (String) j.get("reason"), name);
    }
    adjustments.decide(id, APPLIED, actor.id(), Validate.optional(note, "Note", 500));
    audit.log(actor, "ADJUSTMENT_APPROVED", "adjustment", id, signed(delta) + " " + name);
    return adjustments.find(id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    lockPending(id, "rejected");
    adjustments.decide(id, REJECTED, actor.id(), Validate.text(reason, "Reason", 500));
    audit.log(actor, "ADJUSTMENT_REJECTED", "adjustment", id, reason);
    return adjustments.find(id);
  }

  @Transactional
  public Map<String, Object> cancel(Actor actor, String id) {
    Map<String, Object> j = lockPending(id, "cancelled");
    if (actor.isSeller() && !actor.id().equals(j.get("requested_by"))) throw Validate.forbidden("You can only cancel your own requests.");
    adjustments.decide(id, CANCELLED, actor.id(), null);
    audit.log(actor, "ADJUSTMENT_CANCELLED", "adjustment", id);
    return adjustments.find(id);
  }

  /** Staff see everything (optionally filtered); sellers see only their own requests. */
  public PageResponse<Map<String, Object>> search(Actor actor, String status, String location, PageRequest req) {
    return adjustments.page(status, location, actor.isSeller() ? actor.id() : null, req);
  }

  public List<Map<String, Object>> pending() { return adjustments.search(PENDING, null, null); }

  private Map<String, Object> lockPending(String id, String verb) {
    return Validate.status(adjustments.lock(Validate.id(id, "Adjustment")).orElseThrow(() -> Validate.notFound("Adjustment not found.")),
        "adjustment", verb, PENDING);
  }

  private static String signed(int n) { return n > 0 ? "+" + n : String.valueOf(n); }
}
