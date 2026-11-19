-- A09 Cash Management. Expected cash = opening float + cash sales − cash refunds paid − approved outflows − remittances.
-- Every movement is an append-only ledger entry linked to its shift, actor and source; cash_shift.expected_cash is
-- the running balance of that ledger. Closing with a variance needs manager review; every count is kept.

-- review_status: NOT_REQUIRED (counted = expected), PENDING_REVIEW, RECOUNT_REQUESTED, RESOLVED. NULL while open.
ALTER TABLE cash_shift
  ADD COLUMN review_status VARCHAR(20) NULL,
  ADD COLUMN reviewed_by CHAR(36) NULL,
  ADD COLUMN reviewed_at TIMESTAMP NULL,
  ADD COLUMN review_note VARCHAR(500) NULL;
UPDATE cash_shift SET review_status = CASE WHEN counted_cash = expected_cash THEN 'NOT_REQUIRED' ELSE 'PENDING_REVIEW' END WHERE status = 'CLOSED';

-- entry_type: OPENING_FLOAT, CASH_SALE, REFUND_PAID, OUTFLOW, REMITTANCE. amount_xaf is signed.
CREATE TABLE cash_ledger_entry (
  id CHAR(36) PRIMARY KEY,
  shift_id CHAR(36) NOT NULL,
  seq INT NOT NULL,
  entry_type VARCHAR(20) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  balance_after_xaf BIGINT NOT NULL,
  source_type VARCHAR(20) NULL,
  source_id CHAR(36) NULL,
  actor_id CHAR(36) NULL,
  note VARCHAR(255) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uq_cash_ledger_seq (shift_id, seq),
  INDEX idx_cash_ledger_source (source_type, source_id)
);

-- Backfill the ledger of existing shifts from their float, cash sales and cash refunds paid from the drawer.
INSERT INTO cash_ledger_entry (id, shift_id, seq, entry_type, amount_xaf, balance_after_xaf, source_type, source_id, actor_id, note, created_at)
SELECT UUID(), m.shift_id,
       ROW_NUMBER() OVER (PARTITION BY m.shift_id ORDER BY m.at, m.ord, m.source_id),
       m.entry_type, m.amount,
       SUM(m.amount) OVER (PARTITION BY m.shift_id ORDER BY m.at, m.ord, m.source_id ROWS UNBOUNDED PRECEDING),
       m.source_type, m.source_id, m.actor_id, 'Recorded before the cash ledger existed', m.at
  FROM (
    SELECT c.id shift_id, c.opened_at at, 0 ord, 'OPENING_FLOAT' entry_type, c.opening_float amount, 'SHIFT' source_type, c.id source_id, c.seller_id actor_id
      FROM cash_shift c
    UNION ALL
    SELECT s.shift_id, s.created_at, 1, 'CASH_SALE', s.total_xaf, 'SALE', s.id, s.seller_id
      FROM sale s WHERE s.payment_method = 'CASH'
    UNION ALL
    SELECT r.shift_id, r.paid_at, 2, 'REFUND_PAID', -r.amount_xaf, 'REFUND', r.id, r.paid_by
      FROM refund r WHERE r.status = 'PAID' AND r.fund_source = 'SHIFT'
  ) m;

-- Every count of a closed shift: the first count at closing (INITIAL) and any recount a manager asks for (RECOUNT).
CREATE TABLE cash_count (
  id CHAR(36) PRIMARY KEY,
  shift_id CHAR(36) NOT NULL,
  kind VARCHAR(10) NOT NULL,
  expected_xaf BIGINT NOT NULL,
  counted_xaf BIGINT NOT NULL,
  variance_xaf BIGINT NOT NULL,
  reason VARCHAR(500) NULL,
  counted_by CHAR(36) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_cash_count_shift (shift_id, created_at)
);
INSERT INTO cash_count (id, shift_id, kind, expected_xaf, counted_xaf, variance_xaf, reason, counted_by, created_at)
SELECT UUID(), id, 'INITIAL', expected_cash, counted_cash, counted_cash - expected_cash, variance_reason, seller_id, closed_at
  FROM cash_shift WHERE status = 'CLOSED' AND counted_cash IS NOT NULL;

-- Cash taken from the drawer for an approved business expense. Cash leaves (and the ledger posts) only on approval.
-- status: REQUESTED, APPROVED, REJECTED, CANCELLED.
CREATE TABLE cash_outflow (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  shift_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  description VARCHAR(500) NOT NULL,
  status VARCHAR(12) NOT NULL,
  requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decided_by CHAR(36) NULL,
  decided_at TIMESTAMP NULL,
  decision_note VARCHAR(500) NULL,
  INDEX idx_cash_outflow_status (status, requested_at),
  INDEX idx_cash_outflow_shift (shift_id)
);
CREATE SEQUENCE cash_outflow_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Cash handed to a named staff member (HANDOVER) or deposited at a bank (BANK_DEPOSIT). It leaves the drawer when recorded
-- and stays OUTSTANDING until the recipient acknowledges what they received (ACKNOWLEDGED, or DISPUTED if it differs).
CREATE TABLE cash_remittance (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  shift_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  method VARCHAR(15) NOT NULL,
  recipient_id CHAR(36) NULL,
  bank_reference VARCHAR(80) NULL,
  note VARCHAR(500) NULL,
  status VARCHAR(12) NOT NULL,
  recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  acknowledged_by CHAR(36) NULL,
  acknowledged_at TIMESTAMP NULL,
  received_xaf BIGINT NULL,
  acknowledgement_note VARCHAR(500) NULL,
  INDEX idx_cash_remittance_status (status, recorded_at),
  INDEX idx_cash_remittance_shift (shift_id),
  INDEX idx_cash_remittance_recipient (recipient_id, status)
);
CREATE SEQUENCE cash_remittance_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Reference numbers (SL-, RF-, CO-, RM-) must not jump: MariaDB caches 1000 sequence values per restart by default.
ALTER SEQUENCE sale_reference_seq NOCACHE;
ALTER SEQUENCE refund_reference_seq NOCACHE;
