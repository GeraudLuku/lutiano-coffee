-- Orders placed by customers in the Customer app. A seller at the sales point accepts, prepares and hands the order over; collecting it
-- records a normal cash sale (sale_id), so stock, commission and loyalty points follow the existing sale flow.
-- status: PLACED, ACCEPTED, READY, COLLECTED, REJECTED, CANCELLED (by the customer), EXPIRED (not handled in time).
CREATE SEQUENCE order_reference_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE customer_order (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  customer_id CHAR(36) NOT NULL,
  operation_id CHAR(36) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'PLACED',
  total_xaf BIGINT NOT NULL,
  note VARCHAR(200) NULL,
  idempotency_key VARCHAR(120) NOT NULL UNIQUE,
  payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH',
  sale_id CHAR(36) NULL,
  handled_by CHAR(36) NULL,
  reason VARCHAR(200) NULL,
  placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accepted_at TIMESTAMP NULL,
  ready_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  INDEX idx_order_customer (customer_id, placed_at, id),
  INDEX idx_order_operation (operation_id, status, placed_at)
);

CREATE TABLE customer_order_line (
  order_id CHAR(36) NOT NULL,
  line_no INT NOT NULL,
  product_id CHAR(36) NOT NULL,
  product_name VARCHAR(150) NOT NULL,
  unit_price_xaf BIGINT NOT NULL,
  addons_unit_xaf BIGINT NOT NULL DEFAULT 0,
  quantity INT NOT NULL,
  PRIMARY KEY (order_id, line_no)
);

CREATE TABLE customer_order_line_addon (
  order_id CHAR(36) NOT NULL,
  line_no INT NOT NULL,
  addon_id CHAR(36) NOT NULL,
  addon_name VARCHAR(80) NOT NULL,
  unit_price_xaf BIGINT NOT NULL,
  PRIMARY KEY (order_id, line_no, addon_id)
);
