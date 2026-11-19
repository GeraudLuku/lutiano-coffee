-- A04 Seller Management: seller role/channel, profile address, deactivation record, session revocation,
-- eligibility documents and an assignment history.

-- seller_role applies to SELLER accounts only: FIXED_POS_BARISTA, MOBILE_BARISTA or MARKET_AGENT.
-- tokens_valid_after: sign-in tokens issued before this moment are rejected ("sign out of all devices").
ALTER TABLE staff_user
  ADD COLUMN seller_role VARCHAR(30) NULL,
  ADD COLUMN address VARCHAR(255) NULL,
  ADD COLUMN area VARCHAR(120) NULL,
  ADD COLUMN tokens_valid_after TIMESTAMP NULL,
  ADD COLUMN deactivated_at TIMESTAMP NULL,
  ADD COLUMN deactivated_by CHAR(36) NULL,
  ADD COLUMN deactivation_reason VARCHAR(500) NULL;
UPDATE staff_user SET seller_role = 'FIXED_POS_BARISTA' WHERE role = 'SELLER';

-- Who assigned or removed a seller's sales locations, and when. seller_operation keeps the current assignments.
CREATE TABLE seller_assignment_log (
  id CHAR(36) PRIMARY KEY,
  seller_id CHAR(36) NOT NULL,
  operation_id CHAR(36) NOT NULL,
  action VARCHAR(12) NOT NULL,
  actor_id CHAR(36) NULL,
  note VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_assignment_log_seller (seller_id, created_at)
);
INSERT INTO seller_assignment_log (id, seller_id, operation_id, action, note, created_at)
SELECT UUID(), x.seller_id, x.operation_id, 'ASSIGNED', 'Assigned before the history log existed', u.created_at
  FROM seller_operation x JOIN staff_user u ON u.id = x.seller_id;

-- Eligibility documents (ID, contract…), stored privately in object storage.
CREATE TABLE seller_document (
  id CHAR(36) PRIMARY KEY,
  seller_id CHAR(36) NOT NULL,
  doc_type VARCHAR(30) NOT NULL,
  object_key VARCHAR(255) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  size_bytes BIGINT NOT NULL,
  expires_on DATE NULL,
  note VARCHAR(500) NULL,
  status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
  uploaded_by CHAR(36) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  reviewed_by CHAR(36) NULL,
  reviewed_at TIMESTAMP NULL,
  review_note VARCHAR(500) NULL,
  INDEX idx_seller_document (seller_id, doc_type)
);
