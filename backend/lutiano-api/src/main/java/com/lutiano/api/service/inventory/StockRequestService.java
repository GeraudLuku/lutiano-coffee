package com.lutiano.api.service.inventory;

import static com.lutiano.api.service.inventory.TransferService.APPROVED;
import static com.lutiano.api.service.inventory.TransferService.CANCELLED;
import static com.lutiano.api.service.inventory.TransferService.IN_TRANSIT;
import static com.lutiano.api.service.inventory.TransferService.PENDING;
import static com.lutiano.api.service.inventory.TransferService.REJECTED;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.service.product.ProductService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.repository.security.ActorRepository;
import com.lutiano.api.security.CurrentUser;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.repository.inventory.StockRequestRepository;
import com.lutiano.api.repository.inventory.TransferRepository;
import com.lutiano.api.models.request.inventory.StockRequestItemRequest;
import com.lutiano.api.definitions.enums.MovementType;
import com.lutiano.api.definitions.enums.SellerRole;

/**
 * A stock request asks for several products for one sales location and is decided as a whole: approved (central stock for
 * every product is set aside, or nothing is), rejected, dispatched or cancelled together. Receipt is confirmed per product
 * (the recipient counts each delivery); the request is complete when every product has been confirmed.
 */
@Service
public class StockRequestService {
  /** Seller roles that may ask for stock; a fixed point of sale is restocked by its manager. */
  static final Set<String> REQUESTING_SELLER_ROLES = Set.of(SellerRole.MOBILE_BARISTA.name(), SellerRole.MARKET_AGENT.name());
  static final int MAX_ITEMS = 50, MAX_QUANTITY = 10_000, MAX_DAYS_AHEAD = 90;

  public record Input(String operationId, List<StockRequestItemRequest> items, LocalDate neededBy, String reason, String idempotencyKey) {}

  private final StockRequestRepository requests;
  private final TransferRepository transfers;
  private final InventoryRepository inventory;
  private final OperationRepository operations;
  private final ProductService products;
  private final StockLedger ledger;
  private final ApprovalPolicy policy;
  private final CurrentUser current;
  private final ActorRepository actors;
  private final AuditService audit;
  private final NotificationService notifications;
  private final References references;

  public StockRequestService(StockRequestRepository requests, TransferRepository transfers, InventoryRepository inventory, OperationRepository operations,
                             ProductService products, StockLedger ledger, ApprovalPolicy policy, CurrentUser current, ActorRepository actors,
                             AuditService audit, NotificationService notifications, References references) {
    this.requests = requests; this.transfers = transfers; this.inventory = inventory; this.operations = operations; this.products = products;
    this.ledger = ledger; this.policy = policy; this.current = current; this.actors = actors; this.audit = audit;
    this.notifications = notifications; this.references = references;
  }

