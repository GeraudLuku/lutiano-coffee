package com.lutiano.api.service.order;

import com.lutiano.api.service.audit.AuditService;
import com.lutiano.api.service.catalogue.PublicCatalogueService;
import com.lutiano.api.models.response.PageResponse;
import com.lutiano.api.models.request.PageRequest;
import com.lutiano.api.utils.Periods;
import com.lutiano.api.repository.common.References;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.utils.Validate;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import com.lutiano.api.repository.operation.OperationRepository;
import com.lutiano.api.service.operation.QrCodeService;
import com.lutiano.api.service.payment.PaymentService;
import com.lutiano.api.service.product.AddonService;
import com.lutiano.api.repository.product.ProductRepository;
import com.lutiano.api.service.sales.SaleService;
import com.lutiano.api.security.Actor;
import com.lutiano.api.security.CurrentUser;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lutiano.api.repository.order.OrderRepository;
import com.lutiano.api.config.AppProperties;
import com.lutiano.api.models.request.common.LineItemRequest;
import com.lutiano.api.models.request.order.PlaceOrderRequest;
import com.lutiano.api.models.request.sales.SaleRequest;
import com.lutiano.api.utils.Enums;
import com.lutiano.api.definitions.enums.OrderGroup;
import com.lutiano.api.definitions.enums.OrderStatus;
import com.lutiano.api.entity.PricedAddon;

/**
 * A customer's order to a sales point. The customer places it (cash at the counter), a seller there accepts, prepares and hands it over,
 * and collecting it records a normal cash sale (stock, commission and loyalty points follow the existing sale flow). Everyone involved is
 * told by notification at each step. An order nobody accepts in time expires.
 */
@Service
public class OrderService {
  private static final Logger log = LoggerFactory.getLogger(OrderService.class);
  private static final Set<String> GROUPS = Enums.names(OrderGroup.class);
  private static final Set<String> STATUSES = Enums.names(OrderStatus.class);
  private static final int MAX_LINES = 30, MAX_QUANTITY = 50;

  public record OrderResult(Map<String, Object> order, boolean created) {}

  private record Merged(String productId, List<String> addonIds, int quantity) {}

  private final OrderRepository orders;
  private final ProductRepository products;
  private final AddonService addons;
  private final InventoryRepository inventory;
  private final OperationRepository operations;
  private final SaleService sales;
  private final NotificationService notifications;
  private final CurrentUser current;
  private final AuditService audit;
  private final References references;
  private final int acceptMinutes;

  public OrderService(OrderRepository orders, ProductRepository products, AddonService addons, InventoryRepository inventory, OperationRepository operations,
                      SaleService sales, NotificationService notifications, CurrentUser current, AuditService audit, References references,
                      AppProperties.Orders config) {
    this.orders = orders; this.products = products; this.addons = addons; this.inventory = inventory; this.operations = operations; this.sales = sales;
    this.notifications = notifications; this.current = current; this.audit = audit; this.references = references; this.acceptMinutes = config.acceptTimeoutMinutes();
  }

  // ----- the customer -----

