-- A stock request is one request for several products to one sales location. Its products are ordinary stock_allocation rows
-- (request_id points back here), so approvals, dispatch, receipt, the ledger and the dashboards keep working on the rows.
-- The request carries the shared status: it is approved, rejected, dispatched and cancelled as a whole.
CREATE TABLE stock_request (
  id CHAR(36) PRIMARY KEY,
  reference VARCHAR(30) NOT NULL UNIQUE,
  operation_id CHAR(36) NOT NULL,
  requested_by CHAR(36) NOT NULL,
  idempotency_key VARCHAR(64) NULL,
  needed_by DATE NULL,
  reason VARCHAR(500) NULL,
  status VARCHAR(20) NOT NULL,
  decision_note VARCHAR(500) NULL,
  approved_by CHAR(36) NULL,
  approved_at TIMESTAMP NULL,
  dispatched_at TIMESTAMP NULL,
  completed_at TIMESTAMP NULL,
  closed_by CHAR(36) NULL,
  closed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_stock_request_key (requested_by, idempotency_key),
  INDEX idx_stock_request_requester (requested_by, operation_id, created_at),
  INDEX idx_stock_request_status (status, created_at)
);
CREATE SEQUENCE stock_request_seq START WITH 1 INCREMENT BY 1 NOCACHE;

ALTER TABLE stock_allocation ADD COLUMN request_id CHAR(36) NULL, ADD INDEX idx_allocation_request (request_id);
