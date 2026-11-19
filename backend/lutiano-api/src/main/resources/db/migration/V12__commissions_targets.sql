-- A11 Commissions & Targets. Commission is configured per product and seller role (fixed XAF per unit or a percentage
-- of the eligible paid line value) and snapshotted per sale line when the sale completes. Targets are per seller.
-- Earnings are grouped into monthly periods; finalising a period freezes each seller's statement. Refunds after that
-- create adjustments in the current period; finalised statements are never rewritten.

-- A rule applies to a product (a parent's rule covers its variants) for one seller role. Changing a rate creates a new
-- version; the previous one gets superseded_at, so history is kept and only one current rule exists per pair.
-- basis: FIXED_PER_UNIT (rate = XAF per unit) or PERCENT (rate = basis points, 500 = 5.00%).
CREATE TABLE commission_rule (
  id CHAR(36) PRIMARY KEY,
  product_id CHAR(36) NOT NULL,
  seller_role VARCHAR(30) NOT NULL,
  basis VARCHAR(15) NOT NULL,
  rate INT NOT NULL,
  note VARCHAR(255) NULL,
  created_by CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  superseded_at TIMESTAMP NULL,
  superseded_by CHAR(36) NULL,
  INDEX idx_commission_rule_current (product_id, seller_role, superseded_at)
);

-- Commission per sale line, with the rule snapshot used.
CREATE TABLE sale_commission (
  sale_id CHAR(36) NOT NULL,
  line_no INT NOT NULL,
  seller_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  rule_id CHAR(36) NOT NULL,
  basis VARCHAR(15) NOT NULL,
  rate INT NOT NULL,
  quantity INT NOT NULL,
  line_value_xaf BIGINT NOT NULL,
  commission_xaf BIGINT NOT NULL,
  reversed_xaf BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (sale_id, line_no),
  INDEX idx_sale_commission_seller (seller_id, created_at)
);

-- Individual seller targets. metric: VALUE (paid XAF) or QUANTITY (units, of one product or all).
-- period_type: DAILY, WEEKLY, MONTHLY or CUSTOM; starts_on/ends_on are Douala business days (inclusive).
-- bonus_type: FIXED (bonus_xaf once) or PER_UNIT (bonus_xaf for each unit sold in the period), paid only when reached.
-- status: ACTIVE or CANCELLED. bonus_paid_xaf: the bonus frozen when its earning period was finalised;
-- bonus_reversed: a later refund took that bonus back.
CREATE TABLE seller_target (
  id CHAR(36) PRIMARY KEY,
  seller_id CHAR(36) NOT NULL,
  name VARCHAR(100) NOT NULL,
  metric VARCHAR(10) NOT NULL,
  product_id CHAR(36) NULL,
  period_type VARCHAR(10) NOT NULL,
  starts_on DATE NOT NULL,
  ends_on DATE NOT NULL,
  target_value BIGINT NOT NULL,
  bonus_type VARCHAR(10) NOT NULL,
  bonus_xaf BIGINT NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE',
  bonus_paid_xaf BIGINT NULL,
  bonus_reversed BOOLEAN NOT NULL DEFAULT FALSE,
  created_by CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_seller_target (seller_id, ends_on)
);

-- Monthly earning periods (Douala calendar months). status: OPEN or FINALIZED.
CREATE TABLE earning_period (
  id CHAR(36) PRIMARY KEY,
  starts_on DATE NOT NULL UNIQUE,
  ends_on DATE NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
  finalized_by CHAR(36) NULL,
  finalized_at TIMESTAMP NULL
);

-- Changes to earnings outside sale commissions, posted in the period of their date.
-- type: REFUND_REVERSAL (commission of a refunded share), BONUS_REVERSAL, RECOVERY (negative balance carried from the
-- previous period) or MANUAL (with a reason).
CREATE TABLE earning_adjustment (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  seller_id CHAR(36) NOT NULL,
  adjustment_type VARCHAR(20) NOT NULL,
  amount_xaf BIGINT NOT NULL,
  source_type VARCHAR(12) NULL,
  source_id CHAR(36) NULL,
  note VARCHAR(500) NULL,
  created_by CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_earning_adjustment_seller (seller_id, created_at),
  INDEX idx_earning_adjustment_created (created_at)
);
CREATE SEQUENCE earning_adjustment_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- A seller's frozen statement for a finalised period. payable_xaf = max(total, 0); a negative total is carried into
-- the next period as a RECOVERY adjustment.
CREATE TABLE seller_earning (
  period_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  sales_count INT NOT NULL,
  sales_xaf BIGINT NOT NULL,
  commission_xaf BIGINT NOT NULL,
  bonus_xaf BIGINT NOT NULL,
  adjustments_xaf BIGINT NOT NULL,
  total_xaf BIGINT NOT NULL,
  payable_xaf BIGINT NOT NULL,
  carried_xaf BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (period_id, seller_id)
);
