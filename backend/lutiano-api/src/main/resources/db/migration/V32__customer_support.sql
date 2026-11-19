-- Support requests can come from a customer of the Customer app too (user_id is then a customer id).
ALTER TABLE support_request ADD COLUMN requester_type VARCHAR(10) NOT NULL DEFAULT 'STAFF';
CREATE INDEX idx_support_requester ON support_request (requester_type, status, created_at);
