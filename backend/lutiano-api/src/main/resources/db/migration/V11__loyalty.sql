-- A10 Loyalty & Rewards, with a minimal customer record (full Customer Management is A05).
-- Points are earned only by identified customers on completed, paid sales, after the customer confirms the association
-- (their loyalty QR token). Points live in batches that expire independently; redemptions spend the earliest-expiring
-- batches first; balances never go negative. Every change is an append-only ledger entry.

-- Minimal customer record. loyalty_token is what the customer's loyalty QR encodes (customer-controlled confirmation).
CREATE TABLE customer (
  id CHAR(36) PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  full_name VARCHAR(150) NOT NULL,
  phone VARCHAR(30) NOT NULL UNIQUE,
  phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
  status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
  loyalty_token CHAR(36) NOT NULL UNIQUE,
  tier_code VARCHAR(20) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE SEQUENCE customer_code_seq START WITH 1 INCREMENT BY 1 NOCACHE;

ALTER TABLE sale ADD COLUMN customer_id CHAR(36) NULL, ADD INDEX idx_sale_customer (customer_id, created_at);

-- Programme settings (one row). Base earning: base_points per base_amount_xaf of eligible paid value.
-- point_value_xaf: what one point is worth, used to show the liability. Expiry: months after earning.
CREATE TABLE loyalty_settings (
  id INT PRIMARY KEY,
  program_active BOOLEAN NOT NULL,
  base_points INT NOT NULL,
  base_amount_xaf BIGINT NOT NULL,
  point_value_xaf INT NOT NULL,
  expiry_months INT NOT NULL,
  expiry_notice_days INT NOT NULL,
  min_redeem_points INT NOT NULL,
  updated_by CHAR(36) NULL,
  updated_at TIMESTAMP NULL
);
INSERT INTO loyalty_settings VALUES (1, TRUE, 10, 1000, 5, 12, 30, 100, NULL, NULL);

-- Earning rules.
--   RATE  (scope PRODUCT or CATEGORY): replaces the default rate for matching lines — points per amount_xaf.
--   PER_UNIT (scope PRODUCT/CATEGORY): bonus points per unit sold.
--   MULTIPLIER (scope ALL/PRODUCT/CATEGORY): promotion; adds (multiplier_pct − 100)% of the line's base points. Promotion-eligible products only.
--   FIRST_PURCHASE (scope ALL): one-time bonus on the customer's first earning sale.
-- Base points and bonuses stack. Priority for the rate: product rule, then category rule, then the default.
CREATE TABLE earning_rule (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  method VARCHAR(20) NOT NULL,
  scope VARCHAR(10) NOT NULL,
  product_id CHAR(36) NULL,
  category VARCHAR(60) NULL,
  points INT NOT NULL DEFAULT 0,
  amount_xaf BIGINT NULL,
  multiplier_pct INT NULL,
  starts_on DATE NULL,
  ends_on DATE NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL
);

-- Member tiers by spend over the last 12 months; the multiplier (percent) applies to base points.
CREATE TABLE loyalty_tier (
  code VARCHAR(20) PRIMARY KEY,
  name VARCHAR(40) NOT NULL,
  color VARCHAR(9) NOT NULL,
  min_annual_spend_xaf BIGINT NOT NULL,
  multiplier_pct INT NOT NULL,
  benefits VARCHAR(500) NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0
);
INSERT INTO loyalty_tier VALUES
  ('BRONZE', 'Bronze', '#B87333', 0, 100, 'Earn points on all purchases; redeem rewards', TRUE, 1),
  ('SILVER', 'Silver', '#9AA4B2', 50000, 125, '25% more points; early access to promotions', TRUE, 2),
  ('GOLD', 'Gold', '#F5B800', 200000, 150, '50% more points; exclusive rewards', TRUE, 3),
  ('PLATINUM', 'Platinum', '#6B2FA3', 500000, 200, 'Double points on all purchases; VIP rewards', TRUE, 4);

-- Rewards. FREE_PRODUCT / PRODUCT_EXCHANGE / MERCHANDISE can take stock (product_id + requires_stock);
-- DISCOUNT_VOUCHER gives discount_pct. Conditions: minimum completed purchases and minimum lifetime spend.
CREATE TABLE reward (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  reward_type VARCHAR(20) NOT NULL,
  description VARCHAR(500) NULL,
  point_cost INT NOT NULL,
  product_id CHAR(36) NULL,
  requires_stock BOOLEAN NOT NULL DEFAULT FALSE,
  discount_pct INT NULL,
  min_purchases INT NOT NULL DEFAULT 0,
  min_spend_xaf BIGINT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL
);

-- Points earned together expire together. remaining goes down as points are redeemed, reversed or expired.
CREATE TABLE point_batch (
  id CHAR(36) PRIMARY KEY,
  customer_id CHAR(36) NOT NULL,
  source_type VARCHAR(12) NOT NULL,
  source_id CHAR(36) NULL,
  points INT NOT NULL,
  remaining INT NOT NULL,
  earned_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  INDEX idx_point_batch_customer (customer_id, expires_at),
  INDEX idx_point_batch_expiry (expires_at, remaining)
);

-- Append-only ledger. entry_type: EARNED, REDEEMED, REDEMPTION_REVERSED, REFUND_REVERSAL, EXPIRED, ADJUSTED.
CREATE TABLE loyalty_ledger (
  id CHAR(36) PRIMARY KEY,
  customer_id CHAR(36) NOT NULL,
  entry_type VARCHAR(20) NOT NULL,
  points INT NOT NULL,
  balance_after INT NOT NULL,
  source_type VARCHAR(12) NULL,
  source_id CHAR(36) NULL,
  reference VARCHAR(30) NULL,
  description VARCHAR(255) NULL,
  actor_id CHAR(36) NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_loyalty_ledger_customer (customer_id, created_at),
  INDEX idx_loyalty_ledger_created (created_at)
);

-- How a sale's points were calculated (snapshot of the rules applied).
CREATE TABLE loyalty_award (
  sale_id CHAR(36) PRIMARY KEY,
  customer_id CHAR(36) NOT NULL,
  eligible_xaf BIGINT NOT NULL,
  base_points INT NOT NULL,
  tier_code VARCHAR(20) NULL,
  tier_multiplier_pct INT NOT NULL,
  bonus_points INT NOT NULL,
  total_points INT NOT NULL,
  reversed_points INT NOT NULL DEFAULT 0,
  breakdown VARCHAR(1000) NULL,
  batch_id CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Status: COMPLETED or REVERSED. Stock rewards record where the item was taken from.
CREATE TABLE reward_redemption (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  customer_id CHAR(36) NOT NULL,
  reward_id CHAR(36) NOT NULL,
  reward_name VARCHAR(100) NOT NULL,
  points INT NOT NULL,
  operation_id CHAR(36) NULL,
  status VARCHAR(12) NOT NULL,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reversed_by CHAR(36) NULL,
  reversed_at TIMESTAMP NULL,
  reversal_reason VARCHAR(500) NULL,
  INDEX idx_redemption_customer (customer_id, created_at),
  INDEX idx_redemption_created (created_at)
);
CREATE SEQUENCE redemption_seq START WITH 1 INCREMENT BY 1 NOCACHE;

-- Which batches each redemption spent, so a reversal can give the same points back.
CREATE TABLE redemption_batch (
  redemption_id CHAR(36) NOT NULL,
  batch_id CHAR(36) NOT NULL,
  points INT NOT NULL,
  PRIMARY KEY (redemption_id, batch_id)
);

-- Manual corrections (goodwill, duplicate points, fraud…), always with a reason.
CREATE TABLE points_adjustment (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  customer_id CHAR(36) NOT NULL,
  points INT NOT NULL,
  reason VARCHAR(30) NOT NULL,
  note VARCHAR(500) NULL,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_points_adjustment_created (created_at)
);
CREATE SEQUENCE points_adjustment_seq START WITH 1 INCREMENT BY 1 NOCACHE;
