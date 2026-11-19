package com.lutiano.api.service.inventory;

import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.models.response.PageResponse;
import java.util.List;
import java.util.Map;
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
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.repository.inventory.TransferRepository;
import com.lutiano.api.definitions.enums.MovementType;

/**
 * Transfer lifecycle: PENDING_APPROVAL → APPROVED (central stock set aside) → IN_TRANSIT (dispatched)
 * → RECEIVED (recipient confirms the quantity), or REJECTED / CANCELLED before dispatch.
 */
@Service
public class TransferService {
  public static final String PENDING = "PENDING_APPROVAL", APPROVED = "APPROVED", IN_TRANSIT = "IN_TRANSIT",
      RECEIVED = "RECEIVED", REJECTED = "REJECTED", CANCELLED = "CANCELLED";

  private final TransferRepository transfers;
  private final InventoryRepository inventory;
  private final OperationRepository operations;
  private final ProductService products;
  private final StockLedger ledger;
  private final ApprovalPolicy policy;
  private final CurrentUser current;
  private final AuditService audit;
  private final NotificationService notifications;
  private final StockRequestService stockRequests;

  public TransferService(TransferRepository transfers, InventoryRepository inventory, OperationRepository operations, ProductService products,
                         StockLedger ledger, ApprovalPolicy policy, CurrentUser current, AuditService audit, NotificationService notifications,
                         StockRequestService stockRequests) {
    this.notifications = notifications; this.stockRequests = stockRequests;
    this.transfers = transfers; this.inventory = inventory; this.operations = operations; this.products = products;
    this.ledger = ledger; this.policy = policy; this.current = current; this.audit = audit;
  }

  /** Staff, or a seller for one of their own operations, requests stock. Nothing moves until approved. */
  @Transactional
  public Map<String, Object> request(Actor actor, String operationId, String productId, Integer quantity, String note) {
    Validate.id(operationId, "Sales location");
    Validate.id(productId, "Product");
    int qty = Validate.positive(quantity, "Quantity");
    current.requireOperationAccess(actor, operationId);
    if (!operations.isActive(operationId)) throw Validate.conflict("Stock can only be sent to an active sales location.");
    Map<String, Object> product = products.requireStockItem(productId);
    if (!Boolean.TRUE.equals(product.get("active"))) throw Validate.conflict("Inactive products cannot be transferred.");
    String id = UUID.randomUUID().toString();
    transfers.insertRequest(id, operationId, productId, qty, actor.id(), Validate.optional(note, "Note", 500));
    audit.log(actor, "TRANSFER_REQUESTED", "allocation", id, qty + " x " + product.get("name"));
    notifications.notifyApprovers(NotificationCatalog.APPROVAL_STOCK, actor.id(), Map.of("what", qty + " × " + product.get("name"), "by", actor.fullName()), "allocation", id);
    return transfers.find(id);
  }

  /** Approval sets the central stock aside so it cannot be promised twice. */
  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> t = lockInStatus(id, PENDING, "approved");
    policy.requireDifferentApprover(actor, t.get("created_by"), "a transfer request");
    if (!operations.isActive((String) t.get("operation_id"))) throw Validate.conflict("The destination sales location is inactive.");
    String productId = (String) t.get("product_id");
    int qty = Rows.asInt(t.get("sent_quantity"));
    String name = (String) products.get(productId).get("name");
    if (!ledger.take(null, productId, qty, MovementType.ALLOCATION_OUT, StockLedger.Ref.of("allocation", id), actor.id(), null))
      throw Validate.conflict("Not enough central stock: " + inventory.quantity(null, productId) + " " + name + " available, " + qty + " requested.");
    transfers.approve(id, actor.id(), Validate.optional(note, "Note", 500));
    audit.log(actor, "TRANSFER_APPROVED", "allocation", id, qty + " x " + name);
    return transfers.find(id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    lockInStatus(id, PENDING, "rejected");
    transfers.close(id, REJECTED, actor.id(), Validate.text(reason, "Reason", 500));
    audit.log(actor, "TRANSFER_REJECTED", "allocation", id, reason);
    return transfers.find(id);
  }

