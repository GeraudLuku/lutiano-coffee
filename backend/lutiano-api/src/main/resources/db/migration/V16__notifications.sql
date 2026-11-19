-- A14 Notifications. Which events send an SMS, their French/English texts and every delivery attempt; plus the staff
-- fields they rely on (language, approval alerts) and administrator 2FA enrolment.

-- One row per event the platform can notify about. The list of events lives in code; rows are created at startup.
-- sms_enabled switches the SMS channel for an event off or on (push is not available yet).
CREATE TABLE notification_event (
  event_key VARCHAR(40) PRIMARY KEY,
  sms_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  updated_by CHAR(36) NULL,
  updated_at TIMESTAMP NULL
);

-- The text of an event per language ('fr' or 'en'). Placeholders look like {name}; each event allows only its own.
CREATE TABLE notification_template (
  event_key VARCHAR(40) NOT NULL,
  language CHAR(2) NOT NULL,
  body VARCHAR(500) NOT NULL,
  updated_by CHAR(36) NULL,
  updated_at TIMESTAMP NULL,
  PRIMARY KEY (event_key, language)
);

-- Every message the platform tried to send. status: QUEUED, SENT (accepted by the provider), LOGGED (dev mode: written
-- to the log, not sent), FAILED, or SKIPPED (not attempted: channel off, no valid phone). Codes are never stored.
CREATE TABLE notification_delivery (
  id CHAR(36) PRIMARY KEY,
  event_key VARCHAR(40) NOT NULL,
  recipient_id CHAR(36) NULL,
  recipient_name VARCHAR(150) NULL,
  phone VARCHAR(20) NULL,
  language CHAR(2) NOT NULL,
  body VARCHAR(500) NOT NULL,
  status VARCHAR(10) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  error VARCHAR(500) NULL,
  related_type VARCHAR(30) NULL,
  related_id CHAR(36) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL,
  INDEX idx_delivery_created (created_at),
  INDEX idx_delivery_status (status, created_at),
  INDEX idx_delivery_event (event_key, created_at),
  INDEX idx_delivery_recipient (recipient_id, created_at)
);

-- language: the language of this person's messages. approval_alerts: receives "waiting for approval" messages.
-- mfa_enrolled_at: set when an administrator confirmed their phone with a code; cleared when their phone changes or 2FA is reset.
ALTER TABLE staff_user
  ADD COLUMN language CHAR(2) NOT NULL DEFAULT 'fr',
  ADD COLUMN approval_alerts BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN mfa_enrolled_at TIMESTAMP NULL;
UPDATE staff_user SET approval_alerts = TRUE WHERE role = 'ADMIN';

-- The demo administrator needs a phone to receive sign-in codes (dev mode logs them; see lutiano.otp.dev-code).
UPDATE staff_user SET phone = '+237 690 00 00 01' WHERE email = 'admin@lutiano.local' AND phone IS NULL;
