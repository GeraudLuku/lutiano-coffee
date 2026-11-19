-- The seller's in-app notifications. SMS stays a delivery channel (notification_delivery); this is what the app shows, and it
-- exists whether or not an SMS was sent. Events are titled by the app from event_key; announcements carry their own title.
-- category: INVENTORY, REQUESTS, SALES, EARNINGS, TARGETS, OPERATIONS or SYSTEM. cleared_at hides a notification from the list
-- (it is kept for 90 days, then removed).
CREATE TABLE app_notification (
  id CHAR(36) PRIMARY KEY,
  recipient_id CHAR(36) NOT NULL,
  event_key VARCHAR(40) NOT NULL,
  category VARCHAR(20) NOT NULL,
  title VARCHAR(150) NULL,
  body VARCHAR(2000) NOT NULL,
  important BOOLEAN NOT NULL DEFAULT FALSE,
  related_type VARCHAR(30) NULL,
  related_id CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  read_at TIMESTAMP NULL,
  cleared_at TIMESTAMP NULL,
  INDEX idx_app_notification_inbox (recipient_id, cleared_at, created_at),
  INDEX idx_app_notification_cleanup (cleared_at)
);

-- What each person wants to receive, per category. Missing rows mean on. push is stored for when push delivery exists.
CREATE TABLE notification_preference (
  user_id CHAR(36) NOT NULL,
  category VARCHAR(20) NOT NULL,
  in_app BOOLEAN NOT NULL DEFAULT TRUE,
  push BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (user_id, category)
);

-- A message an administrator sends to sellers. audience: ALL_SELLERS, ROLE (seller_role) or OPERATION (operation_id).
CREATE TABLE announcement (
  id CHAR(36) PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  body VARCHAR(2000) NOT NULL,
  audience VARCHAR(15) NOT NULL,
  seller_role VARCHAR(30) NULL,
  operation_id CHAR(36) NULL,
  important BOOLEAN NOT NULL DEFAULT FALSE,
  recipient_count INT NOT NULL DEFAULT 0,
  created_by CHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_announcement_created (created_at)
);

-- Target milestones already told to the seller (80% and 100%), so each is announced once.
CREATE TABLE target_milestone (
  target_id CHAR(36) NOT NULL,
  milestone INT NOT NULL,
  notified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (target_id, milestone)
);