  @Transactional
  public Map<String, Object> dispatch(Actor actor, String id) {
    Map<String, Object> t = lockInStatus(id, APPROVED, "dispatched");
    transfers.dispatch(id, actor.id());
    audit.log(actor, "TRANSFER_DISPATCHED", "allocation", id);
    String operationId = (String) t.get("operation_id");
    notifications.notifySellersAt(NotificationCatalog.STOCK_IN_TRANSIT, operationId, Map.of("location", (String) operations.findById(operationId).map(o -> o.get("name")).orElse("")), "allocation", id);
    return transfers.find(id);
  }

  /** Requester or staff may cancel before dispatch; approved stock goes back to central. */
  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String reason) {
    Map<String, Object> t = transfers.lock(id).orElseThrow(() -> Validate.notFound("Transfer not found."));
    requireNotInRequest(t);
    String status = (String) t.get("status");
    if (!PENDING.equals(status) && !APPROVED.equals(status)) throw Validate.conflict("Only transfers that have not been dispatched can be cancelled.");
    if (actor.isSeller() && !actor.id().equals(t.get("created_by"))) throw Validate.forbidden("You can only cancel your own requests.");
    if (APPROVED.equals(status))
      ledger.add(null, (String) t.get("product_id"), Rows.asInt(t.get("sent_quantity")), MovementType.ALLOCATION_RELEASED,
          StockLedger.Ref.of("allocation", id), actor.id(), "Transfer cancelled before dispatch");
    transfers.close(id, CANCELLED, actor.id(), Validate.text(reason, "Reason", 500));
    audit.log(actor, "TRANSFER_CANCELLED", "allocation", id, reason);
    return transfers.find(id);
  }

  /** The recipient confirms what arrived; any shortfall needs a reason and is recorded as a discrepancy. */
  @Transactional
  public boolean confirmReceipt(Actor actor, String id, Integer quantity, String note) {
    Validate.id(id, "Allocation");
    if (quantity == null || quantity < 0) throw Validate.bad("Received quantity cannot be negative.");
    Map<String, Object> t = transfers.lock(id).orElseThrow(() -> Validate.notFound("Transfer not found."));
    String operationId = (String) t.get("operation_id"), productId = (String) t.get("product_id");
    current.requireOperationAccess(actor, operationId);
    if (!IN_TRANSIT.equals(t.get("status")))
      throw Validate.conflict(RECEIVED.equals(t.get("status")) ? "This transfer has already been confirmed." : "This transfer has not been dispatched yet.");
    int sent = Rows.asInt(t.get("sent_quantity"));
    if (quantity > sent) throw Validate.bad("Received quantity cannot exceed the " + sent + " units dispatched.");
    String reason = Validate.optional(note, "Note", 500);
    boolean discrepancy = quantity != sent;
    if (discrepancy && reason == null) throw Validate.bad("Explain the difference between dispatched and received quantities.");

    transfers.markReceived(id, quantity, actor.id());
    if (quantity > 0) ledger.add(operationId, productId, quantity, MovementType.TRANSFER_IN, StockLedger.Ref.of("allocation", id), actor.id(), null);
    if (discrepancy) inventory.insertDiscrepancy(UUID.randomUUID().toString(), id, sent, quantity, reason);
    if (t.get("request_id") != null) stockRequests.productReceived((String) t.get("request_id"));
    audit.log(actor, "ALLOCATION_CONFIRMED", "allocation", id, "received " + quantity + " of " + sent);
    return discrepancy;
  }

  /** The whole queue in one status (manager approvals view). */
  public List<Map<String, Object>> inStatus(String status) { return transfers.search(status, null); }

  public PageResponse<Map<String, Object>> search(String status, String operationId, String q, PageRequest req) {
    return transfers.page(status, operationId, q, req);
  }

  public PageResponse<Map<String, Object>> forSeller(Actor seller, String operationId, String status, PageRequest req) {
    return transfers.forSellerPage(seller.id(), operationId, status == null ? IN_TRANSIT : status, req);
  }

  private Map<String, Object> lockInStatus(String id, String expected, String verb) {
    Map<String, Object> t = transfers.lock(Validate.id(id, "Transfer")).orElseThrow(() -> Validate.notFound("Transfer not found."));
    requireNotInRequest(t);
    return Validate.status(t, "transfer", verb, expected);
  }

  /** A product of a stock request is decided with the whole request (see StockRequestService). */
  private static void requireNotInRequest(Map<String, Object> t) {
    if (t.get("request_id") != null) throw Validate.conflict("This product belongs to a stock request. Approve, reject, dispatch or cancel the request instead.");
  }

}
