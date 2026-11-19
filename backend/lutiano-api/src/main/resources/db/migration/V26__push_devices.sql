-- Phones that receive push notifications. One row per FCM token; a token belongs to whoever signed in last on that phone.
CREATE TABLE device_token (
  token VARCHAR(512) PRIMARY KEY,
  user_id CHAR(36) NOT NULL,
  platform VARCHAR(10) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_device_token_user (user_id)
);
