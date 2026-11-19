-- Sales campaigns: for a period (and optionally one sales location) every seller's net sales are measured against tiers; the highest tier
-- reached pays its bonus, once, when an administrator settles the campaign after it ends. status: ACTIVE or CANCELLED.
CREATE TABLE campaign (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description VARCHAR(500) NULL,
  starts_on DATE NOT NULL,
  ends_on DATE NOT NULL,
  operation_id CHAR(36) NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'ACTIVE',
  settled_at TIMESTAMP NULL,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_campaign_dates (starts_on, ends_on, id)
);

-- One row per tier: reaching threshold_xaf of net sales earns bonus_xaf (the highest tier reached is paid, tiers do not add up).
CREATE TABLE campaign_tier (
  campaign_id CHAR(36) NOT NULL,
  threshold_xaf BIGINT NOT NULL,
  bonus_xaf BIGINT NOT NULL,
  PRIMARY KEY (campaign_id, threshold_xaf)
);

-- What a seller earned from a settled campaign; the amount is posted to their earnings as a CAMPAIGN adjustment.
CREATE TABLE campaign_bonus (
  campaign_id CHAR(36) NOT NULL,
  seller_id CHAR(36) NOT NULL,
  sales_xaf BIGINT NOT NULL,
  bonus_xaf BIGINT NOT NULL,
  adjustment_id CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (campaign_id, seller_id)
);
