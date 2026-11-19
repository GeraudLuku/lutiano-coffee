-- A07 Payments & Reconciliation: payments are recorded separately from sales, with an event history.
-- Only cash is available; Mobile Money methods are configured but unavailable until their integration exists.

-- Payment methods. `available` = an integration exists; `enabled` = offered to sellers. A method can only be enabled if available.
CREATE TABLE payment_method (
  code VARCHAR(20) PRIMARY KEY,
  label VARCHAR(60) NOT NULL,
  provider VARCHAR(60) NULL,
  available BOOLEAN NOT NULL,
  enabled BOOLEAN NOT NULL,
  requires_reference BOOLEAN NOT NULL,
  merchant_account VARCHAR(60) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  note VARCHAR(255) NULL,
  updated_at TIMESTAMP NULL
);
INSERT INTO payment_method (code, label, provider, available, enabled, requires_reference, sort_order, note) VALUES
  ('CASH', 'Cash', NULL, TRUE, TRUE, FALSE, 1, 'Seller confirms receipt; recorded in the cash shift.'),
  ('MTN_MOMO', 'MTN Mobile Money', 'MTN Cameroon', FALSE, FALSE, TRUE, 2, 'Needs merchant onboarding and API access before it can be enabled.'),
  ('ORANGE_MONEY', 'Orange Money', 'Orange Cameroon', FALSE, FALSE, TRUE, 3, 'Needs merchant onboarding and API access before it can be enabled.');

-- One payment per sale for now. status: PENDING, CONFIRMED, FAILED, CANCELLED.
-- provider_reference is unique per method, so a confirmed provider reference can never be reused.
CREATE TABLE payment (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  sale_id CHAR(36) NOT NULL,
  operation_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  shift_id CHAR(36) NULL,
  method VARCHAR(20) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  currency CHAR(3) NOT NULL DEFAULT 'XAF',
  status VARCHAR(12) NOT NULL,
  provider_reference VARCHAR(80) NULL,
  payer_account VARCHAR(40) NULL,
  merchant_account VARCHAR(60) NULL,
  cash_received_xaf BIGINT NULL,
  change_xaf BIGINT NULL,
  confirmed_by CHAR(36) NULL,
  confirmed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_payment_provider_reference (method, provider_reference),
  INDEX idx_payment_sale (sale_id),
  INDEX idx_payment_created (created_at),
  INDEX idx_payment_status (status, created_at)
);

-- Append-only history of each payment: created, confirmed, failed…
CREATE TABLE payment_event (
  id CHAR(36) PRIMARY KEY,
  payment_id CHAR(36) NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  from_status VARCHAR(12) NULL,
  to_status VARCHAR(12) NULL,
  source VARCHAR(12) NOT NULL,
  actor_id CHAR(36) NULL,
  details VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_payment_event (payment_id, created_at)
);

-- Every existing sale was paid in cash and confirmed by its seller at the time of sale.
INSERT INTO payment (id, reference, sale_id, operation_id, seller_id, shift_id, method, amount_xaf, status, cash_received_xaf, change_xaf,
                     confirmed_by, confirmed_at, created_at)
SELECT UUID(), CONCAT('PAY-', SUBSTRING(s.reference, 4)), s.id, s.operation_id, s.seller_id, s.shift_id, 'CASH', s.total_xaf, 'CONFIRMED',
       s.cash_received_xaf, CASE WHEN s.cash_received_xaf IS NULL THEN NULL ELSE s.cash_received_xaf - s.total_xaf END,
       s.seller_id, s.created_at, s.created_at
  FROM sale s;

INSERT INTO payment_event (id, payment_id, event_type, from_status, to_status, source, actor_id, details, created_at)
SELECT UUID(), p.id, 'CASH_CONFIRMED', NULL, 'CONFIRMED', 'SELLER', p.seller_id, 'Recorded before payments were tracked separately', p.created_at
  FROM payment p;
