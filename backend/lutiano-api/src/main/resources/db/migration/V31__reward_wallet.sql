-- A reward a customer redeems in the app goes to a wallet: points are spent at once, the reward is used later at a sales point by showing
-- its QR code (token). use_state: AVAILABLE, USED or EXPIRED. Redemptions made by staff before this are already used (the default).
ALTER TABLE reward_redemption
  ADD COLUMN use_state VARCHAR(10) NOT NULL DEFAULT 'USED',
  ADD COLUMN valid_until TIMESTAMP NULL,
  ADD COLUMN token CHAR(36) NULL,
  ADD COLUMN used_at TIMESTAMP NULL,
  ADD COLUMN used_by CHAR(36) NULL,
  ADD COLUMN used_operation_id CHAR(36) NULL,
  ADD UNIQUE INDEX uq_redemption_token (token),
  ADD INDEX idx_redemption_wallet (customer_id, use_state, valid_until);
