package com.lutiano.api.service.inventory;

import java.util.Map;
import com.lutiano.api.utils.Rows;
import com.lutiano.api.service.notification.NotificationCatalog;
import com.lutiano.api.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import com.lutiano.api.repository.inventory.InventoryRepository;
import com.lutiano.api.repository.inventory.StockAlertRepository;

/**
 * Keeps the stock alerts up to date and sends the SMS. It is called whenever a balance changes (from {@link StockLedger},
 * in the same transaction) or a product's threshold or status changes. Only a change of state is acted on: stock falling to
 * the threshold opens an alert and tells people once, stock reaching zero tells them again, and stock rising back above the
 * threshold closes the alert. Further sales while stock is already low send nothing.
 */
@Service
public class StockAlerts {
  private static final String LOW = "LOW", OUT = "OUT";

  private final StockAlertRepository alerts;
  private final InventoryRepository balances;
  private final NotificationService notifications;

  public StockAlerts(StockAlertRepository alerts, InventoryRepository balances, NotificationService notifications) {
    this.alerts = alerts; this.balances = balances; this.notifications = notifications;
  }

  /** Re-evaluates one product at one place after its quantity changed. {@code operationId} is null for central stock. */
  public void check(String operationId, String productId) {
    String scope = operationId == null ? StockAlertRepository.CENTRAL : operationId;
    Map<String, Object> product = alerts.product(productId).orElse(null);
    String location = operationId == null ? "Central" : alerts.activeLocationName(operationId).orElse(null);
    int quantity = balances.quantity(operationId, productId);
    String level = product != null && Rows.truthy(product.get("watched")) && location != null ? levelOf(quantity, Rows.asInt(product.get("low_stock_threshold"))) : null;
    String current = alerts.level(scope, productId).orElse(null);
    if (level == null) { if (current != null) alerts.close(scope, productId); return; }
    if (current == null) alerts.open(scope, productId, level, quantity);
    else alerts.setLevel(scope, productId, level, quantity);
    // Tell people when stock first runs low, and again when it runs out; not while it stays at the same level or recovers a little.
    if (current == null || (LOW.equals(current) && OUT.equals(level))) tell(operationId, scope, (String) product.get("name"), location, quantity, level);
  }

  /** Re-evaluates a product (or all variants of a parent) everywhere after its threshold or status changed. */
  public void recheck(String productId) {
    for (String id : alerts.watchedIds(productId))
      for (String scope : alerts.scopesHolding(id)) check(StockAlertRepository.CENTRAL.equals(scope) ? null : scope, id);
  }

  private static String levelOf(int quantity, int threshold) { return quantity <= 0 ? OUT : quantity <= threshold ? LOW : null; }

  private void tell(String operationId, String scope, String product, String location, int quantity, String level) {
    String event = OUT.equals(level) ? NotificationCatalog.STOCK_OUT : NotificationCatalog.STOCK_LOW;
    Map<String, String> params = Map.of("product", product, "location", location, "quantity", String.valueOf(quantity));
    notifications.notifyApprovers(event, null, params, "stock_alert", scope);
    if (operationId != null) notifications.notifySellersAt(event, operationId, params, "stock_alert", scope);
  }
}