  /** Staff, or a seller whose role may request stock, asks for products for one of their own operations. Nothing moves until approved. */
  @Transactional
  public Map<String, Object> create(Actor actor, Input in) {
    if (actor.isSeller() && !REQUESTING_SELLER_ROLES.contains(actors.sellerRole(actor.id())))
      throw Validate.forbidden("Your role cannot request stock. Ask your inventory manager.");
    String operationId = Validate.id(in.operationId(), "Sales location");
    current.requireOperationAccess(actor, operationId);
    if (in.items() == null || in.items().isEmpty()) throw Validate.bad("Choose at least one product.");
    if (in.items().size() > MAX_ITEMS) throw Validate.bad("A request can have at most " + MAX_ITEMS + " products.");
    String key = Validate.optional(in.idempotencyKey(), "Idempotency key", 64);
    var existing = requests.idByKey(actor.id(), key);
    if (existing.isPresent()) return detail(actor, existing.get());   // the same request sent again (a retry after a lost answer)
    LocalDate today = Periods.today();
    if (in.neededBy() != null && (in.neededBy().isBefore(today) || in.neededBy().isAfter(today.plusDays(MAX_DAYS_AHEAD))))
      throw Validate.bad("The requested delivery date must be within the next " + MAX_DAYS_AHEAD + " days.");
    String reason = Validate.optional(in.reason(), "Reason", 500);
    if (!operations.isActive(operationId)) throw Validate.conflict("Stock can only be sent to an active sales location.");

    Set<String> seen = new HashSet<>();
    List<Map<String, Object>> lines = new ArrayList<>();
    for (StockRequestItemRequest item : in.items()) {
      String productId = Validate.id(item.productId(), "Product");
      if (!seen.add(productId)) throw Validate.bad("Each product can only appear once in a request.");
      int qty = Validate.range(item.quantity(), "Quantity", 1, MAX_QUANTITY);
      Map<String, Object> product = products.requireStockItem(productId);
      if (!Boolean.TRUE.equals(product.get("active"))) throw Validate.conflict(product.get("name") + " is inactive and cannot be requested.");
      lines.add(Map.of("productId", productId, "quantity", qty, "name", product.get("name")));
    }

    String id = UUID.randomUUID().toString(), reference = references.yearly("SR", "stock_request_seq");
    requests.insert(id, reference, operationId, actor.id(), key, in.neededBy(), reason);
    int units = 0;
    for (Map<String, Object> line : lines) {
      transfers.insertRequest(UUID.randomUUID().toString(), id, operationId, (String) line.get("productId"), (Integer) line.get("quantity"), actor.id(), reason);
      units += (Integer) line.get("quantity");
    }
    audit.log(actor, "STOCK_REQUESTED", "stock_request", id, reference + ": " + lines.size() + " products, " + units + " units");
    notifications.notifyApprovers(NotificationCatalog.APPROVAL_STOCK, actor.id(),
        Map.of("what", reference + " (" + lines.size() + " products, " + units + " units)", "by", actor.fullName()), "stock_request", id);
    return detail(actor, id);
  }

  /** Approval sets central stock aside for every product, so it cannot be promised twice. If any product is short, nothing is approved. */
  @Transactional
  public Map<String, Object> approve(Actor actor, String id, String note) {
    Map<String, Object> r = lockInStatus(id, PENDING, "approved");
    policy.requireDifferentApprover(actor, r.get("requested_by"), "a stock request");
    if (!operations.isActive((String) r.get("operation_id"))) throw Validate.conflict("The destination sales location is inactive.");
    List<Map<String, Object>> lines = transfers.lockOfRequest(id);
    List<String> short_ = new ArrayList<>();
    for (Map<String, Object> line : lines) {
      int available = inventory.quantity(null, (String) line.get("product_id")), wanted = Rows.asInt(line.get("sent_quantity"));
      if (available < wanted) short_.add(products.get((String) line.get("product_id")).get("name") + " (" + available + " available, " + wanted + " requested)");
    }
    if (!short_.isEmpty()) throw Validate.conflict("Not enough central stock: " + String.join("; ", short_) + ".");
    String decision = Validate.optional(note, "Note", 500);
    for (Map<String, Object> line : lines) {
      String lineId = (String) line.get("id"), productId = (String) line.get("product_id");
      if (!ledger.take(null, productId, Rows.asInt(line.get("sent_quantity")), MovementType.ALLOCATION_OUT, StockLedger.Ref.of("allocation", lineId), actor.id(), null))
        throw Validate.conflict("Central stock changed while approving. Try again.");
      transfers.approve(lineId, actor.id(), decision);
    }
    requests.approve(id, actor.id(), decision);
    audit.log(actor, "STOCK_REQUEST_APPROVED", "stock_request", id, (String) r.get("reference"));
    notifyRequester(NotificationCatalog.STOCK_REQUEST_APPROVED, r, Map.of("reference", (String) r.get("reference")));
    return detail(actor, id);
  }

  @Transactional
  public Map<String, Object> reject(Actor actor, String id, String reason) {
    Map<String, Object> r = lockInStatus(id, PENDING, "rejected");
    String why = Validate.text(reason, "Reason", 500);
    for (Map<String, Object> line : transfers.lockOfRequest(id)) transfers.close((String) line.get("id"), REJECTED, actor.id(), why);
    requests.close(id, REJECTED, actor.id(), why);
    audit.log(actor, "STOCK_REQUEST_REJECTED", "stock_request", id, r.get("reference") + ": " + why);
    notifyRequester(NotificationCatalog.STOCK_REQUEST_REJECTED, r, Map.of("reference", (String) r.get("reference"), "reason", why));
    return detail(actor, id);
  }

