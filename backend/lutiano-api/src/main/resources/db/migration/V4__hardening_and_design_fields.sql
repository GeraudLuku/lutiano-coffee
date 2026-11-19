-- Replace plain-text demo passwords with bcrypt hashes (same demo password: Coffee123!).
UPDATE staff_user
   SET password_hash = '{bcrypt}$2y$10$LbFxsrWNzXlAqdMVmgWX9eV51uIvIppti0oLBNvcHTLZlqiicV6Ju'
 WHERE password_hash = '{noop}Coffee123!';

ALTER TABLE staff_user
  ADD COLUMN phone VARCHAR(30) NULL,
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN last_login_at TIMESTAMP NULL;

-- Sales locations: type and address, as shown on the "Add Sales Location" design.
ALTER TABLE operation
  ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'FIXED_POS',
  ADD COLUMN address VARCHAR(255) NULL,
  ADD COLUMN city VARCHAR(80) NULL,
  ADD COLUMN region VARCHAR(80) NULL,
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE operation SET address = 'Bonapriso, Douala', city = 'Douala', region = 'Littoral'
 WHERE id = '10000000-0000-4000-8000-000000000001';
UPDATE operation SET type = 'MOBILE', address = 'Akwa, Douala', city = 'Douala', region = 'Littoral'
 WHERE id = '10000000-0000-4000-8000-000000000002';

-- Product catalogue fields from the product form design.
ALTER TABLE product
  ADD COLUMN category VARCHAR(60) NOT NULL DEFAULT 'Beverage',
  ADD COLUMN description VARCHAR(500) NULL,
  ADD COLUMN low_stock_threshold INT NOT NULL DEFAULT 10,
  ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
UPDATE product SET category = 'Coffee Beans' WHERE id = '30000000-0000-4000-8000-000000000003';

-- Stock transfer traceability.
ALTER TABLE stock_allocation
  ADD COLUMN created_by CHAR(36) NULL,
  ADD COLUMN confirmed_by CHAR(36) NULL,
  ADD COLUMN received_at TIMESTAMP NULL;
ALTER TABLE stock_discrepancy ADD COLUMN note VARCHAR(500) NULL;

-- Human-readable sale reference, payment method and cash tendered.
ALTER TABLE sale
  ADD COLUMN reference VARCHAR(30) NULL,
  ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CASH',
  ADD COLUMN cash_received_xaf BIGINT NULL;
UPDATE sale SET reference = CONCAT('SL-', DATE_FORMAT(created_at, '%Y%m%d'), '-L', UPPER(LEFT(id, 6)))
 WHERE reference IS NULL;
ALTER TABLE sale MODIFY reference VARCHAR(30) NOT NULL;
ALTER TABLE sale ADD UNIQUE KEY uq_sale_reference (reference);
CREATE SEQUENCE sale_reference_seq START WITH 1 INCREMENT BY 1;

CREATE INDEX idx_sale_created ON sale (created_at);
CREATE INDEX idx_sale_seller ON sale (seller_id, created_at);
CREATE INDEX idx_sale_operation ON sale (operation_id, created_at);
CREATE INDEX idx_audit_created ON audit_event (created_at);
CREATE INDEX idx_allocation_status ON stock_allocation (status, operation_id);