  /** Places an order; a retry with the same key returns the order that was placed. Prices are today's, add-ons checked, stock checked. */
  @Transactional
  public OrderResult place(Actor me, String key, PlaceOrderRequest in) {
    if (key == null || key.isBlank() || key.length() > 120) throw Validate.bad("Idempotency-Key must be 1-120 characters.");
    var previous = orders.byKey(key);
    if (previous.isPresent()) {
      if (!me.id().equals(previous.get().get("customer_id"))) throw Validate.conflict("This idempotency key has already been used.");
      return new OrderResult(detail(previous.get(), true), false);
    }
    String operationId = Validate.id(in.operationId(), "Sales point");
    if (!operations.isActive(operationId)) throw Validate.conflict("This sales point is not taking orders.");
    if (!operations.accepts(operationId, PaymentService.CASH)) throw Validate.conflict("This sales point does not take cash payments.");
    if (!PublicCatalogueService.openNow(operations.hours(operationId))) throw Validate.conflict("This sales point is closed right now.");
    String note = Validate.optional(in.note(), "Note", 200);

    List<Merged> lines = merge(in.lines());
    String id = UUID.randomUUID().toString(), reference = references.dated("ORD", "order_reference_seq");
    record Priced(Merged m, String name, long unit, List<PricedAddon> addons) {}
    List<Priced> priced = new ArrayList<>();
    TreeMap<String, Integer> tracked = new TreeMap<>();
    Map<String, String> names = new LinkedHashMap<>();
    long total = 0;
    for (Merged m : lines) {
      Map<String, Object> p = products.findSellable(m.productId()).orElseThrow(() -> Validate.conflict("A product in your order is no longer available."));
      List<PricedAddon> a = addons.priceFor(m.productId(), (String) p.get("parent_id"), m.addonIds());
      long unit = Rows.asLong(p.get("price_xaf")), addonUnit = a.stream().mapToLong(PricedAddon::priceXaf).sum();
      total = Math.addExact(total, Math.multiplyExact(Math.addExact(unit, addonUnit), m.quantity()));
      priced.add(new Priced(m, (String) p.get("name"), unit, a));
      if (Boolean.TRUE.equals(p.get("track_stock"))) { tracked.merge(m.productId(), m.quantity(), Math::addExact); names.put(m.productId(), (String) p.get("name")); }
    }
    tracked.forEach((productId, qty) -> {
      if (inventory.quantity(operationId, productId) < qty) throw Validate.conflict("Not enough " + names.get(productId) + " at this sales point right now.");
    });
    orders.insert(id, reference, me.id(), operationId, total, note, key);
    int lineNo = 0;
    for (Priced p : priced) {
      orders.insertLine(id, ++lineNo, p.m().productId(), p.name(), p.unit(), p.addons().stream().mapToLong(PricedAddon::priceXaf).sum(), p.m().quantity());
      for (PricedAddon a : p.addons()) orders.insertLineAddon(id, lineNo, a.id(), a.name(), a.priceXaf());
    }
    audit.log(me, "ORDER_PLACED", "customer_order", id, reference + " " + total + " XAF");
    notifications.notifySellersAt(NotificationCatalog.ORDER_PLACED, operationId,
        Map.of("reference", reference, "customer", me.fullName(), "total", NotificationService.xaf(total)), "customer_order", id);
    return new OrderResult(detail(orders.find(id).orElseThrow(), true), true);
  }

  public PageResponse<Map<String, Object>> mine(Actor me, String group, PageRequest req) {
    return orders.ofCustomer(me.id(), group(group), req).map(o -> clean(o, true));
  }

  public Map<String, Object> mine(Actor me, String id) { return detail(own(me, id), true); }

  /** The customer withdraws an order nobody has accepted yet. */
  @Transactional
  public Map<String, Object> cancel(Actor me, String id) {
    Map<String, Object> o = own(me, id);
    if (!orders.transition(id, List.of("PLACED"), "CANCELLED", "closed_at", null, "Cancelled by the customer"))
      throw Validate.conflict("This order was already accepted or closed, so it can no longer be cancelled here. Ask the sales point.");
    audit.log(me, "ORDER_CANCELLED", "customer_order", id, (String) o.get("reference"));
    return mine(me, id);
  }

