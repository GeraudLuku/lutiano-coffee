-- In-app inbox: a staff member's own messages can be read and dismissed in the admin portal. read_at is set when they open one.
ALTER TABLE notification_delivery ADD COLUMN read_at TIMESTAMP NULL, ADD INDEX idx_delivery_inbox (recipient_id, read_at, created_at);
-- Messages from before the inbox existed count as read.
UPDATE notification_delivery SET read_at = UTC_TIMESTAMP() WHERE read_at IS NULL;
