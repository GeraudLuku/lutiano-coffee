-- Open low-stock and out-of-stock alerts. A row exists only while the product is at or below its threshold at that place;
-- it is created when a stock change crosses the threshold (and an SMS goes out then), updated when stock reaches zero,
-- and deleted when stock rises above the threshold again. scope_id is the sales location id, or 'CENTRAL' for central stock.
-- level: LOW (at or below the threshold) or OUT (nothing left).
CREATE TABLE stock_alert (
  scope_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  level VARCHAR(4) NOT NULL,
  quantity INT NOT NULL,
  opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (scope_id, product_id),
  INDEX idx_stock_alert_product (product_id)
);

-- Alerts for stock that is already low today (no SMS for these).
INSERT INTO stock_alert (scope_id, product_id, level, quantity)
  SELECT 'CENTRAL', p.id, CASE WHEN COALESCE(c.quantity,0) <= 0 THEN 'OUT' ELSE 'LOW' END, COALESCE(c.quantity,0)
  FROM product p LEFT JOIN central_inventory c ON c.product_id = p.id
  WHERE p.active = TRUE AND p.kind <> 'PARENT' AND p.track_stock = TRUE AND COALESCE(c.quantity,0) <= p.low_stock_threshold;
INSERT INTO stock_alert (scope_id, product_id, level, quantity)
  SELECT o.id, p.id, CASE WHEN b.quantity <= 0 THEN 'OUT' ELSE 'LOW' END, b.quantity
  FROM inventory_balance b JOIN product p ON p.id = b.product_id JOIN operation o ON o.id = b.operation_id
  WHERE p.active = TRUE AND p.track_stock = TRUE AND o.active = TRUE AND b.quantity <= p.low_stock_threshold;