  /** The lines of an earlier order at today's prices, each saying whether it can still be ordered, to fill the cart again. */
  public List<Map<String, Object>> reorder(Actor me, String id) {
    Map<String, Object> o = own(me, id);
    List<Map<String, Object>> out = new ArrayList<>();
    for (Map<String, Object> l : orders.lines((String) o.get("id"))) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("product_id", l.get("product_id"));
      row.put("name", l.get("product_name"));
      row.put("quantity", l.get("quantity"));
      List<String> addonIds = ((List<Map<String, Object>>) l.get("addons")).stream().map(a -> (String) a.get("addon_id")).toList();
      row.put("addon_ids", addonIds);
      var p = products.findSellable((String) l.get("product_id"));
      boolean ok = p.isPresent();
      long unit = 0;
      List<PricedAddon> current = List.of();
      if (ok) {
        try {
          current = addons.priceFor((String) l.get("product_id"), (String) p.get().get("parent_id"), addonIds);
          unit = Rows.asLong(p.get().get("price_xaf")) + current.stream().mapToLong(PricedAddon::priceXaf).sum();
        } catch (RuntimeException e) { ok = false; }
      }
      row.put("available", ok);
      row.put("unit_xaf", unit);
      row.put("addons", current.stream().map(x -> Map.of("id", x.id(), "name", x.name(), "price_xaf", x.priceXaf())).toList());
      out.add(row);
    }
    return out;
  }

  /** The sale behind a collected order, for its receipt. */
  public Map<String, Object> receipt(Actor me, String id) {
    Map<String, Object> o = own(me, id);
    if (o.get("sale_id") == null) throw Validate.conflict("The receipt is available once the order is collected.");
    return sales.receiptData((String) o.get("sale_id"));
  }

  // ----- the sales point -----

  /** Orders at the seller's sales points: those still to handle first. */
  public PageResponse<Map<String, Object>> forSeller(Actor seller, String group, String status, String operationId, PageRequest req) {
    List<String> mine = operations.findActiveAssignedTo(seller.id()).stream().map(o -> (String) o.get("id")).toList();
    if (mine.isEmpty()) return PageResponse.of(List.of(), req, 0);
    if (operationId != null && !operationId.isBlank() && !mine.contains(operationId)) throw Validate.forbidden("This sales point is not assigned to you.");
    return orders.atOperations(mine, group(group), status(status), blankToNull(operationId), req).map(o -> clean(o, false));
  }

  public Map<String, Object> forSeller(Actor seller, String id) { return detail(atMyOperation(seller, id), false); }

  @Transactional
  public Map<String, Object> accept(Actor seller, String id) {
    Map<String, Object> o = atMyOperation(seller, id);
    if (!orders.transition(id, List.of("PLACED"), "ACCEPTED", "accepted_at", seller.id(), null)) throw Validate.conflict("This order was already handled.");
    tell(NotificationCatalog.ORDER_ACCEPTED, o, Map.of());
    audit.log(seller, "ORDER_ACCEPTED", "customer_order", id, (String) o.get("reference"));
    return forSeller(seller, id);
  }

  @Transactional
  public Map<String, Object> reject(Actor seller, String id, String reason) {
    Map<String, Object> o = atMyOperation(seller, id);
    String why = Validate.text(reason, "Reason", 200);
    if (!orders.transition(id, List.of("PLACED", "ACCEPTED"), "REJECTED", "closed_at", seller.id(), why)) throw Validate.conflict("This order was already handled.");
    tell(NotificationCatalog.ORDER_REJECTED, o, Map.of("reason", why));
    audit.log(seller, "ORDER_REJECTED", "customer_order", id, (String) o.get("reference"));
    return forSeller(seller, id);
  }

  @Transactional
  public Map<String, Object> ready(Actor seller, String id) {
    Map<String, Object> o = atMyOperation(seller, id);
    if (!orders.transition(id, List.of("ACCEPTED"), "READY", "ready_at", seller.id(), null)) throw Validate.conflict("Accept the order first, or it was already handled.");
    tell(NotificationCatalog.ORDER_READY, o, Map.of());
    return forSeller(seller, id);
  }

  /**
   * Hand-over: records the cash sale (stock, shift, commission and points follow), marks the order collected and thanks the customer. If the
   * sale cannot be made (no open shift, not enough stock) nothing changes and the seller is told why.
   */
  @Transactional
  public Map<String, Object> collect(Actor seller, String id, Long cashReceivedXaf) {
    Map<String, Object> o = atMyOperation(seller, id);
    if (!orders.transition(id, List.of("ACCEPTED", "READY"), "COLLECTED", "closed_at", seller.id(), null)) throw Validate.conflict("Accept the order before handing it over, or it was already handled.");
    List<LineItemRequest> lines = new ArrayList<>();
    for (Map<String, Object> l : orders.lines(id))
      lines.add(new LineItemRequest((String) l.get("product_id"), Rows.asInt(l.get("quantity")),
          ((List<Map<String, Object>>) l.get("addons")).stream().map(a -> (String) a.get("addon_id")).toList()));
    var input = new SaleRequest((String) o.get("operation_id"), lines, cashReceivedXaf, PaymentService.CASH, QrCodeService.CUSTOMER_PREFIX + orders.loyaltyToken((String) o.get("customer_id")));
    SaleService.SaleResult result = sales.complete(seller, "order:" + id, input);
    String saleId = (String) result.sale().get("id");
    orders.setSale(id, saleId, Rows.asLong(result.sale().get("total_xaf")));
    long points = orders.pointsOfSale(saleId);
    if (points > 0) tell(NotificationCatalog.ORDER_COLLECTED_POINTS, o, Map.of("points", String.valueOf(points)));
    else tell(NotificationCatalog.ORDER_COLLECTED, o, Map.of());
    return forSeller(seller, id);
  }

  // ----- administrators -----

  public PageResponse<Map<String, Object>> all(String group, String status, String operationId, PageRequest req) {
    return orders.atOperations(List.of(), group(group), status(status), blankToNull(operationId), req).map(o -> clean(o, false));
  }

  public Map<String, Object> get(String id) { return detail(orders.find(Validate.id(id, "Order")).orElseThrow(() -> Validate.notFound("Order not found.")), false, true); }

  @Transactional
  public Map<String, Object> adminCancel(Actor admin, String id, String reason) {
    Map<String, Object> o = orders.find(Validate.id(id, "Order")).orElseThrow(() -> Validate.notFound("Order not found."));
    String why = Validate.text(reason, "Reason", 200);
    if (!orders.transition(id, List.of("PLACED", "ACCEPTED", "READY"), "CANCELLED", "closed_at", admin.id(), why)) throw Validate.conflict("Only an order still in progress can be cancelled.");
    tell(NotificationCatalog.ORDER_CANCELLED, o, Map.of());
    audit.log(admin, "ORDER_CANCELLED", "customer_order", id, o.get("reference") + ": " + why);
    return get(id);
  }

  // ----- time -----

  /** Every minute: orders nobody accepted in time expire, and orders left accepted or ready from an earlier day are closed. */
  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  public void expire() {
    try {
      for (Map<String, Object> o : orders.unhandledSince(acceptMinutes)) expireOne(o);
      for (Map<String, Object> o : orders.staleBefore(Periods.startOf(Periods.today()))) expireOne(o);
    } catch (RuntimeException e) { log.error("Could not expire old orders: {}", e.toString()); }
  }

  private void expireOne(Map<String, Object> o) {
    if (orders.transition((String) o.get("id"), List.of("PLACED", "ACCEPTED", "READY"), "EXPIRED", "closed_at", null, null)) {
      Map<String, Object> full = orders.find((String) o.get("id")).orElse(o);
      tell(NotificationCatalog.ORDER_EXPIRED, full, Map.of());
    }
  }

  // ----- helpers -----

  private void tell(String event, Map<String, Object> order, Map<String, String> extra) {
    Map<String, String> params = new LinkedHashMap<>(extra);
    params.put("reference", (String) order.get("reference"));
    notifications.send(event, (String) order.get("customer_id"), params, "customer_order", (String) order.get("id"));
  }

  private Map<String, Object> own(Actor me, String id) {
    Map<String, Object> o = orders.find(Validate.id(id, "Order")).orElseThrow(() -> Validate.notFound("Order not found."));
    if (!me.id().equals(o.get("customer_id"))) throw Validate.notFound("Order not found.");
    return o;
  }

  private Map<String, Object> atMyOperation(Actor seller, String id) {
    Map<String, Object> o = orders.find(Validate.id(id, "Order")).orElseThrow(() -> Validate.notFound("Order not found."));
    if (!current.isAssigned(seller, (String) o.get("operation_id"))) throw Validate.forbidden("This order is at a sales point that is not assigned to you.");
    return o;
  }

  private Map<String, Object> detail(Map<String, Object> o, boolean customerView) { return detail(o, customerView, false); }

  private Map<String, Object> detail(Map<String, Object> o, boolean customerView, boolean admin) {
    Map<String, Object> out = clean(new LinkedHashMap<>(o), customerView);
    if (admin) { out.put("customer_phone", o.get("customer_phone")); out.put("handled_by", o.get("handled_by")); }
    out.put("lines", orders.lines((String) o.get("id")));
    List<Map<String, Object>> timeline = new ArrayList<>();
    timeline.add(step("PLACED", o.get("placed_at")));
    if (o.get("accepted_at") != null) timeline.add(step("ACCEPTED", o.get("accepted_at")));
    if (o.get("ready_at") != null) timeline.add(step("READY", o.get("ready_at")));
    if (o.get("closed_at") != null) timeline.add(step((String) o.get("status"), o.get("closed_at")));
    out.put("timeline", timeline);
    return out;
  }

  private static Map<String, Object> step(String status, Object at) { Map<String, Object> m = new LinkedHashMap<>(); m.put("status", status); m.put("at", at); return m; }

  /** What each side may see: the customer never sees staff ids; a seller never sees the customer's phone. */
  private static Map<String, Object> clean(Map<String, Object> o, boolean customerView) {
    Map<String, Object> out = new LinkedHashMap<>(o);
    out.remove("idempotency_key");
    out.remove("handled_by");
    out.remove("customer_phone");
    if (customerView) out.remove("customer_name");
    return out;
  }

  private static String group(String g) { return g == null || g.isBlank() ? null : Validate.oneOf(g, "Status group", GROUPS); }
  private static String status(String s) { return s == null || s.isBlank() ? null : Validate.oneOf(s, "Status", STATUSES); }
  private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s; }

  private static List<Merged> merge(List<LineItemRequest> lines) {
    if (lines == null || lines.isEmpty()) throw Validate.bad("Add at least one product to your order.");
    if (lines.size() > MAX_LINES) throw Validate.bad("An order can have at most " + MAX_LINES + " lines.");
    TreeMap<String, Merged> merged = new TreeMap<>();
    for (LineItemRequest l : lines) {
      String id = Validate.id(l.productId(), "Product");
      int qty = Validate.range(l.quantity(), "Each quantity", 1, MAX_QUANTITY);
      List<String> addonIds = l.addonIds() == null ? List.of() : l.addonIds().stream().distinct().sorted().toList();
      merged.merge(id + "|" + String.join(",", addonIds), new Merged(id, addonIds, qty), (a, b) -> new Merged(a.productId(), a.addonIds(), Math.min(MAX_QUANTITY, a.quantity() + b.quantity())));
    }
    return new ArrayList<>(merged.values());
  }
}