  @Transactional
  public Map<String, Object> dispatch(Actor actor, String id) {
    Map<String, Object> r = lockInStatus(id, APPROVED, "dispatched");
    for (Map<String, Object> line : transfers.lockOfRequest(id)) transfers.dispatch((String) line.get("id"), actor.id());
    requests.dispatch(id);
    audit.log(actor, "STOCK_REQUEST_DISPATCHED", "stock_request", id, (String) r.get("reference"));
    String operationId = (String) r.get("operation_id");
    notifications.notifySellersAt(NotificationCatalog.STOCK_IN_TRANSIT, operationId,
        Map.of("location", (String) operations.findById(operationId).map(o -> o.get("name")).orElse("")), "stock_request", id);
    return detail(actor, id);
  }

  /** The requester or staff may cancel before dispatch; approved stock goes back to central. */
  @Transactional
  public Map<String, Object> cancel(Actor actor, String id, String reason) {
    Map<String, Object> r = requests.lock(Validate.id(id, "Request")).orElseThrow(() -> Validate.notFound("Request not found."));
    String status = (String) r.get("status");
    if (!PENDING.equals(status) && !APPROVED.equals(status)) throw Validate.conflict("Only requests that have not been dispatched can be cancelled.");
    if (actor.isSeller() && !actor.id().equals(r.get("requested_by"))) throw Validate.forbidden("You can only cancel your own requests.");
    String why = Validate.text(reason, "Reason", 500);
    for (Map<String, Object> line : transfers.lockOfRequest(id)) {
      if (APPROVED.equals(status))
        ledger.add(null, (String) line.get("product_id"), Rows.asInt(line.get("sent_quantity")), MovementType.ALLOCATION_RELEASED,
            StockLedger.Ref.of("allocation", (String) line.get("id")), actor.id(), "Request cancelled before dispatch");
      transfers.close((String) line.get("id"), CANCELLED, actor.id(), why);
    }
    requests.close(id, CANCELLED, actor.id(), why);
    audit.log(actor, "STOCK_REQUEST_CANCELLED", "stock_request", id, r.get("reference") + ": " + why);
    return detail(actor, id);
  }

  /** Called when one product of a dispatched request has been received: the request is complete once none is still on its way. */
  void productReceived(String requestId) {
    var r = requests.lock(requestId);
    if (r.isPresent() && IN_TRANSIT.equals(r.get().get("status")) && transfers.inTransitOfRequest(requestId) == 0) requests.complete(requestId);
  }

  /** One request with its products. A seller sees only their own. */
  public Map<String, Object> detail(Actor actor, String id) {
    Map<String, Object> r = requests.find(Validate.id(id, "Request")).orElseThrow(() -> Validate.notFound("Request not found."));
    if (actor.isSeller() && !actor.id().equals(r.get("requested_by"))) throw Validate.notFound("Request not found.");
    return requests.withItems(r, transfers.ofRequest(id));
  }

  /** The signed-in seller's own requests at one of their operations, with counts per status for the overview. */
  public PageResponse<Map<String, Object>> mine(Actor seller, String operationId, String status, String reference, PageRequest req) {
    current.requireOperationAccess(seller, Validate.id(operationId, "Sales location"));
    return requests.forSeller(seller.id(), operationId, validStatus(status), reference, req);
  }

  /** Staff: every request. */
  public PageResponse<Map<String, Object>> search(String status, String operationId, String reference, PageRequest req) {
    return requests.page(validStatus(status), operationId, reference, req);
  }

  public List<Map<String, Object>> inStatus(String status) { return requests.inStatus(status); }

  private static String validStatus(String status) {
    return status == null || status.isBlank() ? null
        : Validate.oneOf(status, "Status", Set.of(PENDING, APPROVED, IN_TRANSIT, TransferService.RECEIVED, REJECTED, CANCELLED));
  }

  private Map<String, Object> lockInStatus(String id, String expected, String verb) {
    return Validate.status(requests.lock(Validate.id(id, "Request")).orElseThrow(() -> Validate.notFound("Request not found.")), "request", verb, expected);
  }

  private void notifyRequester(String event, Map<String, Object> request, Map<String, String> params) {
    notifications.send(event, (String) request.get("requested_by"), params, "stock_request", (String) request.get("id"));
  }
}
