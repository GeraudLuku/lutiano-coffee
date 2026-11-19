-- A08 Refunds: request, approval and payment are separate steps. Refunds are paid in cash, either from an open
-- cash shift (which must hold enough cash) or from a documented authorised advance. Paying a refund never
-- restores stock; returned goods go through a separate, approved stock adjustment.

-- status: REQUESTED → APPROVED → PAID, or REJECTED / CANCELLED. An approved refund with no available fund stays APPROVED (unpaid).
-- fund_source: SHIFT (cash from an open cash shift) or ADVANCE (authorised advance; unused cash returned).
CREATE TABLE refund (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  sale_id CHAR(36) NOT NULL,
  operation_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  refund_type VARCHAR(10) NOT NULL,
  reason VARCHAR(30) NOT NULL,
  note VARCHAR(500) NULL,
  customer_name VARCHAR(150) NULL,
  customer_phone VARCHAR(30) NULL,
  status VARCHAR(12) NOT NULL,
  requested_by CHAR(36) NOT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_by CHAR(36) NULL,
  decided_at TIMESTAMP NULL,
  decision_note VARCHAR(500) NULL,
  fund_source VARCHAR(10) NULL,
  shift_id CHAR(36) NULL,
  advance_reference VARCHAR(80) NULL,
  advance_amount_xaf BIGINT NULL,
  advance_returned_xaf BIGINT NULL,
  recipient_name VARCHAR(150) NULL,
  recipient_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
  paid_by CHAR(36) NULL,
  paid_at TIMESTAMP NULL,
  payment_note VARCHAR(500) NULL,
  INDEX idx_refund_sale (sale_id),
  INDEX idx_refund_status (status, requested_at),
  INDEX idx_refund_requested (requested_at)
);
CREATE SEQUENCE refund_reference_seq START WITH 1 INCREMENT BY 1;

-- Append-only history of each refund.
CREATE TABLE refund_event (
  id CHAR(36) PRIMARY KEY,
  refund_id CHAR(36) NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  from_status VARCHAR(12) NULL,
  to_status VARCHAR(12) NULL,
  actor_id CHAR(36) NULL,
  details VARCHAR(500) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_refund_event (refund_id, created_at)
);

-- Evidence files (signed slip, photo of damaged product…), kept in the private documents bucket.
CREATE TABLE refund_evidence (
  id CHAR(36) PRIMARY KEY,
  refund_id CHAR(36) NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by CHAR(36) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_refund_evidence (refund_id)
);

-- Cash refunds paid out of a shift reduce its expected cash; the total is kept for the shift summary.
ALTER TABLE cash_shift ADD COLUMN refunds_paid_xaf BIGINT NOT NULL DEFAULT 0;

-- Stock returned with a refund is a normal adjustment (reason CUSTOMER_RETURN) linked back to the refund.
ALTER TABLE stock_adjustment ADD COLUMN refund_id CHAR(36) NULL, ADD INDEX idx_adjustment_refund (refund_id);
