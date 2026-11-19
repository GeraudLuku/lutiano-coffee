-- A06 Inventory: approval workflow for transfers, stock adjustments, stocktakes, receipt sources,
-- and an append-only stock movement ledger.

-- Transfers (allocations): PENDING_APPROVAL -> APPROVED -> IN_TRANSIT -> RECEIVED, or REJECTED / CANCELLED.
-- created_by is the requester. Existing rows were dispatched immediately, so they count as approved and dispatched.
ALTER TABLE stock_allocation
  ADD COLUMN request_note VARCHAR(500) NULL,
  ADD COLUMN approved_by CHAR(36) NULL,
  ADD COLUMN approved_at TIMESTAMP NULL,
  ADD COLUMN dispatched_by CHAR(36) NULL,
  ADD COLUMN dispatched_at TIMESTAMP NULL,
  ADD COLUMN closed_by CHAR(36) NULL,
  ADD COLUMN closed_at TIMESTAMP NULL,
  ADD COLUMN decision_note VARCHAR(500) NULL;
UPDATE stock_allocation
   SET approved_by = created_by, approved_at = created_at, dispatched_by = created_by, dispatched_at = created_at;

-- Supplier receipts vs opening balances.
ALTER TABLE stock_receipt
  ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'SUPPLIER',
  ADD COLUMN supplier VARCHAR(150) NULL,
  ADD COLUMN reference VARCHAR(80) NULL,
  ADD COLUMN note VARCHAR(500) NULL;

-- Adjustments need approval before they change stock. operation_id NULL means central stock.
CREATE TABLE stock_adjustment (
  id CHAR(36) PRIMARY KEY,
  operation_id CHAR(36) NULL,
  product_id CHAR(36) NOT NULL,
  quantity_delta INT NOT NULL,
  reason VARCHAR(30) NOT NULL,
  note VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL,
  requested_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_by CHAR(36) NULL,
  decided_at TIMESTAMP NULL,
  decision_note VARCHAR(500) NULL,
  INDEX idx_adjustment_status (status, created_at)
);

-- Stocktakes: COUNTING -> SUBMITTED -> APPROVED (differences posted); rejected counts go back to COUNTING.
CREATE TABLE stocktake (
  id CHAR(36) PRIMARY KEY,
  operation_id CHAR(36) NULL,
  status VARCHAR(20) NOT NULL,
  note VARCHAR(500) NULL,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  submitted_by CHAR(36) NULL,
  submitted_at TIMESTAMP NULL,
  decided_by CHAR(36) NULL,
  decided_at TIMESTAMP NULL,
  decision_note VARCHAR(500) NULL,
  INDEX idx_stocktake_status (status, created_at)
);
CREATE TABLE stocktake_line (
  stocktake_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  expected_quantity INT NOT NULL,
  counted_quantity INT NULL,
  PRIMARY KEY (stocktake_id, product_id)
);

-- Append-only ledger: every change to a stock balance is one row. operation_id NULL means central stock.
CREATE TABLE stock_movement (
  id CHAR(36) PRIMARY KEY,
  operation_id CHAR(36) NULL,
  product_id CHAR(36) NOT NULL,
  quantity_delta INT NOT NULL,
  balance_after INT NULL,
  movement_type VARCHAR(30) NOT NULL,
  reference_type VARCHAR(30) NULL,
  reference_id CHAR(36) NULL,
  reference_label VARCHAR(60) NULL,
  actor_id CHAR(36) NULL,
  note VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_movement_location (operation_id, product_id, created_at),
  INDEX idx_movement_created (created_at)
);

-- Backfill the ledger from existing history (balance_after is unknown for past rows).
INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, reference_type, reference_id, actor_id, created_at)
SELECT UUID(), NULL, product_id, quantity, 'RECEIPT', 'receipt', id, actor_id, created_at FROM stock_receipt;

INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, reference_type, reference_id, actor_id, created_at)
SELECT UUID(), NULL, product_id, -sent_quantity, 'ALLOCATION_OUT', 'allocation', id, created_by, created_at FROM stock_allocation;

INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, reference_type, reference_id, actor_id, created_at)
SELECT UUID(), operation_id, product_id, received_quantity, 'TRANSFER_IN', 'allocation', id, confirmed_by, COALESCE(received_at, created_at)
  FROM stock_allocation WHERE status = 'RECEIVED' AND received_quantity > 0;

INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, reference_type, reference_id, reference_label, actor_id, created_at)
SELECT UUID(), s.operation_id, l.product_id, -l.quantity, 'SALE', 'sale', s.id, s.reference, s.seller_id, s.created_at
  FROM sale_line l JOIN sale s ON s.id = l.sale_id;

-- Opening balances for stock seeded before the ledger existed (V2/V3 demo data), so the ledger sums to today's balances.
INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, note, created_at)
SELECT UUID(), NULL, x.product_id, x.diff, 'OPENING_BALANCE', 'Balance before the stock ledger was introduced', '2026-01-01 00:00:00'
  FROM (SELECT c.product_id,
               c.quantity - COALESCE((SELECT SUM(m.quantity_delta) FROM stock_movement m WHERE m.operation_id IS NULL AND m.product_id = c.product_id), 0) diff
          FROM central_inventory c) x
 WHERE x.diff <> 0;

INSERT INTO stock_movement (id, operation_id, product_id, quantity_delta, movement_type, note, created_at)
SELECT UUID(), x.operation_id, x.product_id, x.diff, 'OPENING_BALANCE', 'Balance before the stock ledger was introduced', '2026-01-01 00:00:00'
  FROM (SELECT b.operation_id, b.product_id,
               b.quantity - COALESCE((SELECT SUM(m.quantity_delta) FROM stock_movement m WHERE m.operation_id = b.operation_id AND m.product_id = b.product_id), 0) diff
          FROM inventory_balance b) x
 WHERE x.diff <> 0;
