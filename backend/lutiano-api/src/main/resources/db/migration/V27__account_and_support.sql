-- A phone number a person asked to change to, waiting for the code sent to it.
CREATE TABLE phone_change (
  user_id CHAR(36) PRIMARY KEY,
  phone VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Requests sellers send to the LUTIANO team. type: QUESTION, ISSUE, IMPROVEMENT or ACCOUNT. status: OPEN, IN_PROGRESS or RESOLVED.
-- The reply is what the team wrote back; the seller sees it in the app.
CREATE TABLE support_request (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  user_id CHAR(36) NOT NULL,
  type VARCHAR(12) NOT NULL,
  subject VARCHAR(150) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'OPEN',
  attachment_key VARCHAR(255) NULL,
  attachment_name VARCHAR(255) NULL,
  attachment_type VARCHAR(100) NULL,
  reply VARCHAR(2000) NULL,
  replied_by CHAR(36) NULL,
  replied_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_support_user (user_id, created_at),
  INDEX idx_support_status (status, created_at)
);
CREATE SEQUENCE support_request_seq START WITH 1 INCREMENT BY 1 NOCACHE;
