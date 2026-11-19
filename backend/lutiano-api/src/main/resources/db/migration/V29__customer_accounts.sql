-- Customer accounts for the Customer app. A customer record may already exist (created by an administrator, with loyalty points);
-- registered_at stays NULL until the person signs up in the app, which then claims that record by phone number.

-- Phones become +2376XXXXXXXX like staff phones (a collision with another customer stops this migration).
UPDATE customer SET phone = CONCAT('+237', RIGHT(REGEXP_REPLACE(phone, '[^0-9]', ''), 9))
 WHERE REGEXP_REPLACE(phone, '[^0-9]', '') REGEXP '^(237)?6[0-9]{8}$';

ALTER TABLE customer
  ADD COLUMN email VARCHAR(190) NULL,
  ADD COLUMN password_hash VARCHAR(100) NULL,
  ADD COLUMN language VARCHAR(2) NOT NULL DEFAULT 'fr',
  ADD COLUMN birth_date DATE NULL,
  ADD COLUMN image_url VARCHAR(255) NULL,
  ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN terms_accepted_at TIMESTAMP NULL,
  ADD COLUMN marketing_consent BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN registered_at TIMESTAMP NULL,
  ADD COLUMN last_login_at TIMESTAMP NULL,
  ADD COLUMN tokens_valid_after TIMESTAMP NULL,
  ADD COLUMN deleted_at TIMESTAMP NULL,
  ADD UNIQUE INDEX uq_customer_email (email);

-- A Google or Apple account linked to a customer. The pair (provider, subject) identifies the person at that provider.
CREATE TABLE customer_identity (
  provider VARCHAR(10) NOT NULL,
  subject VARCHAR(190) NOT NULL,
  customer_id CHAR(36) NOT NULL,
  email VARCHAR(190) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (provider, subject),
  INDEX idx_customer_identity_customer (customer_id)
);

CREATE TABLE customer_favourite (
  customer_id CHAR(36) NOT NULL,
  product_id CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (customer_id, product_id)
);

-- Opening hours of a sales operation, one row per weekday (1 = Monday ... 7 = Sunday) in Douala time. A day without a row is closed;
-- an operation with no rows at all is treated as always open.
CREATE TABLE operation_hours (
  operation_id CHAR(36) NOT NULL,
  weekday TINYINT NOT NULL,
  opens TIME NOT NULL,
  closes TIME NOT NULL,
  PRIMARY KEY (operation_id, weekday)
);
