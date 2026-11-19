-- A12 Payouts. The company pays sellers outside LUTIANO (cash, MTN MoMo, Orange Money); LUTIANO records, evidences and
-- controls those payouts. A payout is made against one finalised earning period of one seller and may be partial: what
-- is still owed on a period = its payable amount (seller_earning.payable_xaf) minus the payouts that count against it.

-- Where a seller is paid by mobile money. provider: MTN_MOMO or ORANGE_MONEY. status: PENDING (needs verification),
-- VERIFIED (usable), REJECTED, REPLACED (an older number superseded by a newer one). Only a VERIFIED number can be paid to.
-- The person who added a number cannot verify it.
CREATE TABLE payout_destination (
  id CHAR(36) PRIMARY KEY,
  seller_id CHAR(36) NOT NULL,
  provider VARCHAR(20) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  account_name VARCHAR(150) NOT NULL,
  status VARCHAR(10) NOT NULL,
  note VARCHAR(255) NULL,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  verified_by CHAR(36) NULL,
  verified_at TIMESTAMP NULL,
  INDEX idx_payout_destination_seller (seller_id, provider, status),
  INDEX idx_payout_destination_status (status, created_at)
);

-- Payouts above this amount (XAF) need approval by an administrator other than the one who recorded them.
-- 0 means every payout needs a second approver (recommended for the pilot).
CREATE TABLE payout_setting (
  id TINYINT PRIMARY KEY,
  approval_threshold_xaf BIGINT NOT NULL,
  updated_by CHAR(36) NULL,
  updated_at TIMESTAMP NULL
);
INSERT INTO payout_setting (id, approval_threshold_xaf) VALUES (1, 0);

-- method: CASH, MTN_MOMO or ORANGE_MONEY. status: PENDING_APPROVAL, APPROVED (waiting to be paid), PAID, REJECTED,
-- CANCELLED, REVERSAL_PENDING (a correction awaits a second reviewer) or REVERSED (the payout is corrected away).
-- PENDING_APPROVAL, APPROVED, PAID and REVERSAL_PENDING count against the period's balance.
-- requires_approval is decided when the payout is recorded; a reversal of such a payout also needs a second reviewer.
-- For mobile money the destination number is copied onto the payout, and the provider reference is unique per method.
CREATE TABLE payout (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  seller_id CHAR(36) NOT NULL,
  period_id CHAR(36) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  method VARCHAR(20) NOT NULL,
  destination_id CHAR(36) NULL,
  destination_phone VARCHAR(20) NULL,
  destination_name VARCHAR(150) NULL,
  provider_reference VARCHAR(80) NULL,
  status VARCHAR(20) NOT NULL,
  requires_approval BOOLEAN NOT NULL,
  note VARCHAR(500) NULL,
  requested_by CHAR(36) NOT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_by CHAR(36) NULL,
  decided_at TIMESTAMP NULL,
  decision_note VARCHAR(500) NULL,
  paid_by CHAR(36) NULL,
  paid_at TIMESTAMP NULL,
  payment_note VARCHAR(500) NULL,
  reversal_reason VARCHAR(500) NULL,
  reversal_requested_by CHAR(36) NULL,
  reversal_requested_at TIMESTAMP NULL,
  reversed_by CHAR(36) NULL,
  reversed_at TIMESTAMP NULL,
  UNIQUE KEY uq_payout_provider_reference (method, provider_reference),
  INDEX idx_payout_seller_period (seller_id, period_id, status),
  INDEX idx_payout_status (status, requested_at),
  INDEX idx_payout_requested (requested_at)
);
CREATE SEQUENCE payout_reference_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Append-only history of each payout.
CREATE TABLE payout_event (
  id CHAR(36) PRIMARY KEY,
  payout_id CHAR(36) NOT NULL,
  event_type VARCHAR(30) NOT NULL,
  from_status VARCHAR(20) NULL,
  to_status VARCHAR(20) NULL,
  actor_id CHAR(36) NULL,
  details VARCHAR(500) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_payout_event (payout_id, created_at)
);

-- Evidence of a payout (transfer screenshot, signed receipt…), kept in the private documents bucket.
CREATE TABLE payout_evidence (
  id CHAR(36) PRIMARY KEY,
  payout_id CHAR(36) NOT NULL,
  object_key VARCHAR(300) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  uploaded_by CHAR(36) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_payout_evidence (payout_id)
);
